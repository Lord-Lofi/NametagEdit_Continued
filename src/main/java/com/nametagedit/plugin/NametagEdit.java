package com.nametagedit.plugin;

import com.nametagedit.plugin.api.INametagApi;
import com.nametagedit.plugin.api.NametagAPI;
import com.nametagedit.plugin.hooks.*;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TODO:
 * - Better uniform message format + more messages
 * - Code cleanup
 * - Add language support
 */
@Getter
public class NametagEdit extends JavaPlugin {

    private static NametagEdit instance;

    private static INametagApi api;

    private NametagHandler handler;
    private NametagManager manager;

    public static INametagApi getApi() {
        return api;
    }

    @Override
    public void onEnable() {
        instance = this;

        manager = new NametagManager(this);
        handler = new NametagHandler(this, manager);

        PluginManager pluginManager = Bukkit.getPluginManager();

        if (checkShouldRegister("LuckPerms")) {
            pluginManager.registerEvents(new HookLuckPerms(handler), this);
        }

        getCommand("ne").setExecutor(new NametagCommand(handler));

        if (api == null) {
            api = new NametagAPI(handler, manager);
        }
    }

    public static NametagEdit getInstance(){
        return instance;
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.reset();
        if (handler != null && handler.getAbstractConfig() != null) handler.getAbstractConfig().shutdown();
    }

    void debug(String message) {
        if (handler != null && handler.debug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private boolean checkShouldRegister(String plugin) {
        if (Bukkit.getPluginManager().getPlugin(plugin) == null) return false;
        getLogger().info("Found " + plugin + "! Hooking in.");
        return true;
    }

}