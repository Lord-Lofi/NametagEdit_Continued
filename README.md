# NametagEdit (Paper 26.3 Fork)

[![Paper](https://img.shields.io/badge/Paper-26.3-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![PacketEvents](https://img.shields.io/badge/requires-PacketEvents%202.14%2B-green.svg)](https://www.spigotmc.org/resources/packetevents-api.80279/)
[![NametagEditAPI](https://img.shields.io/badge/NTE-Developer%20API-ff69b4.svg)](documentation/Developers.creole)

> **Fork of [sgtcaze/NametagEdit](https://github.com/sgtcaze/NametagEdit)** updated for Paper 26.3.

This plugin adds prefixes and suffixes to player nametags and the tab list. Tags can be set per player, or assigned to groups by permission.

---

## Requirements

* **Paper 26.3** (or a fork of it)
* **Java 25**
* **[PacketEvents](https://www.spigotmc.org/resources/packetevents-api.80279/) 2.14.0 or newer**, installed as a separate plugin
* Optional: [LuckPerms](https://luckperms.net/), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

---

## Changes in this fork (v5.0.0)

| Area | Change |
|---|---|
| **Minecraft support** | Targets **Paper 26.3** and Java 25. Support for older Minecraft versions has been removed. |
| **Nametag packets** | Sent through **PacketEvents** instead of reflection into server internals, so Minecraft updates are far less likely to break the plugin (usually updating PacketEvents is enough). Tags stay client-side: nothing is added to the server scoreboard. |
| **Hex colours** | `&#RRGGBB` works in prefixes and suffixes. |
| **PlaceholderAPI** | Placeholders still work inside tags, and NametagEdit now provides its own: `%nametagedit_prefix%`, `%nametagedit_suffix%`, `%nametagedit_group%`. |
| **LuckPerms** | Tags refresh when LuckPerms data changes. New option `LuckPerms.UsePrefixSuffix` uses LuckPerms prefixes/suffixes as nametags (off by default). |
| **UUID lookups** | Use the server's player cache and profile service instead of calling Mojang directly (and work on offline-mode servers). |
| **Removed** | PermissionsEx, zPermissions, GroupManager, Guilds and LibsDisguises support; the NametagEdit 3.x data converter; bStats metrics. |
| **Build** | Paper API 26.3, HikariCP 7 (relocated), Lombok 1.18.42, current Maven plugins. |

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

✔ **LuckPerms** support, including optional LuckPerms prefixes/suffixes

✔ [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholders in tags, plus NametagEdit's own placeholders

✔ Sortable group/player tags in the tab list

✔ Hex colour support (`&#RRGGBB`)

---

## Placeholders

With PlaceholderAPI installed, other plugins can use:

| Placeholder | Value |
|---|---|
| `%nametagedit_prefix%` | The prefix currently shown above the player's head |
| `%nametagedit_suffix%` | The suffix currently shown above the player's head |
| `%nametagedit_group%` | The NametagEdit group the player matches (empty if none) |

---

## Building

Requires Java 25 and Maven.

```bash
mvn clean package
```

The plugin jar is output to `target/NametagEdit.jar`. Put it in your server's `plugins/` folder together with PacketEvents.

---

## Frequently Asked Questions

#### Q: Will this allow me to change my skin or display name?
**A:** No. The plugin sends client-side scoreboard teams (via PacketEvents) to show a prefix/suffix around the vanilla name.

#### Q: My client crashes with "Cannot remove from ID#". Why?
**A:** A player can only belong to one scoreboard team at a time. Any two plugins that manipulate team packets will conflict. There is no workaround.

#### Q: My nametag is cut short even with LongTags enabled!
**A:** LongTags only changes the tab-list display. Prefixes and suffixes are limited to 256 characters each.

#### Q: Can I sort nametags in the tab list?
**A:** Yes — see the [Configuration](documentation/Configuration.creole) docs.

---

## Incompatible Plugins
✖ Any plugin that creates NPCs sharing a username with players who have NametagEdit tags

✖ Any plugin that uses team-based color sidebars without explicit NametagEdit support

---

*Original plugin by [sgtcaze](https://github.com/sgtcaze). Fork maintained by [Lord-Lofi](https://github.com/Lord-Lofi).*
