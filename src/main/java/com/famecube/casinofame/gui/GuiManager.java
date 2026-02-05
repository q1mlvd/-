package com.famecube.casinofame.gui;

import com.famecube.casinofame.chips.ChipManager;
import com.famecube.casinofame.util.ColorUtil;
import com.famecube.casinofame.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiManager {
    private FileConfiguration gui;
    private final ChipManager chipManager;
    private final Map<String, GuiItem> mainItems = new HashMap<>();
    private final Map<String, GuiItem> shopItems = new HashMap<>();

    public GuiManager(FileConfiguration gui, ChipManager chipManager) {
        this.gui = gui;
        this.chipManager = chipManager;
        loadItems();
    }

    public void reload(FileConfiguration newGui) {
        this.gui = newGui;
        mainItems.clear();
        shopItems.clear();
        loadItems();
    }

    private void loadItems() {
        ConfigurationSection mainSection = gui.getConfigurationSection("main.items");
        if (mainSection != null) {
            for (String key : mainSection.getKeys(false)) {
                mainItems.put(key, loadGuiItem(mainSection.getConfigurationSection(key)));
            }
        }
        ConfigurationSection shopSection = gui.getConfigurationSection("shop.items");
        if (shopSection != null) {
            for (String key : shopSection.getKeys(false)) {
                shopItems.put(key, loadGuiItem(shopSection.getConfigurationSection(key)));
            }
        }
    }

    private GuiItem loadGuiItem(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        String name = section.getString("name", "");
        List<String> lore = section.getStringList("lore");
        int slot = section.getInt("slot", 0);
        return new GuiItem(material == null ? Material.STONE : material, name, lore, slot);
    }

    public Inventory createMain(Player player) {
        int size = gui.getInt("main.size", 27);
        String title = getMainTitle();
        Inventory inv = Bukkit.createInventory(null, size, title);
        for (GuiItem item : mainItems.values()) {
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemBuilder(item.getMaterial()).name(item.getName()).lore(item.getLore()).build();
            inv.setItem(item.getSlot(), stack);
        }
        return inv;
    }

    public Inventory createShop(Player player, double rate) {
        int size = gui.getInt("shop.size", 45);
        String title = getShopTitle();
        Inventory inv = Bukkit.createInventory(null, size, title);
        long chips = chipManager.getChips(player.getUniqueId());
        for (Map.Entry<String, GuiItem> entry : shopItems.entrySet()) {
            GuiItem item = entry.getValue();
            if (item == null) {
                continue;
            }
            String name = item.getName();
            List<String> lore = item.getLore() == null ? null : new java.util.ArrayList<>(item.getLore());
            if ("balance".equalsIgnoreCase(entry.getKey())) {
                name = name.replace("%chips%", String.valueOf(chips));
                if (lore != null) {
                    for (int i = 0; i < lore.size(); i++) {
                        lore.set(i, lore.get(i).replace("%rate%", String.valueOf(rate)));
                    }
                }
            }
            ItemStack stack = new ItemBuilder(item.getMaterial()).name(name).lore(lore).build();
            inv.setItem(item.getSlot(), stack);
        }
        return inv;
    }

    public GuiItem getMainItem(String key) {
        return mainItems.get(key);
    }

    public GuiItem getShopItem(String key) {
        return shopItems.get(key);
    }

    public String getMainTitle() {
        return ColorUtil.color(gui.getString("main.title", ""));
    }

    public String getShopTitle() {
        return ColorUtil.color(gui.getString("shop.title", ""));
    }
}
