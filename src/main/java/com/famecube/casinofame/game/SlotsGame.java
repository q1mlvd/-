package com.famecube.casinofame.game;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SlotsGame {
    private final List<SlotsSymbol> symbols = new ArrayList<>();
    private int totalWeight;

    public SlotsGame(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        symbols.clear();
        totalWeight = 0;
        List<Map<?, ?>> list = config.getMapList("slots.symbols");
        for (Map<?, ?> entry : list) {
            Object idObj = entry.get("id");
            Object weightObj = entry.get("weight");
            Object payout3Obj = entry.get("payout3");
            Object payout2Obj = entry.get("payout2");
            String id = idObj == null ? "SYMBOL" : idObj.toString();
            int weight = weightObj instanceof Number ? ((Number) weightObj).intValue() : 1;
            double payout3 = payout3Obj instanceof Number ? ((Number) payout3Obj).doubleValue() : 0.0;
            double payout2 = payout2Obj instanceof Number ? ((Number) payout2Obj).doubleValue() : 0.0;
            symbols.add(new SlotsSymbol(id, weight, payout3, payout2));
            totalWeight += weight;
        }
    }

    public GameResult play(Random random, int bet, double houseEdge) {
        SlotsSymbol first = roll(random);
        SlotsSymbol second = roll(random);
        SlotsSymbol third = roll(random);
        double multiplier = 0.0;
        if (first.getId().equals(second.getId()) && second.getId().equals(third.getId())) {
            multiplier = first.getPayout3();
        } else if (first.getId().equals(second.getId()) || first.getId().equals(third.getId())) {
            multiplier = first.getPayout2();
        } else if (second.getId().equals(third.getId())) {
            multiplier = second.getPayout2();
        }
        int payout = applyHouseEdge(bet, multiplier, houseEdge);
        String outcome = first.getId() + "|" + second.getId() + "|" + third.getId();
        return new GameResult(outcome, payout);
    }

    private SlotsSymbol roll(Random random) {
        if (symbols.isEmpty()) {
            return new SlotsSymbol("EMPTY", 1, 0.0, 0.0);
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        int current = 0;
        for (SlotsSymbol symbol : symbols) {
            current += symbol.getWeight();
            if (roll < current) {
                return symbol;
            }
        }
        return symbols.get(0);
    }

    private int applyHouseEdge(int bet, double multiplier, double houseEdge) {
        double payout = bet * multiplier;
        payout = payout * (1.0 - houseEdge / 100.0);
        return (int) Math.floor(payout);
    }
}
