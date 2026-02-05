package com.famecube.casinofame.fairness;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public class FairnessManager {
    private final JavaPlugin plugin;
    private File fairnessFile;
    private FileConfiguration fairness;
    private File logFile;

    public FairnessManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        fairnessFile = new File(plugin.getDataFolder(), "fairness.yml");
        if (!fairnessFile.exists()) {
            try {
                fairnessFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        fairness = YamlConfiguration.loadConfiguration(fairnessFile);
        logFile = new File(plugin.getDataFolder(), "casino-fairness.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public void save() {
        try {
            fairness.save(fairnessFile);
        } catch (IOException ignored) {
        }
    }

    public void recordRound(UUID roundId, String playerName, UUID playerUuid, String game, long bet, long payout, long seed, String outcome) {
        long timestamp = System.currentTimeMillis();
        ConfigurationSection section = fairness.createSection(roundId.toString());
        section.set("player", playerName);
        section.set("playerUuid", playerUuid.toString());
        section.set("game", game);
        section.set("bet", bet);
        section.set("payout", payout);
        section.set("seed", seed);
        section.set("outcome", outcome);
        section.set("timestamp", timestamp);
        save();
        appendLog(timestamp, playerName, playerUuid, game, bet, roundId, seed, outcome, payout);
    }

    private void appendLog(long timestamp, String playerName, UUID playerUuid, String game, long bet, UUID roundId, long seed, String outcome, long payout) {
        long net = payout - bet;
        String line = timestamp + " | " + playerName + " | " + playerUuid + " | " + game + " | " + bet + " | " + roundId + " | " + seed + " | " + outcome + " | " + payout + " | " + net + System.lineSeparator();
        try {
            Files.write(logFile.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public ConfigurationSection getRound(UUID roundId) {
        return fairness.getConfigurationSection(roundId.toString());
    }

    public void cleanupOld(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        Iterator<String> keys = fairness.getKeys(false).iterator();
        boolean changed = false;
        while (keys.hasNext()) {
            String key = keys.next();
            ConfigurationSection section = fairness.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            long timestamp = section.getLong("timestamp", 0L);
            if (timestamp < cutoff) {
                fairness.set(key, null);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public String formatTime(long timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(timestamp));
    }
}
