package com.famecube.casinofame.play;

import com.famecube.casinofame.games.GameType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GameSession {
    private final Player player;
    private final GameType type;
    private Inventory inventory;
    private long bet;
    private String selection;
    private boolean running;

    public GameSession(Player player, GameType type) {
        this.player = player;
        this.type = type;
    }

    public Player getPlayer() {
        return player;
    }

    public GameType getType() {
        return type;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public long getBet() {
        return bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
