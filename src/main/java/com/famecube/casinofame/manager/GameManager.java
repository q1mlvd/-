package com.famecube.casinofame.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private final Map<UUID, BetSession> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BetSession getSession(Player player, String game) {
        BetSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.getGame().equalsIgnoreCase(game)) {
            session = new BetSession(game);
            sessions.put(player.getUniqueId(), session);
        }
        return session;
    }

    public void clearSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public BetSession getActiveSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void setCooldown(Player player, long millis) {
        cooldowns.put(player.getUniqueId(), millis);
    }

    public long getCooldown(Player player) {
        return cooldowns.getOrDefault(player.getUniqueId(), 0L);
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}
