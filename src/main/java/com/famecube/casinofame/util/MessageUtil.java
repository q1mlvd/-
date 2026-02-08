package com.famecube.casinofame.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageUtil {
    private final FileConfiguration messages;

    public MessageUtil(FileConfiguration messages) {
        this.messages = messages;
    }

    public String get(String key) {
        String prefix = ColorUtil.color(messages.getString("prefix", ""));
        String msg = ColorUtil.color(messages.getString(key, ""));
        return prefix + msg;
    }

    public String raw(String key) {
        return ColorUtil.color(messages.getString(key, ""));
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }
}
