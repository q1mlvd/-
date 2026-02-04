package com.famecube.casinofame.manager;

import com.famecube.casinofame.CasinoFame;
import com.famecube.casinofame.game.CoinFlipGame;
import com.famecube.casinofame.game.DiceGame;
import com.famecube.casinofame.game.GameResult;
import com.famecube.casinofame.game.RouletteGame;
import com.famecube.casinofame.game.SlotsGame;
import com.famecube.casinofame.util.ActionBar;
import com.famecube.casinofame.util.CC;
import com.famecube.casinofame.util.Msg;
import com.famecube.casinofame.util.SoundUtil;
import com.famecube.casinofame.util.YamlFiles;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GuiManager {
    private final CasinoFame plugin;
    private final ChipManager chipManager;
    private final GameManager gameManager;
    private final EconomyHook economyHook;
    private final FairnessService fairnessService;
    private final NamespacedKey actionKey;
    private FileConfiguration gui;
    private final Map<String, String> actionSounds = new HashMap<>();
    private SlotsGame slotsGame;
    private final DiceGame diceGame = new DiceGame();
    private final CoinFlipGame coinFlipGame = new CoinFlipGame();
    private final RouletteGame rouletteGame = new RouletteGame();

    public GuiManager(CasinoFame plugin, ChipManager chipManager, GameManager gameManager, EconomyHook economyHook, FairnessService fairnessService) {
        this.plugin = plugin;
        this.chipManager = chipManager;
        this.gameManager = gameManager;
        this.economyHook = economyHook;
        this.fairnessService = fairnessService;
        this.actionKey = new NamespacedKey(plugin, "cf_action");
        reload();
    }

    public CasinoFame getPlugin() {
        return plugin;
    }

    public NamespacedKey getActionKey() {
        return actionKey;
    }

    public void reload() {
        this.gui = YamlFiles.loadOrCreate(plugin, "gui.yml");
        if (gui.getConfigurationSection("main.items") == null) {
            YamlFiles.loadOrReplace(plugin, "gui.yml");
            this.gui = YamlFiles.loadOrCreate(plugin, "gui.yml");
        }
        this.slotsGame = new SlotsGame(plugin.getConfig());
        rebuildActionSounds();
    }

    public FileConfiguration getGui() {
        return gui;
    }

    public void openMain(Player player) {
        Inventory inv = buildMenu("main", player, null);
        player.openInventory(inv);
    }

    public void openShop(Player player) {
        Inventory inv = buildMenu("shop", player, null);
        player.openInventory(inv);
    }

    public void openSlots(Player player) {
        BetSession session = gameManager.getSession(player, "SLOTS");
        Inventory inv = buildMenu("slots", player, session);
        player.openInventory(inv);
    }

    public void openDice(Player player) {
        BetSession session = gameManager.getSession(player, "DICE");
        Inventory inv = buildMenu("dice", player, session);
        player.openInventory(inv);
    }

    public void openCoinFlip(Player player) {
        BetSession session = gameManager.getSession(player, "COINFLIP");
        Inventory inv = buildMenu("coinflip", player, session);
        player.openInventory(inv);
    }

    public void openRoulette(Player player) {
        BetSession session = gameManager.getSession(player, "ROULETTE");
        Inventory inv = buildMenu("roulette", player, session);
        player.openInventory(inv);
    }

    public void handleAction(Player player, String action, String menuType) {
        String sound = actionSounds.get(action);
        if (sound != null) {
            SoundUtil.play(player, sound);
        }
        switch (action) {
            case "open_slots":
                if (!isGameEnabled("slots")) {
                    Msg.send(player, "game-disabled");
                    return;
                }
                if (!economyHook.isEnabled()) {
                    Msg.send(player, "vault-missing");
                    return;
                }
                openSlots(player);
                return;
            case "open_dice":
                if (!isGameEnabled("dice")) {
                    Msg.send(player, "game-disabled");
                    return;
                }
                if (!economyHook.isEnabled()) {
                    Msg.send(player, "vault-missing");
                    return;
                }
                openDice(player);
                return;
            case "open_coinflip":
                if (!isGameEnabled("coinflip")) {
                    Msg.send(player, "game-disabled");
                    return;
                }
                if (!economyHook.isEnabled()) {
                    Msg.send(player, "vault-missing");
                    return;
                }
                openCoinFlip(player);
                return;
            case "open_roulette":
                if (!isGameEnabled("roulette")) {
                    Msg.send(player, "game-disabled");
                    return;
                }
                if (!economyHook.isEnabled()) {
                    Msg.send(player, "vault-missing");
                    return;
                }
                openRoulette(player);
                return;
            case "open_shop":
                if (!economyHook.isEnabled()) {
                    Msg.send(player, "shop-disabled");
                    return;
                }
                openShop(player);
                return;
            case "back_main":
                openMain(player);
                return;
            case "close":
                player.closeInventory();
                return;
            default:
                break;
        }

        if (action.startsWith("bet_")) {
            adjustBet(player, action);
            return;
        }

        if (action.startsWith("dice_")) {
            setSelection(player, "DICE", action.equals("dice_under") ? "UNDER" : action.equals("dice_over") ? "OVER" : "EXACT");
            return;
        }

        if (action.startsWith("coin_")) {
            setSelection(player, "COINFLIP", action.equals("coin_heads") ? "HEADS" : "TAILS");
            return;
        }

        if (action.startsWith("roulette_")) {
            setSelection(player, "ROULETTE", action.replace("roulette_", "").toUpperCase());
            return;
        }

        if (action.startsWith("start_")) {
            startGame(player, action.replace("start_", "").toUpperCase());
            return;
        }

        if (action.startsWith("buy_") || action.startsWith("sell_")) {
            handleShop(player, action);
        }
    }

    private void adjustBet(Player player, String action) {
        BetSession session = gameManager.getActiveSession(player);
        if (session == null) {
            return;
        }
        int min = plugin.getConfig().getInt("minBetChips");
        int max = plugin.getConfig().getInt("maxBetChips");
        int bet = session.getBet();
        switch (action) {
            case "bet_100":
                bet += 100;
                break;
            case "bet_500":
                bet += 500;
                break;
            case "bet_1000":
                bet += 1000;
                break;
            case "bet_5000":
                bet += 5000;
                break;
            case "bet_max":
                bet = max;
                Msg.send(player, "bet-max", Msg.map("bet", String.valueOf(bet)));
                break;
            case "bet_clear":
                bet = 0;
                Msg.send(player, "bet-cleared");
                break;
            default:
                break;
        }
        bet = Math.min(bet, max);
        session.setBet(bet);
        if (!action.equals("bet_max") && !action.equals("bet_clear")) {
            Msg.send(player, "bet-set", Msg.map("bet", String.valueOf(bet)));
        }
        refreshCurrentMenu(player, session.getGame());
    }

    private void setSelection(Player player, String game, String selection) {
        BetSession session = gameManager.getSession(player, game);
        session.setSelection(selection);
        Msg.send(player, "selection-set", Msg.map("selection", selection));
    }

    private void startGame(Player player, String game) {
        if (!economyHook.isEnabled()) {
            Msg.send(player, "vault-missing");
            return;
        }
        if (!isGameEnabled(game.toLowerCase())) {
            Msg.send(player, "game-disabled");
            return;
        }
        BetSession session = gameManager.getSession(player, game);
        if (session.isInProgress()) {
            return;
        }
        long cooldownMs = plugin.getConfig().getLong("cooldownSeconds") * 1000L;
        if (!player.hasPermission("casinofame.bypass.cooldown")) {
            long last = gameManager.getCooldown(player);
            long now = System.currentTimeMillis();
            long remain = (last + cooldownMs) - now;
            if (remain > 0) {
                long seconds = Math.max(1, remain / 1000L);
                Msg.send(player, "cooldown", Msg.map("seconds", String.valueOf(seconds)));
                return;
            }
        }
        int bet = session.getBet();
        int min = plugin.getConfig().getInt("minBetChips");
        int max = plugin.getConfig().getInt("maxBetChips");
        if (bet < min) {
            Msg.send(player, "bet-too-low", Msg.map("min", String.valueOf(min)));
            return;
        }
        if (bet > max) {
            Msg.send(player, "bet-too-high", Msg.map("max", String.valueOf(max)));
            return;
        }
        if (!chipManager.removeChips(player.getUniqueId(), bet)) {
            Msg.send(player, "not-enough-chips");
            return;
        }
        session.setInProgress(true);
        gameManager.setCooldown(player, System.currentTimeMillis());
        ActionBar.send(player, Msg.get("round-start"));
        String roundId = fairnessService.createRoundId();
        long seed = new Random().nextLong();
        int delayTicks = 20 + new Random().nextInt(40);
        startAnimation(player, delayTicks);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !session.isInProgress()) {
                    return;
                }
                double houseEdge = plugin.getConfig().getDouble("houseEdgePercent", 0.0);
                Random random = new Random(seed);
                GameResult result = playGame(game, random, bet, session.getSelection(), houseEdge);
                int payout = result.getPayout();
                if (payout > 0) {
                    chipManager.addChips(player.getUniqueId(), payout);
                    Msg.send(player, "round-win", Msg.map("payout", String.valueOf(payout), "round", roundId));
                    SoundUtil.play(player, gui.getString("results.win.sound"));
                } else {
                    Msg.send(player, "round-lose", Msg.map("bet", String.valueOf(bet), "round", roundId));
                    SoundUtil.play(player, gui.getString("results.lose.sound"));
                }
                Msg.send(player, "round-result", Msg.map("outcome", result.getOutcome()));
                fairnessService.record(player, roundId, seed, game, bet, result.getOutcome(), payout);
                session.setInProgress(false);
                refreshCurrentMenu(player, game);
            }
        }.runTaskLater(plugin, delayTicks);
    }

    private void startAnimation(Player player, int totalTicks) {
        List<String> frames = Msg.getList("animation-frames");
        if (frames == null || frames.isEmpty()) {
            return;
        }
        int interval = 5;
        int maxRuns = Math.max(1, totalTicks / interval);
        new BukkitRunnable() {
            int index = 0;
            int runs = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                ActionBar.send(player, frames.get(index % frames.size()));
                index++;
                runs++;
                if (runs >= maxRuns) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private GameResult playGame(String game, Random random, int bet, String selection, double houseEdge) {
        switch (game.toUpperCase()) {
            case "SLOTS":
                return slotsGame.play(random, bet, houseEdge);
            case "DICE":
                return diceGame.play(random, bet, selection == null ? "" : selection, houseEdge);
            case "COINFLIP":
                return coinFlipGame.play(random, bet, selection == null ? "" : selection, houseEdge);
            case "ROULETTE":
                return rouletteGame.play(random, bet, selection == null ? "" : selection, houseEdge);
            default:
                return new GameResult("UNKNOWN", 0);
        }
    }

    private void handleShop(Player player, String action) {
        if (!economyHook.isEnabled()) {
            Msg.send(player, "shop-disabled");
            return;
        }
        Economy economy = economyHook.getEconomy();
        int chips = Integer.parseInt(action.replaceAll("\\D", ""));
        double rate = plugin.getConfig().getDouble("chipRateMoneyPer1", 10.0);
        double buyFee = plugin.getConfig().getDouble("buyFeePercent", 0.0);
        double sellFee = plugin.getConfig().getDouble("sellFeePercent", 0.0);
        if (action.startsWith("buy_")) {
            double cost = chips * rate * (1.0 + buyFee / 100.0);
            if (!economy.has(player, cost)) {
                Msg.send(player, "shop-not-enough-money");
                return;
            }
            economy.withdrawPlayer(player, cost);
            chipManager.addChips(player.getUniqueId(), chips);
            Msg.send(player, "shop-buy-success", Msg.map("chips", String.valueOf(chips), "money", String.format("%.2f", cost)));
        } else {
            if (!chipManager.removeChips(player.getUniqueId(), chips)) {
                Msg.send(player, "shop-not-enough-chips");
                return;
            }
            double payout = chips * rate * (1.0 - sellFee / 100.0);
            economy.depositPlayer(player, payout);
            Msg.send(player, "shop-sell-success", Msg.map("chips", String.valueOf(chips), "money", String.format("%.2f", payout)));
        }
        refreshCurrentMenu(player, "shop");
    }

    private Inventory buildMenu(String menuKey, Player player, BetSession session) {
        String title = CC.color(gui.getString(menuKey + ".title", ""));
        int size = gui.getInt(menuKey + ".size", 27);
        Inventory inventory = Bukkit.createInventory(new CasinoHolder(menuKey), size, title);
        ConfigurationSection items = gui.getConfigurationSection(menuKey + ".items");
        if (items == null) {
            return inventory;
        }
        for (String key : items.getKeys(false)) {
            ItemStack item = createItem(items.getConfigurationSection(key), player, session);
            int slot = items.getInt(key + ".slot", -1);
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, item);
            }
        }
        return inventory;
    }

    private ItemStack createItem(ConfigurationSection section, Player player, BetSession session) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", "");
            meta.setDisplayName(CC.color(applyPlaceholders(name, player, session)));
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(CC.color(applyPlaceholders(line, player, session)));
            }
            meta.setLore(lore);
            String action = section.getString("action");
            if (action != null) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyPlaceholders(String input, Player player, BetSession session) {
        if (input == null) {
            return "";
        }
        int chips = chipManager.getChips(player.getUniqueId());
        double rate = plugin.getConfig().getDouble("chipRateMoneyPer1", 10.0);
        int bet = session == null ? 0 : session.getBet();
        int min = plugin.getConfig().getInt("minBetChips");
        int max = plugin.getConfig().getInt("maxBetChips");
        return input
                .replace("{chips}", String.valueOf(chips))
                .replace("{rate}", String.format("%.2f", rate))
                .replace("{bet}", String.valueOf(bet))
                .replace("{min}", String.valueOf(min))
                .replace("{max}", String.valueOf(max));
    }

    private void refreshCurrentMenu(Player player, String menu) {
        switch (menu.toLowerCase()) {
            case "slots":
                openSlots(player);
                break;
            case "dice":
                openDice(player);
                break;
            case "coinflip":
                openCoinFlip(player);
                break;
            case "roulette":
                openRoulette(player);
                break;
            case "shop":
                openShop(player);
                break;
            default:
                openMain(player);
                break;
        }
    }

    private void rebuildActionSounds() {
        actionSounds.clear();
        for (String menu : new String[]{"main", "shop", "slots", "dice", "coinflip", "roulette"}) {
            ConfigurationSection items = gui.getConfigurationSection(menu + ".items");
            if (items == null) {
                continue;
            }
            for (String key : items.getKeys(false)) {
                String action = items.getString(key + ".action");
                String sound = items.getString(key + ".sound");
                if (action != null && sound != null) {
                    actionSounds.put(action, sound);
                }
            }
        }
    }

    private boolean isGameEnabled(String game) {
        return plugin.getConfig().getBoolean("enabledGames." + game, true);
    }
}
