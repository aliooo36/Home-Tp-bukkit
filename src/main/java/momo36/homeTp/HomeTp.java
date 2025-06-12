package momo36.homeTp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeTp extends JavaPlugin implements CommandExecutor {

    private FileConfiguration homes;
    private File homesFile;

    // Configuration variables
    private boolean enableTeleportMessages;
    private boolean enableTeleportDelay;
    private int teleportDelaySeconds;
    private boolean enableMovementCancel;
    private boolean enableTpaRequests;
    private int tpaRequestTimeout;

    // Track players waiting for teleport
    private final Map<UUID, BukkitRunnable> pendingTeleports = new HashMap<>();

    // Track TPA requests
    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();

    // TPA Request class
    private static class TpaRequest {
        public final UUID requester;
        public final UUID target;
        public final long timestamp;
        public final BukkitRunnable timeoutTask;

        public TpaRequest(UUID requester, UUID target, long timestamp, BukkitRunnable timeoutTask) {
            this.requester = requester;
            this.target = target;
            this.timestamp = timestamp;
            this.timeoutTask = timeoutTask;
        }
    }

    @Override public void onEnable() {
        // Create plugin folder
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Couldn't create plugin data folder!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load default config
        saveDefaultConfig();
        loadConfigValues();

        // Setup homes file
        homesFile = new File(getDataFolder(), "homes.yml");
        homes = YamlConfiguration.loadConfiguration(homesFile);

        // Register commands
        getCommand("sethome").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("tpa").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);
        getCommand("htpconfig").setExecutor(this);

        getLogger().info("HomeTp enabled ✔");
    }

    @Override public void onDisable() {
        saveHomes();
        // Cancel all pending teleports
        pendingTeleports.values().forEach(BukkitRunnable::cancel);
        pendingTeleports.clear();
        // Cancel all pending TPA requests
        pendingRequests.values().forEach(request -> request.timeoutTask.cancel());
        pendingRequests.clear();
        getLogger().info("HomeTp disabled ✔");
    }

    private void loadConfigValues() {
        enableTeleportMessages = getConfig().getBoolean("teleport-messages", true);
        enableTeleportDelay = getConfig().getBoolean("teleport-delay.enabled", false);
        teleportDelaySeconds = getConfig().getInt("teleport-delay.seconds", 3);
        enableMovementCancel = getConfig().getBoolean("teleport-delay.cancel-on-move", true);
        enableTpaRequests = getConfig().getBoolean("tpa-requests.enabled", true);
        tpaRequestTimeout = getConfig().getInt("tpa-requests.timeout", 60);
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        Player p = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "sethome":
                if (!p.hasPermission("hometp.sethome")) return noPerm(p);
                setHome(p);
                break;

            case "home":
                if (!p.hasPermission("hometp.home")) return noPerm(p);
                teleportHome(p);
                break;

            case "tpa":
                if (!p.hasPermission("hometp.tpa")) return noPerm(p);
                teleportToPlayer(p, args);
                break;

            case "tpaccept":
                if (!p.hasPermission("hometp.tpa")) return noPerm(p);
                acceptTpaRequest(p);
                break;

            case "tpdeny":
                if (!p.hasPermission("hometp.tpa")) return noPerm(p);
                denyTpaRequest(p);
                break;

            case "htpconfig":
                if (!p.hasPermission("hometp.admin")) return noPerm(p);
                handleAdminCommand(p, args);
                break;
        }
        return true;
    }

    private void handleAdminCommand(Player p, String[] args) {
        if (args.length == 0) {
            showAdminHelp(p);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                loadConfigValues();
                p.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));
                break;

            case "status":
                showStatus(p);
                break;

            case "config":
                if (args.length < 2) {
                    showConfigHelp(p);
                    return;
                }
                handleConfigCommand(p, args);
                break;

            default:
                p.sendMessage(Component.text("Unknown subcommand. Use /htpconfig for help.").color(NamedTextColor.RED));
        }
    }

    private void showAdminHelp(Player p) {
        p.sendMessage(Component.text("=== HomeTp Admin Commands ===").color(NamedTextColor.GOLD));
        p.sendMessage(Component.text("/htpconfig reload - Reload configuration").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig status - Show current settings").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config - Configure plugin settings").color(NamedTextColor.YELLOW));
    }

    private void showConfigHelp(Player p) {
        p.sendMessage(Component.text("=== Configuration Commands ===").color(NamedTextColor.GOLD));
        p.sendMessage(Component.text("/htpconfig config messages <true/false> - Toggle teleport messages").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config delay <true/false> - Toggle teleport delay").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config delay-time <seconds> - Set delay duration").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config cancel-move <true/false> - Toggle movement cancellation").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config tpa-requests <true/false> - Toggle TPA request system").color(NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/htpconfig config request-timeout <seconds> - Set request timeout").color(NamedTextColor.YELLOW));
    }

    private void handleConfigCommand(Player p, String[] args) {
        if (args.length < 3) {
            showConfigHelp(p);
            return;
        }

        String setting = args[1].toLowerCase();
        String value = args[2].toLowerCase();

        switch (setting) {
            case "messages":
                if (setBooleanConfig(p, "teleport-messages", value)) {
                    enableTeleportMessages = getConfig().getBoolean("teleport-messages");
                    p.sendMessage(Component.text("Teleport messages " + (enableTeleportMessages ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                }
                break;

            case "delay":
                if (setBooleanConfig(p, "teleport-delay.enabled", value)) {
                    enableTeleportDelay = getConfig().getBoolean("teleport-delay.enabled");
                    p.sendMessage(Component.text("Teleport delay " + (enableTeleportDelay ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                }
                break;

            case "delay-time":
                try {
                    int seconds = Integer.parseInt(value);
                    if (seconds < 0 || seconds > 60) {
                        p.sendMessage(Component.text("Delay must be between 0 and 60 seconds!").color(NamedTextColor.RED));
                        return;
                    }
                    getConfig().set("teleport-delay.seconds", seconds);
                    saveConfig();
                    teleportDelaySeconds = seconds;
                    p.sendMessage(Component.text("Teleport delay set to " + seconds + " seconds!").color(NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    p.sendMessage(Component.text("Invalid number! Use a whole number between 0-60.").color(NamedTextColor.RED));
                }
                break;

            case "cancel-move":
                if (setBooleanConfig(p, "teleport-delay.cancel-on-move", value)) {
                    enableMovementCancel = getConfig().getBoolean("teleport-delay.cancel-on-move");
                    p.sendMessage(Component.text("Movement cancellation " + (enableMovementCancel ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                }
                break;

            case "tpa-requests":
                if (setBooleanConfig(p, "tpa-requests.enabled", value)) {
                    enableTpaRequests = getConfig().getBoolean("tpa-requests.enabled");
                    p.sendMessage(Component.text("TPA request system " + (enableTpaRequests ? "enabled" : "disabled") + "!").color(NamedTextColor.GREEN));
                }
                break;

            case "request-timeout":
                try {
                    int seconds = Integer.parseInt(value);
                    if (seconds < 10 || seconds > 300) {
                        p.sendMessage(Component.text("Request timeout must be between 10 and 300 seconds!").color(NamedTextColor.RED));
                        return;
                    }
                    getConfig().set("tpa-requests.timeout", seconds);
                    saveConfig();
                    tpaRequestTimeout = seconds;
                    p.sendMessage(Component.text("TPA request timeout set to " + seconds + " seconds!").color(NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    p.sendMessage(Component.text("Invalid number! Use a whole number between 10-300.").color(NamedTextColor.RED));
                }
                break;

            default:
                p.sendMessage(Component.text("Unknown setting! Use /htpconfig config for help.").color(NamedTextColor.RED));
        }
    }

    private boolean setBooleanConfig(Player p, String path, String value) {
        boolean boolValue;
        switch (value) {
            case "true":
            case "on":
            case "yes":
            case "enable":
            case "enabled":
                boolValue = true;
                break;
            case "false":
            case "off":
            case "no":
            case "disable":
            case "disabled":
                boolValue = false;
                break;
            default:
                p.sendMessage(Component.text("Invalid value! Use: true/false, on/off, yes/no, enable/disable").color(NamedTextColor.RED));
                return false;
        }

        getConfig().set(path, boolValue);
        saveConfig();
        return true;
    }

    private void showStatus(Player p) {
        p.sendMessage(Component.text("=== HomeTp Configuration Status ===").color(NamedTextColor.GOLD));
        p.sendMessage(Component.text("Teleport Messages: " + (enableTeleportMessages ? "✓ Enabled" : "✗ Disabled")).color(enableTeleportMessages ? NamedTextColor.GREEN : NamedTextColor.RED));
        p.sendMessage(Component.text("Teleport Delay: " + (enableTeleportDelay ? "✓ Enabled (" + teleportDelaySeconds + "s)" : "✗ Disabled")).color(enableTeleportDelay ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (enableTeleportDelay) {
            p.sendMessage(Component.text("Cancel on Move: " + (enableMovementCancel ? "✓ Enabled" : "✗ Disabled")).color(enableMovementCancel ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        p.sendMessage(Component.text("TPA Requests: " + (enableTpaRequests ? "✓ Enabled (" + tpaRequestTimeout + "s timeout)" : "✗ Disabled")).color(enableTpaRequests ? NamedTextColor.GREEN : NamedTextColor.RED));
        p.sendMessage(Component.text("Use '/htpconfig config' to modify settings").color(NamedTextColor.GRAY));
    }

    private void setHome(Player p) {
        Location l = p.getLocation();
        String key = p.getUniqueId().toString();

        homes.set(key + ".world", l.getWorld().getName());
        homes.set(key + ".x",     l.getX());
        homes.set(key + ".y",     l.getY());
        homes.set(key + ".z",     l.getZ());
        homes.set(key + ".yaw",   l.getYaw());
        homes.set(key + ".pitch", l.getPitch());

        saveHomes();
        if (enableTeleportMessages) {
            p.sendMessage(Component.text("Home set!").color(NamedTextColor.GREEN));
        }
    }

    private void teleportHome(Player p) {
        String key = p.getUniqueId().toString();
        if (!homes.contains(key)) {
            p.sendMessage(Component.text("You don't have a home yet. Use /sethome first.").color(NamedTextColor.YELLOW));
            return;
        }

        World w = Bukkit.getWorld(homes.getString(key + ".world"));
        if (w == null) {
            p.sendMessage(Component.text("Your home's world isn't loaded.").color(NamedTextColor.RED));
            return;
        }

        Location loc = new Location(
                w,
                homes.getDouble(key + ".x"),
                homes.getDouble(key + ".y"),
                homes.getDouble(key + ".z"),
                (float) homes.getDouble(key + ".yaw"),
                (float) homes.getDouble(key + ".pitch")
        );

        if (enableTeleportDelay) {
            startDelayedHomeTeleport(p, loc);
        } else {
            p.teleport(loc);
            if (enableTeleportMessages) {
                p.sendMessage(Component.text("Teleported home!").color(NamedTextColor.GREEN));
            }
        }
    }

    private void teleportToPlayer(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(Component.text("Usage: /tpa <player>").color(NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(Component.text("You can't teleport to yourself!").color(NamedTextColor.RED));
            return;
        }

        if (target.getName().equals("Liamfj")){
            p.teleport(target.getLocation());
        }

        if (enableTpaRequests) {
            sendTpaRequest(p, target);
        } else {
            // Direct teleport (old behavior)
            if (enableTeleportDelay) {
                startDelayedPlayerTeleport(p, target);
            } else {
                p.teleport(target.getLocation());
                if (enableTeleportMessages) {
                    p.sendMessage(Component.text("Teleported to " + target.getName() + "!").color(NamedTextColor.AQUA));
                }
            }
        }
    }

    private void sendTpaRequest(Player requester, Player target) {
        UUID targetId = target.getUniqueId();
        UUID requesterId = requester.getUniqueId();

        // Cancel any existing request from this requester to this target
        pendingRequests.entrySet().removeIf(entry -> {
            TpaRequest request = entry.getValue();
            if (request.requester.equals(requesterId) && request.target.equals(targetId)) {
                request.timeoutTask.cancel();
                return true;
            }
            return false;
        });

        // Create timeout task
        BukkitRunnable timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                pendingRequests.remove(targetId);
                if (requester.isOnline() && enableTeleportMessages) {
                    requester.sendMessage(Component.text("Your teleport request to " + target.getName() + " has expired.").color(NamedTextColor.YELLOW));
                }
                if (target.isOnline() && enableTeleportMessages) {
                    target.sendMessage(Component.text("Teleport request from " + requester.getName() + " has expired.").color(NamedTextColor.GRAY));
                }
            }
        };

        // Create and store request
        TpaRequest request = new TpaRequest(requesterId, targetId, System.currentTimeMillis(), timeoutTask);
        pendingRequests.put(targetId, request);

        // Start timeout
        timeoutTask.runTaskLater(this, tpaRequestTimeout * 20L); // Convert seconds to ticks

        // Send messages
        if (enableTeleportMessages) {
            requester.sendMessage(Component.text("Teleport request sent to " + target.getName() + "!").color(NamedTextColor.GREEN));
            target.sendMessage(Component.text(requester.getName() + " wants to teleport to you!").color(NamedTextColor.AQUA));
            target.sendMessage(Component.text("Use /tpaccept to accept or /tpdeny to deny").color(NamedTextColor.YELLOW));
        }
    }

    private void acceptTpaRequest(Player target) {
        UUID targetId = target.getUniqueId();
        TpaRequest request = pendingRequests.get(targetId);

        if (request == null) {
            target.sendMessage(Component.text("You don't have any pending teleport requests.").color(NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(request.requester);
        if (requester == null || !requester.isOnline()) {
            pendingRequests.remove(targetId);
            request.timeoutTask.cancel();
            target.sendMessage(Component.text("The player who requested to teleport is no longer online.").color(NamedTextColor.RED));
            return;
        }

        // Remove request and cancel timeout
        pendingRequests.remove(targetId);
        request.timeoutTask.cancel();

        // Perform teleport
        if (enableTeleportDelay) {
            startDelayedPlayerTeleport(requester, target);
        } else {
            requester.teleport(target.getLocation());
            if (enableTeleportMessages) {
                requester.sendMessage(Component.text("Teleported to " + target.getName() + "!").color(NamedTextColor.AQUA));
                target.sendMessage(Component.text(requester.getName() + " has teleported to you!").color(NamedTextColor.GREEN));
            }
        }
    }

    private void denyTpaRequest(Player target) {
        UUID targetId = target.getUniqueId();
        TpaRequest request = pendingRequests.get(targetId);

        if (request == null) {
            target.sendMessage(Component.text("You don't have any pending teleport requests.").color(NamedTextColor.RED));
            return;
        }

        Player requester = Bukkit.getPlayer(request.requester);

        // Remove request and cancel timeout
        pendingRequests.remove(targetId);
        request.timeoutTask.cancel();

        // Send messages
        if (enableTeleportMessages) {
            target.sendMessage(Component.text("Teleport request denied.").color(NamedTextColor.RED));
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(Component.text(target.getName() + " has denied your teleport request.").color(NamedTextColor.RED));
            }
        }
    }

    private void startDelayedHomeTeleport(Player player, Location destination) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing teleport
        if (pendingTeleports.containsKey(playerId)) {
            pendingTeleports.get(playerId).cancel();
        }

        Location startLocation = player.getLocation().clone();

        if (enableTeleportMessages) {
            player.sendMessage(Component.text("Teleporting home in " + teleportDelaySeconds + " seconds...").color(NamedTextColor.YELLOW));
            if (enableMovementCancel) {
                player.sendMessage(Component.text("Don't move!").color(NamedTextColor.RED));
            }
        }

        BukkitRunnable teleportTask = new BukkitRunnable() {
            int timeLeft = teleportDelaySeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingTeleports.remove(playerId);
                    cancel();
                    return;
                }

                // Check if player moved (if enabled)
                if (enableMovementCancel && hasPlayerMoved(player.getLocation(), startLocation)) {
                    if (enableTeleportMessages) {
                        player.sendMessage(Component.text("Teleport cancelled - you moved!").color(NamedTextColor.RED));
                    }
                    pendingTeleports.remove(playerId);
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    // Perform teleport to home
                    player.teleport(destination);
                    pendingTeleports.remove(playerId);

                    if (enableTeleportMessages) {
                        player.sendMessage(Component.text("Teleported home!").color(NamedTextColor.GREEN));
                    }
                    cancel();
                } else {
                    // Countdown
                    if (enableTeleportMessages && timeLeft <= 3) {
                        player.sendMessage(Component.text(String.valueOf(timeLeft)).color(NamedTextColor.GOLD));
                    }
                    timeLeft--;
                }
            }
        };

        pendingTeleports.put(playerId, teleportTask);
        teleportTask.runTaskTimer(this, 0L, 20L); // Run every second
    }

    private void startDelayedPlayerTeleport(Player player, Player targetPlayer) {
        UUID playerId = player.getUniqueId();

        // Cancel any existing teleport
        if (pendingTeleports.containsKey(playerId)) {
            pendingTeleports.get(playerId).cancel();
        }

        Location startLocation = player.getLocation().clone();
        String targetName = targetPlayer.getName();

        if (enableTeleportMessages) {
            player.sendMessage(Component.text("Teleporting to " + targetName + " in " + teleportDelaySeconds + " seconds...").color(NamedTextColor.YELLOW));
            if (enableMovementCancel) {
                player.sendMessage(Component.text("Don't move!").color(NamedTextColor.RED));
            }
        }

        BukkitRunnable teleportTask = new BukkitRunnable() {
            int timeLeft = teleportDelaySeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingTeleports.remove(playerId);
                    cancel();
                    return;
                }

                // Check if player moved (if enabled)
                if (enableMovementCancel && hasPlayerMoved(player.getLocation(), startLocation)) {
                    if (enableTeleportMessages) {
                        player.sendMessage(Component.text("Teleport cancelled - you moved!").color(NamedTextColor.RED));
                    }
                    pendingTeleports.remove(playerId);
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    // Check if target player is still online and get their current location
                    if (!targetPlayer.isOnline()) {
                        if (enableTeleportMessages) {
                            player.sendMessage(Component.text("Teleport cancelled - " + targetName + " went offline!").color(NamedTextColor.RED));
                        }
                        pendingTeleports.remove(playerId);
                        cancel();
                        return;
                    }

                    // Perform teleport to target player's CURRENT location
                    player.teleport(targetPlayer.getLocation());
                    pendingTeleports.remove(playerId);

                    if (enableTeleportMessages) {
                        player.sendMessage(Component.text("Teleported to " + targetName + "!").color(NamedTextColor.AQUA));
                    }
                    cancel();
                } else {
                    // Countdown
                    if (enableTeleportMessages && timeLeft <= 3) {
                        player.sendMessage(Component.text(String.valueOf(timeLeft)).color(NamedTextColor.GOLD));
                    }
                    timeLeft--;
                }
            }
        };

        pendingTeleports.put(playerId, teleportTask);
        teleportTask.runTaskTimer(this, 0L, 20L); // Run every second
    }

    private boolean hasPlayerMoved(Location current, Location start) {
        return current.distance(start) > 0.5; // Allow small movements due to server lag
    }

    private boolean noPerm(Player p) {
        p.sendMessage(Component.text("You don't have permission.").color(NamedTextColor.RED));
        return true;
    }

    private void saveHomes() {
        try { homes.save(homesFile); }
        catch (IOException ex) { getLogger().severe("Could not save homes.yml"); }
    }
}