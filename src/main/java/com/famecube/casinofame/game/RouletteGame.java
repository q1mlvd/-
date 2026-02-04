package com.famecube.casinofame.game;

import java.util.Random;

public class RouletteGame {
    public GameResult play(Random random, int bet, String selection, double houseEdge) {
        int sector = random.nextInt(15);
        String outcome;
        double multiplier = 0.0;
        if (sector == 0) {
            outcome = "GREEN";
            multiplier = "GREEN".equalsIgnoreCase(selection) ? 14.0 : 0.0;
        } else if (sector <= 7) {
            outcome = "RED";
            multiplier = "RED".equalsIgnoreCase(selection) ? 2.0 : 0.0;
        } else {
            outcome = "BLACK";
            multiplier = "BLACK".equalsIgnoreCase(selection) ? 2.0 : 0.0;
        }
        int payout = applyHouseEdge(bet, multiplier, houseEdge);
        return new GameResult("ROULETTE:" + outcome, payout);
    }

    private int applyHouseEdge(int bet, double multiplier, double houseEdge) {
        double payout = bet * multiplier;
        payout = payout * (1.0 - houseEdge / 100.0);
        return (int) Math.floor(payout);
    }
}
