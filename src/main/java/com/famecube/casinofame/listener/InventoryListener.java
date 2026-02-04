package com.famecube.casinofame.listener;

import com.famecube.casinofame.manager.CasinoHolder;
import com.famecube.casinofame.manager.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryListener implements Listener {
    private final GuiManager guiManager;
    private final Map<UUID, Long> debounce = new HashMap<>();

    public InventoryListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CasinoHolder)) {
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        long now = System.currentTimeMillis();
        long last = debounce.getOrDefault(player.getUniqueId(), 0L);
        long debounceMs = guiManager.getPlugin().getConfig().getLong("debounceMs", 300L);
        if (now - last < debounceMs) {
            return;
        }
        debounce.put(player.getUniqueId(), now);
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String action = meta.getPersistentDataContainer().get(guiManager.getActionKey(), PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        CasinoHolder holder = (CasinoHolder) top.getHolder();
        guiManager.handleAction(player, action, holder.getType());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CasinoHolder) {
            event.setCancelled(true);
        }
    }
}
