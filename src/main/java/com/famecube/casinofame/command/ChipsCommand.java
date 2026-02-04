package com.famecube.casinofame.command;

import com.famecube.casinofame.manager.EconomyHook;
import com.famecube.casinofame.manager.GuiManager;
import com.famecube.casinofame.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChipsCommand implements CommandExecutor {
    private final GuiManager guiManager;
    private final EconomyHook economyHook;

    public ChipsCommand(GuiManager guiManager, EconomyHook economyHook) {
        this.guiManager = guiManager;
        this.economyHook = economyHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("casinofame.use")) {
            Msg.send(player, "no-permission");
            return true;
        }
        if (!economyHook.isEnabled()) {
            Msg.send(player, "shop-disabled");
            return true;
        }
        guiManager.openShop(player);
        return true;
    }
}
