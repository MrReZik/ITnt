# 🧨 iTNT

**Custom TNT for your Minecraft server with flags, holograms, and ultimate explosion control!**

[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![SpigotMC](https://img.shields.io/badge/SpigotMC-1.17+-green.svg)](https://www.spigotmc.org/resources/itnt)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

---

## ✨ Core Features

The iTNT plugin allows server owners to define **fully customized TNT types** with granular control over their properties, extending well beyond vanilla mechanics.

* **Flexible Configuration:** Create an unlimited number of custom TNT types directly within `config.yml`.
* **Granular Explosion Control:**
    * Flags to disable block damage (`block-damage: false`).
    * Flags to disable entity damage (`entity-damage: false`).
* **Special Destruction Mechanics:**
    * Ability to break **obsidian** (`break-obsidian: true`).
    * Custom logic to break blocks **underwater** (`explode-in-water: true`).
* **Timers & Ignition:** Define the exact fuse time (`fuse-time`) and set TNT to ignite immediately upon placement (`auto-ignite`).
* **Dynamic Holograms:** Displays a real-time countdown timer above the TNT entity.
* **World Protection:** Disable specific TNT types in defined worlds (`disabled-worlds`).
* **Permission System:** Fine-tune control over which players can craft, receive, or place specific custom TNT types.

---

## 🛠️ Installation & Dependencies

### Prerequisites

iTNT requires **Java 17** or higher to run.

### Hologram Dependencies (Optional)

The plugin dynamically supports multiple hologram providers. You must choose one in `config.yml`.

* **ArmorStand:**
    * **Required Plugin:** None.
    * *Note: Works out-of-the-box, using vanilla entities.*
* **HolographicDisplays:**
    * **Required Plugin:** [HolographicDisplays Plugin]([https://dev.bukkit.org/projects/holographic-displays)).
    * *Note: Must be selected as the provider in your configuration.*
* **DecentHolograms:**
    * **Required Plugin:** [DecentHolograms Plugin](https://www.spigotmc.org/resources/decentholograms.96749/).
    * *Note: Must be selected as the provider in your configuration.*

### Installation Steps

1.  Download the latest stable version of the plugin.
2.  Place the `iTNT.jar` file into your server's `/plugins/` folder.
3.  Restart or reload your server.
4.  Modify the `config.yml` (generated in the `/plugins/iTNT` folder) to set up your custom TNT items.
5.  Run the command **`/itnt reload`** to apply your configuration changes without restarting.

---

## ⚙️ Configuration (`config.yml` Example)

The configuration allows detailed setup for holograms and individual TNT types.

```yaml
# Hologram Settings
hologram:
  enabled: true
  provider: "ArmorStand" # DecentHolograms, HolographicDisplays, ArmorStand
  format: "&#FF6347%name% &f- &e%time%s"
  offset-y: 0.8 # Vertical offset

# Custom TNT Types
tnt_types:
  tnt1:
    display-name: "&cBasic TNT"
    lore: 
      - "&7Standard explosion power."
      - ""
      - "&#FFD700Special Properties:"
      - "&f- &aPower: &c4.0"
      - "&f- &aExplode in Water: &aYes"
    fuse-time: 4
    auto-ignite: false
    power: 4.0
    block-damage: true
    entity-damage: true
    explode-in-water: true # Breaks blocks underwater
    break-obsidian: false
    disabled-worlds: ["world_nether"] # Disabled in this world
