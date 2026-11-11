package com.nametagedit.plugin.hooks;

import com.nametagedit.plugin.NametagEdit;
import com.nametagedit.plugin.api.data.FakeTeam;
import com.nametagedit.plugin.api.data.GroupData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * NametagEdit's own PlaceholderAPI placeholders, for other plugins to use:
 *
 *   %nametagedit_prefix%  the prefix currently shown above the player's head
 *   %nametagedit_suffix%  the suffix currently shown above the player's head
 *   %nametagedit_group%   the NametagEdit group the player matches (empty if none)
 *
 * Only touch this class when PlaceholderAPI is installed; it references PlaceholderAPI classes.
 */
public class NametagExpansion extends PlaceholderExpansion {

    private final NametagEdit plugin;

    public NametagExpansion(NametagEdit plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nametagedit";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    // Keep the expansion registered when PlaceholderAPI reloads.
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getName() == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "prefix": {
                FakeTeam team = plugin.getManager().getFakeTeam(player.getName());
                return team == null || team.getPrefix() == null ? "" : team.getPrefix();
            }
            case "suffix": {
                FakeTeam team = plugin.getManager().getFakeTeam(player.getName());
                return team == null || team.getSuffix() == null ? "" : team.getSuffix();
            }
            case "group": {
                Player online = player.getPlayer();
                if (online == null) return "";
                for (GroupData group : plugin.getHandler().getGroupData()) {
                    if (online.hasPermission(group.getBukkitPermission())) {
                        return group.getGroupName();
                    }
                }
                return "";
            }
            default:
                return null; // unknown placeholder
        }
    }

}
