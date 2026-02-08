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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SlotsGame {
    private final FileConfiguration config;
    private final FairnessManager fairnessManager;
    private final ChipManager chipManager;
    private final SoundUtil soundUtil;
    private final boolean particles;
    private final double houseEdge;

    public SlotsGame(FileConfiguration config, FairnessManager fairnessManager, ChipManager chipManager, SoundUtil soundUtil, boolean particles) {
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
        List<SlotSymbol> symbols = loadSymbols();
        List<SlotSymbol> weighted = buildWeighted(symbols);
        Random random = new Random(seed);
        List<SlotSymbol> result = new ArrayList<>();
        int duration = config.getInt("animation.durationTicks", 40);
        int step = config.getInt("animation.stepTicks", 4);
        int steps = Math.max(1, duration / step);
        new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (current >= steps) {
                    for (int i = 0; i < 3; i++) {
                        result.add(weighted.get(random.nextInt(weighted.size())));
                    }
                    for (int i = 0; i < 3; i++) {
                        inv.setItem(12 + i, new ItemStack(result.get(i).material));
                    }
                    finish(session, bet, seed, result);
                    cancel();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    SlotSymbol symbol = weighted.get(random.nextInt(weighted.size()));
                    inv.setItem(12 + i, new ItemStack(symbol.material));
                }
                soundUtil.tick(player);
                current++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("CasinoFame"), 0L, step);
    }

    private void finish(GameSession session, long bet, long seed, List<SlotSymbol> result) {
        Player player = session.getPlayer();
        String outcome;
        long payout = 0L;
        boolean win = false;
        if (result.get(0).material == result.get(1).material && result.get(1).material == result.get(2).material) {
            payout = Math.round(bet * result.get(0).payout3);
            outcome = "3" + result.get(0).name;
            win = true;
        } else if (result.get(0).material == result.get(1).material || result.get(1).material == result.get(2).material || result.get(0).material == result.get(2).material) {
            SlotSymbol symbol = result.get(0).material == result.get(1).material ? result.get(0) : result.get(1).material == result.get(2).material ? result.get(1) : result.get(2);
            payout = Math.round(bet * symbol.payout2);
            outcome = "2" + symbol.name;
            win = true;
        } else {
            outcome = "0";
        }
        if (win) {
            payout = applyHouseEdge(payout);
            chipManager.addChips(player.getUniqueId(), payout);
        }
        UUID roundId = UUID.randomUUID();
        fairnessManager.recordRound(roundId, player.getName(), player.getUniqueId(), "SLOTS", bet, payout, seed, outcome);
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

    private List<SlotSymbol> loadSymbols() {
        List<SlotSymbol> symbols = new ArrayList<>();
        List<?> list = config.getList("slots.symbols");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof ConfigurationSection) {
                    symbols.add(fromSection((ConfigurationSection) obj));
                } else if (obj instanceof java.util.Map) {
                    ConfigurationSection section = config.createSection("temp", (java.util.Map<?, ?>) obj);
                    symbols.add(fromSection(section));
                    config.set("temp", null);
                }
            }
        }
        if (symbols.isEmpty()) {
            symbols.add(new SlotSymbol(Material.DIAMOND, "Алмаз", 1, 2.0, 1.5));
        }
        return symbols;
    }

    private List<SlotSymbol> buildWeighted(List<SlotSymbol> symbols) {
        List<SlotSymbol> weighted = new ArrayList<>();
        for (SlotSymbol symbol : symbols) {
            int weight = Math.max(1, symbol.weight);
            for (int i = 0; i < weight; i++) {
                weighted.add(symbol);
            }
        }
        return weighted;
    }

    private long applyHouseEdge(long payout) {
        double factor = Math.max(0.0, 1.0 - (houseEdge / 100.0));
        return Math.round(payout * factor);
    }

    private SlotSymbol fromSection(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("id", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        String name = section.getString("name", "");
        int weight = section.getInt("weight", 1);
        double payout3 = section.getDouble("payout3", 2.0);
        double payout2 = section.getDouble("payout2", 1.2);
        return new SlotSymbol(material, name, weight, payout3, payout2);
    }

    private static class SlotSymbol {
        private final Material material;
        private final String name;
        private final int weight;
        private final double payout3;
        private final double payout2;

        private SlotSymbol(Material material, String name, int weight, double payout3, double payout2) {
            this.material = material;
            this.name = name;
            this.weight = weight;
            this.payout3 = payout3;
            this.payout2 = payout2;
        }
    }
}
