package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveClanCommand implements CommandExecutor {

    private final OutlawClansPlugin plugin;
    public LeaveClanCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
        Clan clan = opt.get();

        if (clan.isLeader(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Vous êtes leader. Utilisez /deleteclan confirm."); return true; }
        if (args.length != 1 || !args[0].equalsIgnoreCase("confirm")) { p.sendMessage(ChatColor.RED + "Confirmez avec: /leaveclan confirm"); return true; }

        plugin.clans().removeMember(clan, p.getUniqueId());
        p.sendMessage(ChatColor.GREEN + "Vous avez quitté le clan " + ChatColor.AQUA + clan.getName());
        return true;
    }
}
