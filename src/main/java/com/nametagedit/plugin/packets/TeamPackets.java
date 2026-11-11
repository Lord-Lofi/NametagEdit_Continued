package com.nametagedit.plugin.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.OptionData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import com.nametagedit.plugin.NametagHandler;
import com.nametagedit.plugin.api.data.FakeTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Sends client-side ("fake") scoreboard teams through PacketEvents.
 *
 * The teams only exist on players' clients. Nothing is added to the server's
 * scoreboard, so NametagEdit does not clash with sidebar or scoreboard plugins
 * and nothing is saved into the world data.
 */
public final class TeamPackets {

    // Prefixes and suffixes arrive already coloured with section signs (§),
    // including hex colours in the §x§R§R§G§G§B§B form produced by Utils.color().
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private TeamPackets() {
    }

    /** Creates the team, with its current members, for every online player. */
    public static void create(FakeTeam team) {
        sendToAll(createPacket(team));
    }

    /** Creates the team, with its current members, for one player (used when a player joins). */
    public static void create(FakeTeam team, Player viewer) {
        send(viewer, createPacket(team));
    }

    /** Removes the team for every online player. */
    public static void remove(String teamName) {
        sendToAll(new WrapperPlayServerTeams(teamName, TeamMode.REMOVE, (ScoreBoardTeamInfo) null, Collections.emptyList()));
    }

    /** Adds players to an existing team for every online player. */
    public static void addMembers(String teamName, Collection<String> players) {
        sendToAll(new WrapperPlayServerTeams(teamName, TeamMode.ADD_ENTITIES, (ScoreBoardTeamInfo) null, new ArrayList<>(players)));
    }

    /** Removes players from an existing team for every online player. */
    public static void removeMembers(String teamName, Collection<String> players) {
        sendToAll(new WrapperPlayServerTeams(teamName, TeamMode.REMOVE_ENTITIES, (ScoreBoardTeamInfo) null, new ArrayList<>(players)));
    }

    private static WrapperPlayServerTeams createPacket(FakeTeam team) {
        String prefix = team.getPrefix() == null ? "" : team.getPrefix();
        String suffix = team.getSuffix() == null ? "" : team.getSuffix();

        // The team colour sets the colour of the player's name. It uses the last
        // standard colour in the prefix, as before. Hex colours can't be team colours.
        LastColor last = lastColor(prefix);
        if (last.code != 0) {
            // Start the suffix in the same colour, matching the previous behaviour.
            suffix = LegacyComponentSerializer.SECTION_CHAR + String.valueOf(last.code) + suffix;
        }

        ScoreBoardTeamInfo info = new ScoreBoardTeamInfo(
                Component.text(team.getName()),
                LEGACY.deserialize(prefix),
                LEGACY.deserialize(suffix),
                team.isVisible() ? NameTagVisibility.ALWAYS : NameTagVisibility.NEVER,
                NametagHandler.DISABLE_PUSH_ALL_TAGS ? CollisionRule.NEVER : CollisionRule.ALWAYS,
                last.color != null ? last.color : NamedTextColor.WHITE,
                OptionData.FRIENDLY_FIRE
        );

        return new WrapperPlayServerTeams(team.getName(), TeamMode.CREATE, info, new ArrayList<>(team.getMembers()));
    }

    private static void sendToAll(WrapperPlayServerTeams packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, packet);
        }
    }

    private static void send(Player player, WrapperPlayServerTeams packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /** The last standard (0-9, a-f) colour code in a §-formatted string, if any. */
    private static LastColor lastColor(String text) {
        char code = 0;
        NamedTextColor color = null;
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != LegacyComponentSerializer.SECTION_CHAR) continue;
            char c = Character.toLowerCase(text.charAt(i + 1));
            if (c == 'x') {
                // §x§R§R§G§G§B§B hex colour: skip it; it replaces any earlier standard colour
                code = 0;
                color = null;
                i += 13;
            } else if (c == 'r') {
                code = 0;
                color = null;
                i++;
            } else {
                NamedTextColor named = namedColor(c);
                if (named != null) {
                    code = c;
                    color = named;
                }
                i++;
            }
        }
        return new LastColor(code, color);
    }

    private static NamedTextColor namedColor(char code) {
        switch (code) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            case 'f': return NamedTextColor.WHITE;
            default: return null;
        }
    }

    private static final class LastColor {
        private final char code;
        private final NamedTextColor color;

        private LastColor(char code, NamedTextColor color) {
            this.code = code;
            this.color = color;
        }
    }

}
