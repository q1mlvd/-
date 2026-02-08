package com.famecube.casinofame.listeners;

import com.famecube.casinofame.games.GameManager;
import com.famecube.casinofame.play.GameSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final GameManager gameManager;
    private final boolean refundOnQuit;

    public PlayerListener(GameManager gameManager, boolean refundOnQuit) {
        this.gameManager = gameManager;
        this.refundOnQuit = refundOnQuit;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameSession session = gameManager.getSession(event.getPlayer());
        if (session != null && refundOnQuit && session.getBet() > 0 && session.isRunning()) {
            gameManager.refund(event.getPlayer(), session.getBet());
        }
        if (session != null) {
            gameManager.endSession(event.getPlayer());
        }
    }
}
