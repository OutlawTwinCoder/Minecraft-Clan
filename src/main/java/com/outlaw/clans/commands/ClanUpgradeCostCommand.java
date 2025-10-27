package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanUpgradeCostCommand implements CommandExecutor {
    private final OutlawClansPlugin plugin;
    public ClanUpgradeCostCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!p.hasPermission("outlawclans.admin") && !p.hasPermission("outlawclan.admin")) {
            p.sendMessage(ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length == 0 || args.length > 2) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
            return true;
        }

        String amountArg;
        if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("cost")) {
                p.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
                return true;
            }
            amountArg = args[1];
        } else {
            amountArg = args[0];
        }

        try {
            int amount = Integer.parseInt(amountArg);
            if (plugin.economy().mode() == com.outlaw.clans.service.EconomyService.Mode.ITEM) {
                plugin.getConfig().set("economy.item.upgrade_cost", amount);
            } else {
                plugin.getConfig().set("economy.xp.upgrade_cost", amount);
            }
            plugin.saveConfig();
            p.sendMessage(ChatColor.GREEN + "Coût d'amélioration du territoire: " + plugin.economy().formatAmount(amount));
        } catch (NumberFormatException ex) {
            p.sendMessage(ChatColor.RED + "Montant invalide.");
        }
        return true;
    }
}
