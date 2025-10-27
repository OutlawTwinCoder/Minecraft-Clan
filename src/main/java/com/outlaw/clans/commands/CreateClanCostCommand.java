package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateClanCostCommand implements CommandExecutor {
    private final OutlawClansPlugin plugin;
    public CreateClanCostCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!p.hasPermission("outlawclans.admin") && !p.hasPermission("outlawclan.admin")) { p.sendMessage(ChatColor.RED+"Permission manquante."); return true; }
        if (args.length != 1) { p.sendMessage(ChatColor.YELLOW+"Usage: /createclancost <amount>"); return true; }
        try {
            int amount = Integer.parseInt(args[0]);
            plugin.getConfig().set("currency.create_cost", amount);
            plugin.saveConfig();
            p.sendMessage(ChatColor.GREEN+"Coût création clan: "+amount);
        } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED+"Montant invalide."); }
        return true;
    }
}
