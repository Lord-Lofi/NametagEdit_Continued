# NametagEdit (1.21.x Fork)

[![Spigot](https://img.shields.io/badge/Spigot-1.21.x-yellow.svg)](https://www.spigotmc.org/resources/nametagedit.3836/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7--1.21.11-green.svg)](documentation/Support)
[![JDK](https://img.shields.io/badge/JDK-17%2B-blue.svg)](https://adoptium.net/)
[![NametagEditAPI](https://img.shields.io/badge/NTE-Developer%20API-ff69b4.svg)](documentation/Developers.creole)

> **Fork of [sgtcaze/NametagEdit](https://github.com/sgtcaze/NametagEdit)** updated to support Minecraft 1.21.x (including 1.21.11) on Spigot.

This plugin allows users to add prefixes and suffixes to player nametags. Individual tags can be created for players, or a group tag can be assigned via permissions.

* Minecraft `1.7.x – 1.12.x` — max **16-character** prefix/suffix
* Minecraft `1.13.x – 1.21.x` — max **256-character** prefix/suffix
* Minecraft `1.16.x – 1.21.x` — **hex color** (`&#RRGGBB`) support

Supports EssentialsGroupManager, PermissionsEx, zPermissions, **LuckPerms**, and LibsDisguises. Tags update automatically when a player's group or permissions change.

---

## Changes in this fork (v4.6.0)

| Area | Change |
|---|---|
| **Minecraft support** | Extended to 1.20.5/1.20.6 and all 1.21.x versions up to **1.21.11** |
| **VersionChecker** | No longer crashes with NPE on unknown future versions — falls back gracefully |
| **NMS reflection** | `playerConnection` field resolved by type-scan if Spigot renames it in a future build |
| **bStats** | Upgraded from embedded v1 class to **bStats v3** (`org.bstats:bstats-bukkit:3.0.2`) |
| **Dependencies** | Removed `commons-lang` and `json-simple` — replaced with Java built-ins and Gson |
| **Build** | Java 17+, Spigot API `1.21.11-R0.2-SNAPSHOT`, shade plugin 3.5.1 |

---

## Quick Links
* [API & Developers](documentation/Developers.creole)
* [Permissions](documentation/Permissions.creole)
* [Commands](documentation/Commands.creole)
* [Configuration](documentation/Configuration.creole)
* [Common Issues](documentation/Support.creole)

---

## Features
✔ Flatfile and MySQL storage with connection pooling (HikariCP)

✔ PermissionsEX, zPermissions, GroupManager, **LuckPerms** support

✔ Sortable group/player tags in the tab list

✔ [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) support

✔ [Guilds](https://www.spigotmc.org/resources/guilds.66176/) support

✔ Hex color support on 1.16+

---

## Building

Requires Java 17+ and Maven.

```bash
mvn clean package
```

The plugin jar is output to `target/NametagEdit.jar`. Drop it into your server's `plugins/` folder.

> **Note:** All NMS access is via reflection — no BuildTools run is required to compile.

---

## Frequently Asked Questions

#### Q: Will this allow me to change my skin or display name?
**A:** No. The plugin creates fake scoreboard teams with packets to show prefix/suffix around the vanilla name.

#### Q: My client crashes with "Cannot remove from ID#". Why?
**A:** A player can only belong to one scoreboard team at a time. Any two plugins that manipulate team packets will conflict. There is no workaround.

#### Q: My nametag is cut short even with LongTags enabled!
**A:** LongTags only extends the tab-list display. The name above a player's head has a 16-character limit imposed by Mojang for prefix and suffix respectively.

#### Q: Can I sort nametags in the tab list?
**A:** Yes — see the [Configuration](documentation/Configuration.creole) docs.

---

## Incompatible Plugins
✖ Any plugin that creates NPCs sharing a username with players who have NametagEdit tags

✖ Any plugin that uses team-based color sidebars without explicit NametagEdit support

---

*Original plugin by [sgtcaze](https://github.com/sgtcaze). Fork maintained by [Lord-Lofi](https://github.com/Lord-Lofi).*
