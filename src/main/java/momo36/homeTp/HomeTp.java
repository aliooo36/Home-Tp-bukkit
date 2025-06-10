package momo36.homeTp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class HomeTp extends JavaPlugin implements CommandExecutor {

    private FileConfiguration homes;
    private File homesFile;

    @Override public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Couldn't create plugin data folder!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        homesFile = new File(getDataFolder(), "homes.yml");
        homes     = YamlConfiguration.loadConfiguration(homesFile);
        getCommand("sethome").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("tpa").setExecutor(this);
        getLogger().info("HomeTp enabled ✔");
    }

    @Override public void onDisable() {
        saveHomes();
        getLogger().info("HomeTp disabled ✔");
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
        }
        return true;
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
        p.sendMessage(Component.text("Home set!").color(NamedTextColor.GREEN));
    }

    private void teleportHome(Player p) {
        String key = p.getUniqueId().toString();
        if (!homes.contains(key)) {
            p.sendMessage(Component.text("You don’t have a home yet. Use /sethome first.").color(NamedTextColor.YELLOW));
            return;
        }

        World w = Bukkit.getWorld(homes.getString(key + ".world"));
        if (w == null) {
            p.sendMessage(Component.text("Your home’s world isn’t loaded.").color(NamedTextColor.RED));
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

        p.teleport(loc);
    }

    private void teleportToPlayer(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(Component.text("Usage: /tp <player>").color(NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
            return;
        }

        p.teleport(target.getLocation());
        p.sendMessage(Component.text("Teleported to " + target.getName() + "!").color(NamedTextColor.AQUA));
    }

    private boolean noPerm(Player p) {
        p.sendMessage(Component.text("You don’t have permission.").color(NamedTextColor.RED));
        return true;
    }

    private void saveHomes() {
        try { homes.save(homesFile); }
        catch (IOException ex) { getLogger().severe("Could not save homes.yml"); }
    }
}
