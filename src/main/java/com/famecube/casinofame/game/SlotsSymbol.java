package com.famecube.casinofame.game;

public class SlotsSymbol {
    private final String id;
    private final int weight;
    private final double payout3;
    private final double payout2;

    public SlotsSymbol(String id, int weight, double payout3, double payout2) {
        this.id = id;
        this.weight = weight;
        this.payout3 = payout3;
        this.payout2 = payout2;
    }

    public String getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }

    public double getPayout3() {
        return payout3;
    }

    public double getPayout2() {
        return payout2;
    }
}
