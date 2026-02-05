package com.famecube.casinofame.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private FileConfiguration gui;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        messages = loadConfig("messages.yml");
        gui = loadConfig("gui.yml");
    }

    public void reload() {
        plugin.reloadConfig();
        messages = loadConfig("messages.yml");
        gui = loadConfig("gui.yml");
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getGui() {
        return gui;
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig(FileConfiguration config, String name) {
        File file = new File(plugin.getDataFolder(), name);
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }
}
