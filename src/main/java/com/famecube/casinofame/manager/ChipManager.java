package com.famecube.casinofame.manager;

import com.famecube.casinofame.util.YamlFiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ChipManager {
    private final JavaPlugin plugin;
    private FileConfiguration players;

    public ChipManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.players = YamlFiles.loadOrCreateEmpty(plugin, "players.yml");
    }

    public int getChips(UUID uuid) {
        return players.getInt(uuid.toString() + ".chips", 0);
    }

    public void setChips(UUID uuid, int amount) {
        players.set(uuid.toString() + ".chips", Math.max(0, amount));
        save();
    }

    public void addChips(UUID uuid, int amount) {
        setChips(uuid, getChips(uuid) + Math.max(0, amount));
    }

    public boolean removeChips(UUID uuid, int amount) {
        int current = getChips(uuid);
        if (current < amount) {
            return false;
        }
        setChips(uuid, current - amount);
        return true;
    }

    public void save() {
        YamlFiles.save(plugin, players, "players.yml");
    }

    public void reload() {
        players = YamlFiles.loadOrCreateEmpty(plugin, "players.yml");
    }
}
