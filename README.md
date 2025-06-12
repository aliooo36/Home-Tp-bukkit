# Home-Tp-bukkit
**HomeTp** is a feature-rich Bukkit plugin that provides comprehensive teleportation functionality for your server. With configurable options, TPA request systems, and admin controls, it's perfect for servers that want robust teleportation features with full customization.

## ✨ Features

### Core Teleportation
- **`/sethome`** – Set your personal home location
- **`/home`** – Teleport to your saved home location
- **`/tpa <player>`** – Request to teleport to another player

### TPA Request System
- **`/tpaccept`** – Accept incoming teleport requests
- **`/tpdeny`** – Deny incoming teleport requests
- Configurable request timeout system
- Automatic request expiration with notifications

### Admin Configuration
- **`/htpconfig`** – Comprehensive admin command system
  - `reload` – Reload plugin configuration
  - `status` – View current plugin settings
  - `config` – Modify plugin settings in-game

### Advanced Features
- **Configurable teleport delays** with countdown timer
- **Movement cancellation** during teleport countdown
- **Toggleable teleport messages** for silent operation
- **Permission-based access control**
- **Per-setting configuration** through commands or config file

## 🎛️ Configuration

The plugin comes with extensive configuration options:

### Teleport Messages
- Enable/disable all teleport-related messages
- Perfect for servers wanting silent teleportation

### Teleport Delay System
- Optional countdown before teleportation
- Configurable delay duration (0-60 seconds)
- Movement cancellation during countdown
- Visual countdown for last 3 seconds

### TPA Request System
- Enable/disable the TPA request system
- Configurable request timeout (10-300 seconds)
- Automatic cleanup of expired requests

### In-Game Configuration
Use `/htpconfig config` to modify settings without editing files:
- `/htpconfig config messages <true/false>` – Toggle teleport messages
- `/htpconfig config delay <true/false>` – Toggle teleport delay
- `/htpconfig config delay-time <seconds>` – Set delay duration
- `/htpconfig config cancel-move <true/false>` – Toggle movement cancellation
- `/htpconfig config tpa-requests <true/false>` – Toggle TPA request system
- `/htpconfig config request-timeout <seconds>` – Set request timeout

## 📋 Commands & Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/sethome` | `hometp.sethome` | Set your home location |
| `/home` | `hometp.home` | Teleport to your home |
| `/tpa <player>` | `hometp.tpa` | Request teleport to a player |
| `/tpaccept` | `hometp.tpa` | Accept a teleport request |
| `/tpdeny` | `hometp.tpa` | Deny a teleport request |
| `/htpconfig` | `hometp.admin` | Admin configuration commands |

### Permission Details
- **`hometp.sethome`** – Default: `true` (all players)
- **`hometp.home`** – Default: `true` (all players)  
- **`hometp.tpa`** – Default: `true` (all players)
- **`hometp.admin`** – Default: `op` (operators only)

## 📦 Installation

1. Download the latest `.jar` file from releases
2. Place it in your server's `plugins/` directory
3. Restart your server
4. Configure settings using `/htpconfig` or edit `config.yml`

## 🔗 Download

**[Download Latest Release (.jar)](https://github.com/aliooo36/Home-Tp-bukkit/releases/latest)**

## 🖥️ Compatibility

- **Tested with**: PaperMC 1.20+
- **API Version**: 1.21
- **Compatible with**: All modern Bukkit/Spigot/Paper servers

## 🛠️ Development

The plugin uses Adventure API for modern text components and includes:
- Comprehensive error handling
- Automatic cleanup of expired requests
- Thread-safe teleport management
- Efficient file-based home storage

## 🤝 Contributing

Feel free to contribute to the project! Visit the [GitHub repository](https://github.com/aliooo36/Home-Tp-bukkit) to:
- Report bugs
- Suggest features
- Submit pull requests

---

**Version**: 1.1+  
**Author**: aliooo36  
**License**: MIT

![Plugin Screenshot](https://github.com/user-attachments/assets/4c41c703-e722-4ed7-80b3-51033f3e443f)
