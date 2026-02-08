package com.famecube.casinofame.util;

import org.bukkit.ChatColor;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
