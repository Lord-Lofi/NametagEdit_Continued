package com.nametagedit.plugin.packets;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class PacketAccessor {

    // Spigot: "v1_21_R1" etc.  Paper 1.20.5+: null (no versioned CraftBukkit package).
    protected static String VERSION;
    protected static int MINOR_VERSION;
    // True on Paper 1.20.5+ which uses Mojang-mapped NMS class/field names.
    protected static boolean PAPER_NEW;

    private static final List<String> legacyVersions = Arrays.asList(
            "v1_7_R1", "v1_7_R2", "v1_7_R3", "v1_7_R4",
            "v1_8_R1", "v1_8_R2", "v1_8_R3",
            "v1_9_R1", "v1_9_R2",
            "v1_10_R1", "v1_11_R1", "v1_12_R1");
    private static boolean CAULDRON_SERVER = false;
    private static boolean LEGACY_SERVER   = false;

    private static Object UNSAFE;
    private static Method ALLOCATE_INSTANCE;

    static Field MEMBERS;
    static Field PREFIX;
    static Field SUFFIX;
    static Field TEAM_NAME;
    static Field PARAM_INT;
    static Field PACK_OPTION;
    static Field DISPLAY_NAME;
    static Field TEAM_COLOR;
    static Field PUSH;
    static Field VISIBILITY;
    // 1.17+
    static Field PARAMS;
    // Paper 1.20.5+: enum constants for Team.CollisionRule / Team.Visibility
    // (those fields use proper enum types rather than raw Strings on Paper)
    static Object PUSH_ENUM_ALWAYS;
    static Object PUSH_ENUM_NEVER;
    static Object VISIBILITY_ENUM_ALWAYS;
    static Object VISIBILITY_ENUM_NEVER;

    private static Method getHandle;
    private static Method sendPacket;
    private static Field playerConnection;

    private static Class<?> packetClass;
    // 1.17+
    private static Class<?> packetParamsClass;

    static {
        // ── Version detection ──────────────────────────────────────────────────────
        // Paper 1.20.5+ removed the version segment from the CraftBukkit package:
        //   Spigot → "org.bukkit.craftbukkit.v1_21_R1"  (4 parts, part[3] = "v1_21_R1")
        //   Paper  → "org.bukkit.craftbukkit"            (3 parts, no version segment)
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        String[] pkgParts = pkg.split("\\.");
        if (pkgParts.length > 3 && pkgParts[3].startsWith("v")) {
            VERSION      = pkgParts[3];
            MINOR_VERSION = Integer.parseInt(VERSION.split("_")[1]);
            PAPER_NEW    = false;
        } else {
            VERSION      = null;
            String bv    = Bukkit.getBukkitVersion().split("-")[0]; // e.g. "1.21.11"
            MINOR_VERSION = Integer.parseInt(bv.split("\\.")[1]);   // 21
            PAPER_NEW    = true;
        }

        // ── Cauldron detection ─────────────────────────────────────────────────────
        try {
            Class.forName("cpw.mods.fml.common.Mod");
            CAULDRON_SERVER = true;
        } catch (ClassNotFoundException ignored) {}

        // ── Main reflection setup ──────────────────────────────────────────────────
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            UNSAFE            = theUnsafeField.get(null);
            ALLOCATE_INSTANCE = UNSAFE.getClass().getMethod("allocateInstance", Class.class);

            if (VERSION != null && legacyVersions.contains(VERSION))
                LEGACY_SERVER = true;

            // CraftPlayer.getHandle() — path differs between Spigot and Paper
            Class<?> typeCraftPlayer = Class.forName(craftPath("entity.CraftPlayer"));
            getHandle = typeCraftPlayer.getMethod("getHandle");

            if (CAULDRON_SERVER) {
                // ── Cauldron (legacy Forge/Bukkit hybrid) ──────────────────────
                packetClass = Class.forName("net.minecraft.server.v1_7_R4.PacketPlayOutScoreboardTeam");
                Class<?> typeNMSPlayer       = Class.forName("net.minecraft.server.v1_7_R4.EntityPlayer");
                Class<?> typePlayerConn      = Class.forName("net.minecraft.server.v1_7_R4.PlayerConnection");
                playerConnection = typeNMSPlayer.getField("field_71135_a");
                sendPacket = typePlayerConn.getMethod("func_147359_a",
                        Class.forName("net.minecraft.server.v1_7_R4.Packet"));

            } else if (!isParamsVersion()) {
                // ── Spigot pre-1.17 (versioned NMS package) ────────────────────
                packetClass = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutScoreboardTeam");
                Class<?> typeNMSPlayer  = Class.forName("net.minecraft.server." + VERSION + ".EntityPlayer");
                Class<?> typePlayerConn = Class.forName("net.minecraft.server." + VERSION + ".PlayerConnection");
                playerConnection = typeNMSPlayer.getField("playerConnection");
                sendPacket = typePlayerConn.getMethod("sendPacket",
                        Class.forName("net.minecraft.server." + VERSION + ".Packet"));

            } else if (!PAPER_NEW) {
                // ── Spigot 1.17+ (Spigot-obfuscated names) ─────────────────────
                packetClass      = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam");
                packetParamsClass = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam$b");
                Class<?> typeNMSPlayer  = Class.forName("net.minecraft.server.level.EntityPlayer");
                Class<?> typePlayerConn = Class.forName("net.minecraft.server.network.PlayerConnection");

                String connField = MINOR_VERSION >= 20 ? "c" : "b";
                try {
                    playerConnection = typeNMSPlayer.getField(connField);
                } catch (NoSuchFieldException ignored) {
                    playerConnection = findFieldByType(typeNMSPlayer, typePlayerConn);
                    if (playerConnection == null)
                        throw new NoSuchFieldException(
                                "Cannot find playerConnection in EntityPlayer for MC 1." + MINOR_VERSION);
                }
                sendPacket = findSendMethod(typePlayerConn);

            } else {
                // ── Paper 1.20.5+ (Mojang-mapped names) ────────────────────────
                packetClass       = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
                packetParamsClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters");
                Class<?> typeNMSPlayer  = Class.forName("net.minecraft.server.level.ServerPlayer");
                Class<?> typePlayerConn = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");

                try {
                    playerConnection = typeNMSPlayer.getField("connection");
                } catch (NoSuchFieldException ignored) {
                    playerConnection = findFieldByType(typeNMSPlayer, typePlayerConn);
                    if (playerConnection == null)
                        throw new NoSuchFieldException("Cannot find connection field in ServerPlayer");
                }
                sendPacket = findSendMethod(typePlayerConn);
            }

            // ── Field mapping ──────────────────────────────────────────────────
            if (PAPER_NEW) {
                // Mojang-mapped field names (Paper 1.20.5+)
                PARAM_INT    = getNMS("method");
                TEAM_NAME    = getNMS("name");
                MEMBERS      = getNMS("players");
                PARAMS       = getNMS("parameters");
                PREFIX       = getParamNMS("playerPrefix");
                SUFFIX       = getParamNMS("playerSuffix");
                PACK_OPTION  = getParamNMS("options");
                DISPLAY_NAME = getParamNMS("displayName");
                TEAM_COLOR   = getParamNMS("color");
                PUSH         = getParamNMS("collisionRule");
                VISIBILITY   = getParamNMS("nametagVisibility");
                // Pre-load enum constants — Paper uses Team.CollisionRule / Team.Visibility
                // instead of raw Strings for these fields.
                PUSH_ENUM_ALWAYS        = getEnumConst(PUSH,       "ALWAYS");
                PUSH_ENUM_NEVER         = getEnumConst(PUSH,       "NEVER");
                VISIBILITY_ENUM_ALWAYS  = getEnumConst(VISIBILITY, "ALWAYS");
                VISIBILITY_ENUM_NEVER   = getEnumConst(VISIBILITY, "NEVER");
            } else {
                // Spigot obfuscated names via PacketData lookup
                PacketData currentVersion = null;
                for (PacketData pd : PacketData.values()) {
                    if (VERSION != null && VERSION.contains(pd.name()))
                        currentVersion = pd;
                }
                if (CAULDRON_SERVER) currentVersion = PacketData.cauldron;
                if (currentVersion == null && isParamsVersion()) {
                    System.err.println("[NametagEdit] No PacketData for VERSION=" + VERSION
                            + " — falling back to v1_21 field mappings.");
                    currentVersion = PacketData.v1_21;
                }

                if (currentVersion != null) {
                    if (!isParamsVersion()) {
                        PREFIX       = getNMS(currentVersion.getPrefix());
                        SUFFIX       = getNMS(currentVersion.getSuffix());
                        MEMBERS      = getNMS(currentVersion.getMembers());
                        TEAM_NAME    = getNMS(currentVersion.getTeamName());
                        PARAM_INT    = getNMS(currentVersion.getParamInt());
                        PACK_OPTION  = getNMS(currentVersion.getPackOption());
                        DISPLAY_NAME = getNMS(currentVersion.getDisplayName());
                        if (!isLegacyVersion())   TEAM_COLOR = getNMS(currentVersion.getColor());
                        if (isPushVersion())       PUSH       = getNMS(currentVersion.getPush());
                        if (isVisibilityVersion()) VISIBILITY = getNMS(currentVersion.getVisibility());
                    } else {
                        PARAM_INT    = getNMS(currentVersion.getParamInt());
                        TEAM_NAME    = getNMS(currentVersion.getTeamName());
                        MEMBERS      = getNMS(currentVersion.getMembers());
                        PARAMS       = getNMS(currentVersion.getParams());
                        PREFIX       = getParamNMS(currentVersion.getPrefix());
                        SUFFIX       = getParamNMS(currentVersion.getSuffix());
                        PACK_OPTION  = getParamNMS(currentVersion.getPackOption());
                        DISPLAY_NAME = getParamNMS(currentVersion.getDisplayName());
                        TEAM_COLOR   = getParamNMS(currentVersion.getColor());
                        PUSH         = getParamNMS(currentVersion.getPush());
                        VISIBILITY   = getParamNMS(currentVersion.getVisibility());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a fully-qualified CraftBukkit class name, handling Paper's unversioned package.
     *   Spigot: "org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer"
     *   Paper:  "org.bukkit.craftbukkit.entity.CraftPlayer"
     */
    static String craftPath(String suffix) {
        return VERSION != null
                ? "org.bukkit.craftbukkit." + VERSION + "." + suffix
                : "org.bukkit.craftbukkit." + suffix;
    }

    public static boolean isLegacyVersion()     { return LEGACY_SERVER; }
    public static boolean isParamsVersion()      { return MINOR_VERSION >= 17; }
    private static boolean isPushVersion()       { return MINOR_VERSION >= 9; }
    private static boolean isVisibilityVersion() { return MINOR_VERSION >= 8; }

    private static Field getNMS(String path) throws Exception {
        Field f = packetClass.getDeclaredField(path);
        f.setAccessible(true);
        return f;
    }

    private static Field getParamNMS(String path) throws Exception {
        Field f = packetParamsClass.getDeclaredField(path);
        f.setAccessible(true);
        return f;
    }

    private static Field findFieldByType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Method findSendMethod(Class<?> connClass) throws Exception {
        Class<?> packetIface = Class.forName("net.minecraft.network.protocol.Packet");
        Class<?>[] params = {packetIface};
        return Stream.concat(
                        Arrays.stream(connClass.getSuperclass().getMethods()),
                        Arrays.stream(connClass.getMethods()))
                .filter(m -> Arrays.equals(m.getParameterTypes(), params))
                .findFirst().orElseThrow(NoSuchMethodException::new);
    }

    static Object createPacket() {
        try {
            return isParamsVersion()
                    ? ALLOCATE_INSTANCE.invoke(UNSAFE, packetClass)
                    : packetClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Object createPacketParams() {
        try {
            return isParamsVersion()
                    ? ALLOCATE_INSTANCE.invoke(UNSAFE, packetParamsClass)
                    : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void sendPacket(Collection<? extends Player> players, Object packet) {
        for (Player player : players) sendPacket(player, packet);
    }

    static void sendPacket(Player player, Object packet) {
        try {
            Object nmsPlayer = getHandle.invoke(player);
            Object connection = playerConnection.get(nmsPlayer);
            sendPacket.invoke(connection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the named enum constant from the declared type of {@code f}, or
     * {@code null} if the field is null, its type is not an enum, or the
     * constant does not exist.
     */
    @SuppressWarnings("unchecked")
    private static Object getEnumConst(Field f, String name) {
        if (f == null) return null;
        Class<?> type = f.getType();
        if (!type.isEnum()) return null;
        try {
            return Enum.valueOf((Class<Enum>) type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
