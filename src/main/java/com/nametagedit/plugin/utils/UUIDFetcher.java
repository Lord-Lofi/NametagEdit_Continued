package com.nametagedit.plugin.utils;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * This class is responsible for retrieving UUIDs from Names
 *
 * @author evilmidget38
 */
public class UUIDFetcher implements Callable<Map<String, UUID>> {

    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private final List<String> names;
    private final boolean rateLimiting;

    private UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.names = ImmutableList.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    private UUIDFetcher(List<String> names) {
        this(names, true);
    }

    public static void lookupUUID(final String name, final Plugin plugin, final UUIDLookup uuidLookup) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID response = null;
                try {
                    response = getUUIDOf(name);
                } catch (Exception e) {
                    // Swallow
                }

                final UUID finalResponse = response;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        uuidLookup.response(finalResponse);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body.getBytes(StandardCharsets.UTF_8));
            stream.flush();
        }
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URL(PROFILE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }

    private static UUID getUUIDOf(String name) throws Exception {
        return new UUIDFetcher(Collections.singletonList(name)).call().get(name);
    }

    private static String toJsonArray(List<String> names) {
        JsonArray array = new JsonArray();
        for (String name : names) array.add(name);
        return array.toString();
    }

    @Override
    public Map<String, UUID> call() throws Exception {
        Map<String, UUID> uuidMap = new HashMap<>();
        int requests = (int) Math.ceil(names.size() / PROFILES_PER_REQUEST);
        for (int i = 0; i < requests; i++) {
            HttpURLConnection connection = createConnection();
            String body = toJsonArray(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
            writeBody(connection, body);
            JsonArray array = JsonParser.parseReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
                    .getAsJsonArray();

            for (JsonElement element : array) {
                JsonObject profile = element.getAsJsonObject();
                String id = profile.get("id").getAsString();
                String name = profile.get("name").getAsString();
                uuidMap.put(name, getUUID(id));
            }

            if (rateLimiting && i != requests - 1) {
                Thread.sleep(100L);
            }
        }
        return uuidMap;
    }

    public interface UUIDLookup {
        void response(UUID uuid);
    }

}
