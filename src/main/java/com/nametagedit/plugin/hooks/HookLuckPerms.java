package com.nametagedit.plugin.hooks;

import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.INametag;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * LuckPerms integration.
 *
 * Always: refreshes a player's nametag when their LuckPerms data changes
 * (groups, permissions, prefixes, suffixes).
 *
 * Optionally (LuckPerms.UsePrefixSuffix in config.yml): uses the prefix and
 * suffix set in LuckPerms as the player's nametag.
 *
 * Only touch this class when LuckPerms is installed; it references LuckPerms classes.
 */
public class HookLuckPerms implements Listener {

    private final NametagHandler handler;

    public HookLuckPerms(NametagHandler handler) {
        this.handler = handler;
        EventBus eventBus = Bukkit.getServicesManager().load(LuckPerms.class).getEventBus();
        eventBus.subscribe(handler.getPlugin(), UserDataRecalculateEvent.class, this::onUserDataRecalculateEvent);
    }

    private void onUserDataRecalculateEvent(UserDataRecalculateEvent event) {
        User user = event.getUser();
        Player player = Bukkit.getPlayer(user.getUniqueId());
        if (player != null) {
            handler.applyTagToPlayer(player, false);
        }
    }

    /**
     * The player's LuckPerms prefix and suffix as a nametag, or null if
     * LuckPerms has neither for this player.
     */
    public static INametag getTag(Player player) {
        CachedMetaData meta = LuckPermsProvider.get().getPlayerAdapter(Player.class).getMetaData(player);
        String prefix = meta.getPrefix();
        String suffix = meta.getSuffix();
        if (prefix == null && suffix == null) {
            return null;
        }
        return new LuckPermsTag(prefix == null ? "" : prefix, suffix == null ? "" : suffix);
    }

    private static final class LuckPermsTag implements INametag {
        private final String prefix;
        private final String suffix;

        private LuckPermsTag(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getSuffix() {
            return suffix;
        }

        @Override
        public int getSortPriority() {
            return -1;
        }

        @Override
        public boolean isPlayerTag() {
            return false;
        }
    }

}
