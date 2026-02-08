package com.famecube.casinofame.games;

import com.famecube.casinofame.chips.ChipManager;
import com.famecube.casinofame.fairness.FairnessManager;
import com.famecube.casinofame.play.GameSession;
import com.famecube.casinofame.util.ActionBarUtil;
import com.famecube.casinofame.util.ColorUtil;
import com.famecube.casinofame.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.UUID;

public class RouletteGame {
    private final FileConfiguration config;
    private final FairnessManager fairnessManager;
    private final ChipManager chipManager;
    private final SoundUtil soundUtil;
    private final boolean particles;
    private final double houseEdge;

    public RouletteGame(FileConfiguration config, FairnessManager fairnessManager, ChipManager chipManager, SoundUtil soundUtil, boolean particles) {
        this.config = config;
        this.fairnessManager = fairnessManager;
        this.chipManager = chipManager;
        this.soundUtil = soundUtil;
        this.particles = particles;
        this.houseEdge = config.getDouble("houseEdgePercent", 0.0);
    }

    public void start(GameSession session, long bet, long seed) {
        Player player = session.getPlayer();
        Inventory inv = session.getInventory();
        Random random = new Random(seed);
        int duration = config.getInt("animation.durationTicks", 40);
        int step = config.getInt("animation.stepTicks", 4);
        int steps = Math.max(1, duration / step);
        new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (current >= steps) {
                    int roll = random.nextInt(15);
                    Material mat = getMaterial(roll);
                    inv.setItem(13, new ItemStack(mat));
                    finish(session, bet, seed, roll);
                    cancel();
                    return;
                }
                inv.setItem(13, new ItemStack(getMaterial(random.nextInt(15))));
                soundUtil.tick(player);
                current++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("CasinoFame"), 0L, step);
    }

    private void finish(GameSession session, long bet, long seed, int roll) {
        Player player = session.getPlayer();
        String choice = session.getSelection();
        long payout = 0L;
        boolean win = false;
        String outcome = roll == 0 ? "GREEN" : (roll <= 7 ? "RED" : "BLACK");
        if ("RED".equals(choice) && roll >= 1 && roll <= 7) {
            payout = Math.round(bet * config.getDouble("roulette.payoutRedBlack", 2.0));
            win = true;
        } else if ("BLACK".equals(choice) && roll >= 8 && roll <= 14) {
            payout = Math.round(bet * config.getDouble("roulette.payoutRedBlack", 2.0));
            win = true;
        } else if ("GREEN".equals(choice) && roll == 0) {
            payout = Math.round(bet * config.getDouble("roulette.payoutGreen", 14.0));
            win = true;
        }
        if (win) {
            payout = applyHouseEdge(payout);
            chipManager.addChips(player.getUniqueId(), payout);
        }
        UUID roundId = UUID.randomUUID();
        fairnessManager.recordRound(roundId, player.getName(), player.getUniqueId(), "ROULETTE", bet, payout, seed, outcome);
        if (win) {
            ActionBarUtil.send(player, ColorUtil.color("&aВыигрыш: " + payout));
            if (particles) {
                player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
            soundUtil.win(player);
            player.sendTitle(ColorUtil.color("&6Победа!"), ColorUtil.color("&a+" + payout), 5, 30, 10);
        } else {
            soundUtil.lose(player);
            ActionBarUtil.send(player, ColorUtil.color("&cПроигрыш: " + bet));
            player.sendTitle(ColorUtil.color("&cПоражение"), ColorUtil.color("&7-" + bet), 5, 30, 10);
        }
        session.setRunning(false);
    }

    private long applyHouseEdge(long payout) {
        double factor = Math.max(0.0, 1.0 - (houseEdge / 100.0));
        return Math.round(payout * factor);
    }

    private Material getMaterial(int roll) {
        if (roll == 0) {
            return Material.LIME_WOOL;
        }
        if (roll <= 7) {
            return Material.RED_WOOL;
        }
        return Material.BLACK_WOOL;
    }
}
