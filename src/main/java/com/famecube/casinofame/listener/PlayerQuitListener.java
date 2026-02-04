package com.famecube.casinofame.listener;

import com.famecube.casinofame.manager.BetSession;
import com.famecube.casinofame.manager.ChipManager;
import com.famecube.casinofame.manager.GameManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final GameManager gameManager;
    private final ChipManager chipManager;
    private final FileConfiguration config;

    public PlayerQuitListener(GameManager gameManager, ChipManager chipManager, FileConfiguration config) {
        this.gameManager = gameManager;
        this.chipManager = chipManager;
        this.config = config;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BetSession session = gameManager.getActiveSession(player);
        if (session == null) {
            return;
        }
        if (session.isInProgress() && config.getBoolean("refundOnQuit", true)) {
            chipManager.addChips(player.getUniqueId(), session.getBet());
            session.setInProgress(false);
            session.setBet(0);
        }
    }
}
