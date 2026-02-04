package com.famecube.casinofame.manager;

public class BetSession {
    private final String game;
    private int bet;
    private String selection;
    private boolean inProgress;

    public BetSession(String game) {
        this.game = game;
    }

    public String getGame() {
        return game;
    }

    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }
}
