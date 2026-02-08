package com.famecube.casinofame.gui;

import com.famecube.casinofame.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BetGuiHelper {
    private final FileConfiguration gui;

    public BetGuiHelper(FileConfiguration gui) {
        this.gui = gui;
    }

    public ItemStack getBetItem(String key, long bet, long chips) {
        ConfigurationSection section = gui.getConfigurationSection("bet." + key);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        String name = section.getString("name", "").replace("%bet%", String.valueOf(bet));
        List<String> lore = new java.util.ArrayList<>(section.getStringList("lore"));
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace("%bet%", String.valueOf(bet)).replace("%chips%", String.valueOf(chips)));
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    public ItemStack getChoiceItem(String key) {
        ConfigurationSection section = gui.getConfigurationSection("choices." + key);
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        String name = section.getString("name", "");
        List<String> lore = new java.util.ArrayList<>(section.getStringList("lore"));
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        return new ItemBuilder(material).name(name).lore(lore).build();
    }
}
