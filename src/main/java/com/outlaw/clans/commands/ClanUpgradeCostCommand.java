package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanUpgradeCostCommand implements CommandExecutor {
    private final OutlawClansPlugin plugin;

    public ClanUpgradeCostCommand(OutlawClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("outlawclans.admin") && !player.hasPermission("outlawclan.admin")) {
            player.sendMessage(ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /clanupgrade cost <montant>");
            return true;
        }
        try {
            int amount = Integer.parseInt(args[0]);
            plugin.getConfig().set("territory.upgrade_cost", Math.max(0, amount));
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Coût d'upgrade du territoire: " + amount);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Montant invalide.");
        }
        return true;
    }
}
