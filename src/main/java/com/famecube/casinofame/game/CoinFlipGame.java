package com.famecube.casinofame.game;

import java.util.Random;

public class CoinFlipGame {
    public GameResult play(Random random, int bet, String selection, double houseEdge) {
        boolean heads = random.nextBoolean();
        String outcome = heads ? "HEADS" : "TAILS";
        double multiplier = outcome.equalsIgnoreCase(selection) ? 2.0 : 0.0;
        int payout = applyHouseEdge(bet, multiplier, houseEdge);
        return new GameResult("COIN:" + outcome, payout);
    }

    private int applyHouseEdge(int bet, double multiplier, double houseEdge) {
        double payout = bet * multiplier;
        payout = payout * (1.0 - houseEdge / 100.0);
        return (int) Math.floor(payout);
    }
}
