package com.famecube.casinofame.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ActionBarUtil {
    private ActionBarUtil() {
    }

    public static void send(Player player, String message) {
        if (player == null) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtil.color(message)));
    }
}
