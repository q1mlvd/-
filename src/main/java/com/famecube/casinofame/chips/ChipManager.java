package com.famecube.casinofame.chips;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ChipManager {
    private final JavaPlugin plugin;
    private FileConfiguration players;
    private File playersFile;

    public ChipManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        players = YamlConfiguration.loadConfiguration(playersFile);
    }

    public void save() {
        try {
            players.save(playersFile);
        } catch (IOException ignored) {
        }
    }

    public long getChips(UUID uuid) {
        return players.getLong(uuid.toString(), 0L);
    }

    public void setChips(UUID uuid, long amount) {
        players.set(uuid.toString(), Math.max(0L, amount));
        save();
    }

    public void addChips(UUID uuid, long amount) {
        setChips(uuid, getChips(uuid) + amount);
    }

    public boolean takeChips(UUID uuid, long amount) {
        long current = getChips(uuid);
        if (current < amount) {
            return false;
        }
        setChips(uuid, current - amount);
        return true;
    }
}
