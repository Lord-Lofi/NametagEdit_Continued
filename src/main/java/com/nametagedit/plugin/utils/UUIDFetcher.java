package com.nametagedit.plugin.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Looks up a player's UUID from their name using the server's own player data.
 *
 * In order:
 * 1. A player who is online right now.
 * 2. The server's cache of players who have joined before (no network call).
 * 3. The server's profile lookup, which uses the server's own (cached,
 *    rate-limited) Mojang session service and respects offline-mode servers.
 *
 * The callback always runs on the main server thread; the UUID is null if
 * the player can't be found.
 */
public final class UUIDFetcher {

    private UUIDFetcher() {
    }

    public static void lookupUUID(final String name, final Plugin plugin, final UUIDLookup uuidLookup) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            respond(plugin, uuidLookup, online.getUniqueId());
            return;
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            respond(plugin, uuidLookup, cached.getUniqueId());
            return;
        }

        // Not seen on this server before: ask the server's profile service, off the main thread.
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = null;
                try {
                    PlayerProfile profile = Bukkit.createProfile(name);
                    if (profile.complete(false)) {
                        uuid = profile.getId();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not look up the UUID for " + name + ": " + e.getMessage());
                }

                respond(plugin, uuidLookup, uuid);
            }
        }.runTaskAsynchronously(plugin);
    }

    /** Runs the callback on the main thread, straight away if we're already on it. */
    private static void respond(Plugin plugin, UUIDLookup uuidLookup, UUID uuid) {
        if (Bukkit.isPrimaryThread()) {
            uuidLookup.response(uuid);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> uuidLookup.response(uuid));
        }
    }

    public interface UUIDLookup {
        void response(UUID uuid);
    }

}
