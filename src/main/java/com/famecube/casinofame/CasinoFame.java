package com.famecube.casinofame;

import com.famecube.casinofame.command.CasinoCommand;
import com.famecube.casinofame.command.ChipsCommand;
import com.famecube.casinofame.listener.InventoryListener;
import com.famecube.casinofame.listener.PlayerQuitListener;
import com.famecube.casinofame.manager.ChipManager;
import com.famecube.casinofame.manager.EconomyHook;
import com.famecube.casinofame.manager.FairnessRepository;
import com.famecube.casinofame.manager.FairnessService;
import com.famecube.casinofame.manager.GameManager;
import com.famecube.casinofame.manager.GuiManager;
import com.famecube.casinofame.util.Msg;
import com.famecube.casinofame.util.YamlFiles;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class CasinoFame extends JavaPlugin {
    private ChipManager chipManager;
    private FairnessRepository fairnessRepository;
    private FairnessService fairnessService;
    private GuiManager guiManager;
    private GameManager gameManager;
    private EconomyHook economyHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration messages = YamlFiles.loadOrCreate(this, "messages.yml");
        if (!messages.contains("prefix")) {
            YamlFiles.loadOrReplace(this, "messages.yml");
            messages = YamlFiles.loadOrCreate(this, "messages.yml");
        }
        YamlFiles.loadOrCreate(this, "gui.yml");
        YamlFiles.loadOrCreateEmpty(this, "players.yml");
        YamlFiles.loadOrCreateEmpty(this, "fairness.yml");
        createLogFile();
        Msg.init(messages);

        economyHook = new EconomyHook(this);
        economyHook.setup();

        chipManager = new ChipManager(this);
        fairnessRepository = new FairnessRepository(this);
        fairnessService = new FairnessService(this, fairnessRepository);
        gameManager = new GameManager();
        guiManager = new GuiManager(this, chipManager, gameManager, economyHook, fairnessService);

        fairnessRepository.cleanupOld();

        getCommand("casino").setExecutor(new CasinoCommand(guiManager, chipManager, fairnessRepository));
        getCommand("chips").setExecutor(new ChipsCommand(guiManager, economyHook));

        Bukkit.getPluginManager().registerEvents(new InventoryListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(gameManager, chipManager, getConfig()), this);
    }

    private void createLogFile() {
        File file = new File(getDataFolder(), "casino-fairness.log");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Failed to create casino-fairness.log: " + e.getMessage());
            }
        }
    }
}
