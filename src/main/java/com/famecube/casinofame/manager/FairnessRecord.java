package com.famecube.casinofame.manager;

public class FairnessRecord {
    private final long timestamp;
    private final String roundId;
    private final long seed;
    private final String game;
    private final int bet;
    private final String outcome;
    private final int payout;
    private final String playerUuid;
    private final String playerName;

    public FairnessRecord(long timestamp, String roundId, long seed, String game, int bet, String outcome, int payout, String playerUuid, String playerName) {
        this.timestamp = timestamp;
        this.roundId = roundId;
        this.seed = seed;
        this.game = game;
        this.bet = bet;
        this.outcome = outcome;
        this.payout = payout;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getRoundId() {
        return roundId;
    }

    public long getSeed() {
        return seed;
    }

    public String getGame() {
        return game;
    }

    public int getBet() {
        return bet;
    }

    public String getOutcome() {
        return outcome;
    }

    public int getPayout() {
        return payout;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }
}
