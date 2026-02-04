package com.famecube.casinofame.game;

public class GameResult {
    private final String outcome;
    private final int payout;

    public GameResult(String outcome, int payout) {
        this.outcome = outcome;
        this.payout = payout;
    }

    public String getOutcome() {
        return outcome;
    }

    public int getPayout() {
        return payout;
    }
}
