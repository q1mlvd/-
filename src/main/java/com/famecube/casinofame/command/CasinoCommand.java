package com.famecube.casinofame.command;

import com.famecube.casinofame.manager.ChipManager;
import com.famecube.casinofame.manager.FairnessRecord;
import com.famecube.casinofame.manager.FairnessRepository;
import com.famecube.casinofame.manager.GuiManager;
import com.famecube.casinofame.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CasinoCommand implements CommandExecutor {
    private final GuiManager guiManager;
    private final ChipManager chipManager;
    private final FairnessRepository fairnessRepository;

    public CasinoCommand(GuiManager guiManager, ChipManager chipManager, FairnessRepository fairnessRepository) {
        this.guiManager = guiManager;
        this.chipManager = chipManager;
        this.fairnessRepository = fairnessRepository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("casinofame.use")) {
                Msg.send(player, "no-permission");
                return true;
            }
            guiManager.openMain(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("casinofame.reload")) {
                Msg.send(sender, "no-permission");
                return true;
            }
            guiManager.getPlugin().reloadConfig();
            guiManager.reload();
            chipManager.reload();
            fairnessRepository.reload();
            Msg.send(sender, "reloaded");
            return true;
        }
        if (args[0].equalsIgnoreCase("fairness")) {
            if (!sender.hasPermission("casinofame.fairness")) {
                Msg.send(sender, "no-permission");
                return true;
            }
            if (args.length < 2) {
                Msg.send(sender, "usage-casino");
                return true;
            }
            FairnessRecord record = fairnessRepository.get(args[1]);
            if (record == null) {
                Msg.send(sender, "fairness-not-found");
                return true;
            }
            Msg.send(sender, "fairness-info", Msg.map(
                    "round", record.getRoundId(),
                    "game", record.getGame(),
                    "bet", String.valueOf(record.getBet()),
                    "payout", String.valueOf(record.getPayout()),
                    "seed", String.valueOf(record.getSeed()),
                    "outcome", record.getOutcome()
            ));
            return true;
        }
        if (args[0].equalsIgnoreCase("chips")) {
            if (!sender.hasPermission("casinofame.chips.admin")) {
                Msg.send(sender, "no-permission");
                return true;
            }
            if (args.length < 4) {
                Msg.send(sender, "usage-casino");
                return true;
            }
            String action = args[1].toLowerCase();
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                Msg.send(sender, "player-not-found");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                Msg.send(sender, "chips-invalid-amount");
                return true;
            }
            if (amount <= 0) {
                Msg.send(sender, "chips-invalid-amount");
                return true;
            }
            UUID uuid = target.getUniqueId();
            if (action.equals("give")) {
                chipManager.addChips(uuid, amount);
                Msg.send(sender, "chips-given", Msg.map("amount", String.valueOf(amount), "player", target.getName()));
            } else if (action.equals("take")) {
                chipManager.removeChips(uuid, amount);
                Msg.send(sender, "chips-taken", Msg.map("amount", String.valueOf(amount), "player", target.getName()));
            } else {
                Msg.send(sender, "usage-casino");
            }
            return true;
        }
        Msg.send(sender, "usage-casino");
        return true;
    }
}
