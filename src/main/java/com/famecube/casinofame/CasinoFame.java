package com.famecube.casinofame;

import com.famecube.casinofame.chips.ChipManager;
import com.famecube.casinofame.economy.EconomyManager;
import com.famecube.casinofame.fairness.FairnessManager;
import com.famecube.casinofame.games.GameManager;
import com.famecube.casinofame.gui.BetGuiHelper;
import com.famecube.casinofame.gui.GuiManager;
import com.famecube.casinofame.listeners.InventoryListener;
import com.famecube.casinofame.listeners.PlayerListener;
import com.famecube.casinofame.util.ConfigManager;
import com.famecube.casinofame.util.MessageUtil;
import com.famecube.casinofame.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CasinoFame extends JavaPlugin implements CommandExecutor, TabCompleter {
    private ConfigManager configManager;
    private MessageUtil messageUtil;
    private ChipManager chipManager;
    private FairnessManager fairnessManager;
    private EconomyManager economyManager;
    private GuiManager guiManager;
    private BetGuiHelper betGuiHelper;
    private SoundUtil soundUtil;
    private GameManager gameManager;
    private InventoryListener inventoryListener;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configManager = new ConfigManager(this);
        configManager.load();
        messageUtil = new MessageUtil(configManager.getMessages());
        chipManager = new ChipManager(this);
        chipManager.load();
        fairnessManager = new FairnessManager(this);
        fairnessManager.load();
        fairnessManager.cleanupOld(7L * 24L * 60L * 60L * 1000L);
        economyManager = new EconomyManager(this);
        economyManager.setup();
        guiManager = new GuiManager(configManager.getGui(), chipManager);
        betGuiHelper = new BetGuiHelper(configManager.getGui());
        soundUtil = new SoundUtil(getConfig().getBoolean("sounds.enabled", true));
        gameManager = new GameManager(chipManager, fairnessManager, messageUtil, getConfig(), soundUtil, getConfig().getBoolean("particles.enabled", true));
        registerListeners();
        getCommand("casino").setExecutor(this);
        getCommand("casino").setTabCompleter(this);
        getCommand("chips").setExecutor(this);
        getCommand("chips").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        chipManager.save();
        fairnessManager.save();
    }

    private void registerListeners() {
        inventoryListener = new InventoryListener(guiManager, betGuiHelper, gameManager, chipManager, economyManager, messageUtil, soundUtil, getConfig());
        playerListener = new PlayerListener(gameManager, getConfig().getBoolean("refundOnQuit", true));
        Bukkit.getPluginManager().registerEvents(inventoryListener, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
    }

    private void unregisterListeners() {
        if (inventoryListener != null) {
            HandlerList.unregisterAll(inventoryListener);
        }
        if (playerListener != null) {
            HandlerList.unregisterAll(playerListener);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chips")) {
            if (!(sender instanceof Player)) {
                messageUtil.send(sender, "player-only");
                return true;
            }
            Player player = (Player) sender;
            if (!economyManager.isAvailable()) {
                messageUtil.send(player, "vault-missing");
                return true;
            }
            player.openInventory(guiManager.createShop(player, getRate()));
            return true;
        }
        if (command.getName().equalsIgnoreCase("casino")) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    messageUtil.send(sender, "player-only");
                    return true;
                }
                Player player = (Player) sender;
                player.openInventory(guiManager.createMain(player));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("casinofame.admin")) {
                    messageUtil.send(sender, "no-permission");
                    return true;
                }
                reloadAll();
                messageUtil.send(sender, "reloaded");
                return true;
            }
            if (args[0].equalsIgnoreCase("fairness") && args.length >= 2) {
                if (!sender.hasPermission("casinofame.admin")) {
                    messageUtil.send(sender, "no-permission");
                    return true;
                }
                handleFairness(sender, args[1]);
                return true;
            }
            if (args[0].equalsIgnoreCase("chips") && args.length >= 4) {
                if (!sender.hasPermission("casinofame.admin")) {
                    messageUtil.send(sender, "no-permission");
                    return true;
                }
                handleAdminChips(sender, args);
                return true;
            }
        }
        return true;
    }

    private void handleAdminChips(CommandSender sender, String[] args) {
        String action = args[1];
        String name = args[2];
        String amountStr = args[3];
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException ex) {
            messageUtil.send(sender, "invalid-number");
            return;
        }
        if (amount <= 0) {
            messageUtil.send(sender, "invalid-number");
            return;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            messageUtil.send(sender, "player-not-found");
            return;
        }
        if (action.equalsIgnoreCase("give")) {
            chipManager.addChips(target.getUniqueId(), amount);
            sender.sendMessage(messageUtil.get("chips-given").replace("%amount%", String.valueOf(amount)));
        } else if (action.equalsIgnoreCase("take")) {
            chipManager.takeChips(target.getUniqueId(), amount);
            sender.sendMessage(messageUtil.get("chips-taken").replace("%amount%", String.valueOf(amount)));
        }
    }

    private void handleFairness(CommandSender sender, String roundIdStr) {
        UUID roundId;
        try {
            roundId = UUID.fromString(roundIdStr);
        } catch (IllegalArgumentException ex) {
            messageUtil.send(sender, "fairness-not-found");
            return;
        }
        ConfigurationSection section = fairnessManager.getRound(roundId);
        if (section == null) {
            messageUtil.send(sender, "fairness-not-found");
            return;
        }
        String message = messageUtil.get("fairness-info")
                .replace("%round%", roundId.toString())
                .replace("%game%", translateGame(section.getString("game", "")))
                .replace("%bet%", String.valueOf(section.getLong("bet", 0L)))
                .replace("%payout%", String.valueOf(section.getLong("payout", 0L)))
                .replace("%outcome%", section.getString("outcome", ""))
                .replace("%seed%", String.valueOf(section.getLong("seed", 0L)))
                .replace("%time%", fairnessManager.formatTime(section.getLong("timestamp", 0L)));
        sender.sendMessage(message);
    }

    private String translateGame(String game) {
        if ("SLOTS".equalsIgnoreCase(game)) {
            return "Слоты";
        }
        if ("DICE".equalsIgnoreCase(game)) {
            return "Кости";
        }
        if ("COINFLIP".equalsIgnoreCase(game)) {
            return "Монетка";
        }
        if ("ROULETTE".equalsIgnoreCase(game)) {
            return "Рулетка";
        }
        return "Игра";
    }

    private void reloadAll() {
        configManager.reload();
        messageUtil = new MessageUtil(configManager.getMessages());
        guiManager.reload(configManager.getGui());
        betGuiHelper = new BetGuiHelper(configManager.getGui());
        soundUtil = new SoundUtil(getConfig().getBoolean("sounds.enabled", true));
        economyManager.setup();
        gameManager = new GameManager(chipManager, fairnessManager, messageUtil, getConfig(), soundUtil, getConfig().getBoolean("particles.enabled", true));
        unregisterListeners();
        registerListeners();
    }

    private double getRate() {
        return getConfig().getDouble("exchange.moneyPerChip", 1.0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("casino")) {
            if (args.length == 1) {
                return filter(Arrays.asList("reload", "fairness", "chips"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("chips")) {
                return filter(Arrays.asList("give", "take"), args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("chips")) {
                List<String> names = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    names.add(player.getName());
                }
                return filter(names, args[2]);
            }
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String start) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(start.toLowerCase())) {
                result.add(opt);
            }
        }
        return result;
    }
}
