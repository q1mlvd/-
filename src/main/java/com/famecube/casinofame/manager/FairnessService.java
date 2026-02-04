package com.famecube.casinofame.manager;

import com.famecube.casinofame.util.Msg;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class FairnessService {
    private final JavaPlugin plugin;
    private final FairnessRepository repository;

    public FairnessService(JavaPlugin plugin, FairnessRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public String createRoundId() {
        return UUID.randomUUID().toString();
    }

    public void record(Player player, String roundId, long seed, String game, int bet, String outcome, int payout) {
        long timestamp = System.currentTimeMillis();
        FairnessRecord record = new FairnessRecord(timestamp, roundId, seed, game, bet, outcome, payout, player.getUniqueId().toString(), player.getName());
        repository.add(record);
        logFairness(record);
    }

    public void logFairness(FairnessRecord record) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("logsEnabled", true)) {
            return;
        }
        File file = new File(plugin.getDataFolder(), "casino-fairness.log");
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(record.getTimestamp()));
        int net = record.getPayout() - record.getBet();
        String line = String.join(" | ",
                time,
                record.getPlayerName(),
                record.getPlayerUuid(),
                record.getGame(),
                String.valueOf(record.getBet()),
                record.getRoundId(),
                String.valueOf(record.getSeed()),
                record.getOutcome(),
                String.valueOf(record.getPayout()),
                String.valueOf(net)) + System.lineSeparator();
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(line);
        } catch (IOException e) {
            plugin.getLogger().warning(Msg.format("Failed to write fairness log: " + e.getMessage()));
        }
    }
}
