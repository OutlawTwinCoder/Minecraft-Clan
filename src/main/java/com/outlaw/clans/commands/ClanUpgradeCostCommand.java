package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ClanUpgradeCostCommand implements CommandExecutor {
    private final OutlawClansPlugin plugin;
    public ClanUpgradeCostCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("outlawclans.admin") && !player.hasPermission("outlawclan.admin")) {
                player.sendMessage(ChatColor.RED + "Permission manquante.");
                return true;
            }
        } else if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "Commande réservée aux administrateurs.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
            return true;
        }

        int index = 0;
        if (args.length >= 2 && args[0].equalsIgnoreCase("cost")) {
            index = 1;
        } else if (args[0].equalsIgnoreCase("cost")) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
            return true;
        }

        if (index >= args.length) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
            return true;
        }

        try {
            int amount = Integer.parseInt(args[index]);
            if (plugin.economy().mode() == com.outlaw.clans.service.EconomyService.Mode.ITEM) {
                plugin.getConfig().set("economy.item.upgrade_cost", amount);
            } else {
                plugin.getConfig().set("economy.xp.upgrade_cost", amount);
            }
            plugin.saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Coût d'amélioration du territoire: " + plugin.economy().formatAmount(amount));
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Montant invalide.");
        }
        return true;
    }
}
