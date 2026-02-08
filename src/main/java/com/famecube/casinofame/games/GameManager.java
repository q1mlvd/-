package com.famecube.casinofame.games;

import com.famecube.casinofame.chips.ChipManager;
import com.famecube.casinofame.fairness.FairnessManager;
import com.famecube.casinofame.play.GameSession;
import com.famecube.casinofame.util.MessageUtil;
import com.famecube.casinofame.util.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameManager {
    private final ChipManager chipManager;
    private final FairnessManager fairnessManager;
    private final MessageUtil messageUtil;
    private final FileConfiguration config;
    private final Map<UUID, GameSession> sessions = new HashMap<>();
    private final Map<UUID, Long> lastPlay = new HashMap<>();

    private final SlotsGame slotsGame;
    private final DiceGame diceGame;
    private final CoinFlipGame coinFlipGame;
    private final RouletteGame rouletteGame;

    public GameManager(ChipManager chipManager, FairnessManager fairnessManager, MessageUtil messageUtil, FileConfiguration config, SoundUtil soundUtil, boolean particles) {
        this.chipManager = chipManager;
        this.fairnessManager = fairnessManager;
        this.messageUtil = messageUtil;
        this.config = config;
        this.slotsGame = new SlotsGame(config, fairnessManager, chipManager, soundUtil, particles);
        this.diceGame = new DiceGame(config, fairnessManager, chipManager, soundUtil, particles);
        this.coinFlipGame = new CoinFlipGame(config, fairnessManager, chipManager, soundUtil, particles);
        this.rouletteGame = new RouletteGame(config, fairnessManager, chipManager, soundUtil, particles);
    }

    public GameSession createSession(Player player, GameType type) {
        GameSession session = new GameSession(player, type);
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public GameSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void endSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public boolean canPlay(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            messageUtil.send(player, "already-playing");
            return false;
        }
        return checkCooldown(player);
    }

    public boolean canStart(Player player) {
        return checkCooldown(player);
    }

    private boolean checkCooldown(Player player) {
        long cooldown = config.getLong("cooldownSeconds", 2L) * 1000L;
        long last = lastPlay.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < cooldown) {
            messageUtil.send(player, "cooldown");
            return false;
        }
        return true;
    }

    public void markPlayed(Player player) {
        lastPlay.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void startGame(GameSession session) {
        Player player = session.getPlayer();
        long bet = session.getBet();
        long minBet = config.getLong("minBetChips", 100L);
        long maxBet = config.getLong("maxBetChips", 10000L);
        if (bet < minBet) {
            messageUtil.send(player, "bet-too-low");
            return;
        }
        if (bet > maxBet) {
            messageUtil.send(player, "bet-too-high");
            return;
        }
        if (!chipManager.takeChips(player.getUniqueId(), bet)) {
            messageUtil.send(player, "not-enough-chips");
            return;
        }
        if (session.getType() != GameType.SLOTS && session.getSelection() == null) {
            messageUtil.send(player, "no-selection");
            chipManager.addChips(player.getUniqueId(), bet);
            return;
        }
        session.setRunning(true);
        markPlayed(player);
        long seed = new Random().nextLong();
        if (session.getType() == GameType.SLOTS) {
            slotsGame.start(session, bet, seed);
        } else if (session.getType() == GameType.DICE) {
            diceGame.start(session, bet, seed);
        } else if (session.getType() == GameType.COINFLIP) {
            coinFlipGame.start(session, bet, seed);
        } else if (session.getType() == GameType.ROULETTE) {
            rouletteGame.start(session, bet, seed);
        }
    }

    public void refund(Player player, long bet) {
        chipManager.addChips(player.getUniqueId(), bet);
        messageUtil.send(player, "refund");
    }
}
