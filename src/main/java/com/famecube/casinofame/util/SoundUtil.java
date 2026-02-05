package com.famecube.casinofame.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {
    private final boolean enabled;

    public SoundUtil(boolean enabled) {
        this.enabled = enabled;
    }

    public void click(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    public void tick(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.5f);
    }

    public void win(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public void lose(Player player) {
        play(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private void play(Player player, Sound sound, float volume, float pitch) {
        if (!enabled || player == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
