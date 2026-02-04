package com.famecube.casinofame.game;

import java.util.Random;

public class DiceGame {
    public GameResult play(Random random, int bet, String selection, double houseEdge) {
        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        int sum = die1 + die2;
        double multiplier = 0.0;
        if ("UNDER".equalsIgnoreCase(selection) && sum >= 2 && sum <= 6) {
            multiplier = 2.0;
        } else if ("OVER".equalsIgnoreCase(selection) && sum >= 8 && sum <= 12) {
            multiplier = 2.0;
        } else if ("EXACT".equalsIgnoreCase(selection) && sum == 7) {
            multiplier = 6.0;
        }
        int payout = applyHouseEdge(bet, multiplier, houseEdge);
        String outcome = "DICE:" + die1 + "+" + die2 + "=" + sum;
        return new GameResult(outcome, payout);
    }

    private int applyHouseEdge(int bet, double multiplier, double houseEdge) {
        double payout = bet * multiplier;
        payout = payout * (1.0 - houseEdge / 100.0);
        return (int) Math.floor(payout);
    }
}
