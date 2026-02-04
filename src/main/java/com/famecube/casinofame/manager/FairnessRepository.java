package com.famecube.casinofame.manager;

import com.famecube.casinofame.util.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FairnessRepository {
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    private final JavaPlugin plugin;
    private FileConfiguration fairness;
    private final Map<String, FairnessRecord> cache = new HashMap<>();

    public FairnessRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.fairness = YamlFiles.loadOrCreateEmpty(plugin, "fairness.yml");
        load();
    }

    public void load() {
        cache.clear();
        ConfigurationSection section = fairness.getConfigurationSection("rounds");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            long timestamp = section.getLong(key + ".timestamp");
            long seed = section.getLong(key + ".seed");
            String game = section.getString(key + ".game", "");
            int bet = section.getInt(key + ".bet");
            String outcome = section.getString(key + ".outcome", "");
            int payout = section.getInt(key + ".payout");
            String playerUuid = section.getString(key + ".playerUuid", "");
            String playerName = section.getString(key + ".playerName", "");
            cache.put(key, new FairnessRecord(timestamp, key, seed, game, bet, outcome, payout, playerUuid, playerName));
        }
    }

    public void add(FairnessRecord record) {
        cache.put(record.getRoundId(), record);
        String base = "rounds." + record.getRoundId();
        fairness.set(base + ".timestamp", record.getTimestamp());
        fairness.set(base + ".seed", record.getSeed());
        fairness.set(base + ".game", record.getGame());
        fairness.set(base + ".bet", record.getBet());
        fairness.set(base + ".outcome", record.getOutcome());
        fairness.set(base + ".payout", record.getPayout());
        fairness.set(base + ".playerUuid", record.getPlayerUuid());
        fairness.set(base + ".playerName", record.getPlayerName());
        save();
    }

    public FairnessRecord get(String roundId) {
        return cache.get(roundId);
    }

    public void cleanupOld() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, FairnessRecord>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FairnessRecord> entry = iterator.next();
            if (now - entry.getValue().getTimestamp() > SEVEN_DAYS_MS) {
                fairness.set("rounds." + entry.getKey(), null);
                iterator.remove();
            }
        }
        save();
    }

    public void save() {
        YamlFiles.save(plugin, fairness, "fairness.yml");
    }

    public void reload() {
        fairness = YamlFiles.loadOrCreateEmpty(plugin, "fairness.yml");
        load();
    }
}
