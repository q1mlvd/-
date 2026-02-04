package com.famecube.casinofame.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class Msg {
    private static FileConfiguration messages;

    private Msg() {
    }

    public static void init(FileConfiguration config) {
        messages = config;
    }

    public static String get(String key) {
        return format(messages.getString(key, ""));
    }

    public static String format(String message) {
        return CC.color(message);
    }

    public static String withPrefix(String message) {
        return get("prefix") + format(message);
    }

    public static String replace(String message, Map<String, String> placeholders) {
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static void send(CommandSender sender, String key) {
        sender.sendMessage(withPrefix(get(key)));
    }

    public static void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = replace(get(key), placeholders);
        sender.sendMessage(withPrefix(message));
    }

    public static Map<String, String> map(String k1, String v1) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static Map<String, String> map(String k1, String v1, String k2, String v2) {
        Map<String, String> map = map(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, String> map(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> map = map(k1, v1, k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static Map<String, String> map(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
        Map<String, String> map = map(k1, v1, k2, v2, k3, v3);
        map.put(k4, v4);
        return map;
    }

    public static Map<String, String> map(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5, String k6, String v6) {
        Map<String, String> map = map(k1, v1, k2, v2, k3, v3, k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }
}
