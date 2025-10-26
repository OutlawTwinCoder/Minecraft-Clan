package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShowTerritoryCommand implements CommandExecutor {

    private final OutlawClansPlugin plugin;
    public ShowTerritoryCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (args.length >= 2 && args[0].equalsIgnoreCase("clan") && args[1].equalsIgnoreCase("territoire")) {
            var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
            if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
            plugin.npcs().showTerritory(p, opt.get());
            return true;
        }
        p.sendMessage(ChatColor.YELLOW + "Usage: /show clan territoire");
        return true;
    }
}
