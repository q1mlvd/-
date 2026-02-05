package com.famecube.casinofame.listeners;

import com.famecube.casinofame.chips.ChipManager;
import com.famecube.casinofame.economy.EconomyManager;
import com.famecube.casinofame.games.GameManager;
import com.famecube.casinofame.games.GameType;
import com.famecube.casinofame.gui.BetGuiHelper;
import com.famecube.casinofame.gui.GuiManager;
import com.famecube.casinofame.play.GameSession;
import com.famecube.casinofame.util.MessageUtil;
import com.famecube.casinofame.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GuiManager guiManager;
    private final BetGuiHelper betGuiHelper;
    private final GameManager gameManager;
    private final ChipManager chipManager;
    private final EconomyManager economyManager;
    private final MessageUtil messageUtil;
    private final SoundUtil soundUtil;
    private final FileConfiguration config;
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private String mainTitle;
    private String shopTitle;

    public InventoryListener(GuiManager guiManager, BetGuiHelper betGuiHelper, GameManager gameManager, ChipManager chipManager,
                             EconomyManager economyManager, MessageUtil messageUtil, SoundUtil soundUtil, FileConfiguration config) {
        this.guiManager = guiManager;
        this.betGuiHelper = betGuiHelper;
        this.gameManager = gameManager;
        this.chipManager = chipManager;
        this.economyManager = economyManager;
        this.messageUtil = messageUtil;
        this.soundUtil = soundUtil;
        this.config = config;
        updateTitles();
    }

    public void updateTitles() {
        this.mainTitle = guiManager.getMainTitle();
        this.shopTitle = guiManager.getShopTitle();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (inv == null) {
            return;
        }
        String title = event.getView().getTitle();
        if (!isCasinoTitle(title)) {
            return;
        }
        event.setCancelled(true);
        if (!debounce(player.getUniqueId())) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        soundUtil.click(player);
        if (title.equals(mainTitle)) {
            handleMain(player, clicked);
        } else if (title.equals(shopTitle)) {
            handleShop(player, clicked);
        } else {
            handleGame(player, inv, clicked);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (isCasinoTitle(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        String title = player.getOpenInventory().getTitle();
        if (isCasinoTitle(title)) {
            event.setCancelled(true);
        }
    }

    private boolean isCasinoTitle(String title) {
        return title.equals(mainTitle)
                || title.equals(shopTitle)
                || title.equals(getGameTitle(GameType.SLOTS))
                || title.equals(getGameTitle(GameType.DICE))
                || title.equals(getGameTitle(GameType.COINFLIP))
                || title.equals(getGameTitle(GameType.ROULETTE));
    }

    private boolean debounce(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = lastClick.getOrDefault(uuid, 0L);
        if (now - last < 300L) {
            return false;
        }
        lastClick.put(uuid, now);
        return true;
    }

    private void handleMain(Player player, ItemStack clicked) {
        if (clicked.getType() == guiManager.getMainItem("slots").getMaterial()) {
            openGame(player, GameType.SLOTS);
        } else if (clicked.getType() == guiManager.getMainItem("dice").getMaterial()) {
            openGame(player, GameType.DICE);
        } else if (clicked.getType() == guiManager.getMainItem("coinflip").getMaterial()) {
            openGame(player, GameType.COINFLIP);
        } else if (clicked.getType() == guiManager.getMainItem("roulette").getMaterial()) {
            openGame(player, GameType.ROULETTE);
        } else if (clicked.getType() == guiManager.getMainItem("shop").getMaterial()) {
            openShop(player);
        } else if (clicked.getType() == guiManager.getMainItem("exit").getMaterial()) {
            player.closeInventory();
        }
    }

    private void handleShop(Player player, ItemStack clicked) {
        if (!economyManager.isAvailable()) {
            messageUtil.send(player, "vault-missing");
            return;
        }
        double rate = getRate();
        Map<Material, Integer> buyMap = new HashMap<>();
        buyMap.put(guiManager.getShopItem("buy100").getMaterial(), 100);
        buyMap.put(guiManager.getShopItem("buy500").getMaterial(), 500);
        buyMap.put(guiManager.getShopItem("buy1000").getMaterial(), 1000);
        buyMap.put(guiManager.getShopItem("buy5000").getMaterial(), 5000);
        buyMap.put(guiManager.getShopItem("buy10000").getMaterial(), 10000);
        Map<Material, Integer> sellMap = new HashMap<>();
        sellMap.put(guiManager.getShopItem("sell100").getMaterial(), 100);
        sellMap.put(guiManager.getShopItem("sell500").getMaterial(), 500);
        sellMap.put(guiManager.getShopItem("sell1000").getMaterial(), 1000);
        sellMap.put(guiManager.getShopItem("sell5000").getMaterial(), 5000);
        sellMap.put(guiManager.getShopItem("sell10000").getMaterial(), 10000);
        if (buyMap.containsKey(clicked.getType())) {
            int amount = buyMap.get(clicked.getType());
            double cost = amount * rate;
            if (economyManager.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
                chipManager.addChips(player.getUniqueId(), amount);
                player.sendMessage(messageUtil.get("shop-bought").replace("%amount%", String.valueOf(amount)));
            } else {
                messageUtil.send(player, "not-enough-money");
            }
            player.openInventory(guiManager.createShop(player, rate));
        } else if (sellMap.containsKey(clicked.getType())) {
            int amount = sellMap.get(clicked.getType());
            if (chipManager.takeChips(player.getUniqueId(), amount)) {
                economyManager.getEconomy().depositPlayer(player, amount * rate);
                player.sendMessage(messageUtil.get("shop-sold").replace("%amount%", String.valueOf(amount)));
            } else {
                messageUtil.send(player, "not-enough-chips");
            }
            player.openInventory(guiManager.createShop(player, rate));
        } else if (clicked.getType() == guiManager.getShopItem("back").getMaterial()) {
            player.openInventory(guiManager.createMain(player));
        }
    }

    private void handleGame(Player player, Inventory inv, ItemStack clicked) {
        GameSession session = gameManager.getSession(player);
        if (session == null) {
            return;
        }
        long bet = session.getBet();
        long chips = chipManager.getChips(player.getUniqueId());
        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
            if (name.contains("+100")) {
                bet += 100;
            } else if (name.contains("+500")) {
                bet += 500;
            } else if (name.contains("+1000")) {
                bet += 1000;
            } else if (name.contains("+5000")) {
                bet += 5000;
            }
        } else if (clicked.getType() == Material.GOLD_BLOCK) {
            bet = Math.min(chips, config.getLong("maxBetChips", 10000L));
        } else if (clicked.getType() == Material.BARRIER) {
            bet = 0;
        } else if (clicked.getType() == Material.EMERALD_BLOCK) {
            if (!gameManager.canStart(player)) {
                return;
            }
            if (session.isRunning()) {
                return;
            }
            gameManager.startGame(session);
            return;
        } else {
            handleSelection(session, clicked.getType());
        }
        session.setBet(bet);
        updateGameInventory(session);
    }

    private void handleSelection(GameSession session, Material material) {
        if (session.getType() == GameType.DICE) {
            if (material == Material.BLUE_WOOL) {
                session.setSelection("UNDER");
            } else if (material == Material.RED_WOOL) {
                session.setSelection("OVER");
            } else if (material == Material.WHITE_WOOL) {
                session.setSelection("EXACT");
            }
        } else if (session.getType() == GameType.COINFLIP) {
            if (material == Material.SUNFLOWER) {
                session.setSelection("HEADS");
            } else if (material == Material.FERN) {
                session.setSelection("TAILS");
            }
        } else if (session.getType() == GameType.ROULETTE) {
            if (material == Material.RED_WOOL) {
                session.setSelection("RED");
            } else if (material == Material.BLACK_WOOL) {
                session.setSelection("BLACK");
            } else if (material == Material.LIME_WOOL) {
                session.setSelection("GREEN");
            }
        }
    }

    private void updateGameInventory(GameSession session) {
        Inventory inv = session.getInventory();
        long bet = session.getBet();
        long chips = chipManager.getChips(session.getPlayer().getUniqueId());
        inv.setItem(22, betGuiHelper.getBetItem("betinfo", bet, chips));
    }

    private void openGame(Player player, GameType type) {
        if (!economyManager.isAvailable()) {
            messageUtil.send(player, "vault-missing");
            return;
        }
        if (!gameManager.canPlay(player)) {
            return;
        }
        GameSession session = gameManager.createSession(player, type);
        Inventory inv = Bukkit.createInventory(null, 45, getGameTitle(type));
        session.setInventory(inv);
        session.setBet(0);
        buildGameInventory(session);
        player.openInventory(inv);
    }

    private void buildGameInventory(GameSession session) {
        Inventory inv = session.getInventory();
        long chips = chipManager.getChips(session.getPlayer().getUniqueId());
        inv.setItem(10, betGuiHelper.getBetItem("add100", session.getBet(), chips));
        inv.setItem(11, betGuiHelper.getBetItem("add500", session.getBet(), chips));
        inv.setItem(12, betGuiHelper.getBetItem("add1000", session.getBet(), chips));
        inv.setItem(19, betGuiHelper.getBetItem("add5000", session.getBet(), chips));
        inv.setItem(20, betGuiHelper.getBetItem("max", session.getBet(), chips));
        inv.setItem(21, betGuiHelper.getBetItem("clear", session.getBet(), chips));
        inv.setItem(22, betGuiHelper.getBetItem("betinfo", session.getBet(), chips));
        inv.setItem(23, betGuiHelper.getBetItem("start", session.getBet(), chips));
        if (session.getType() == GameType.DICE) {
            inv.setItem(28, betGuiHelper.getChoiceItem("dice_under"));
            inv.setItem(31, betGuiHelper.getChoiceItem("dice_exact"));
            inv.setItem(34, betGuiHelper.getChoiceItem("dice_over"));
        } else if (session.getType() == GameType.COINFLIP) {
            inv.setItem(30, betGuiHelper.getChoiceItem("coin_heads"));
            inv.setItem(32, betGuiHelper.getChoiceItem("coin_tails"));
        } else if (session.getType() == GameType.ROULETTE) {
            inv.setItem(28, betGuiHelper.getChoiceItem("roulette_red"));
            inv.setItem(31, betGuiHelper.getChoiceItem("roulette_green"));
            inv.setItem(34, betGuiHelper.getChoiceItem("roulette_black"));
        }
    }

    private void openShop(Player player) {
        player.openInventory(guiManager.createShop(player, getRate()));
    }

    private String getGameTitle(GameType type) {
        if (type == GameType.SLOTS) {
            return "Слоты";
        }
        if (type == GameType.DICE) {
            return "Кости";
        }
        if (type == GameType.COINFLIP) {
            return "Монетка";
        }
        return "Рулетка";
    }

    private double getRate() {
        return config.getDouble("exchange.moneyPerChip", 1.0);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        GameSession session = gameManager.getSession(player);
        if (session != null && session.isRunning()) {
            return;
        }
        if (session != null) {
            gameManager.endSession(player);
        }
    }
}
