package com.famecube.casinofame.manager;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CasinoHolder implements InventoryHolder {
    private final String type;

    public CasinoHolder(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
