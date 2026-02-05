package com.famecube.casinofame.gui;

import org.bukkit.Material;

import java.util.List;

public class GuiItem {
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int slot;

    public GuiItem(Material material, String name, List<String> lore, int slot) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.slot = slot;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getSlot() {
        return slot;
    }
}
