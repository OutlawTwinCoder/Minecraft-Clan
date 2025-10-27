package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateClanCommand implements CommandExecutor {

    private final OutlawClansPlugin plugin;
    public CreateClanCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!p.hasPermission("outlawclans.create")) { p.sendMessage(ChatColor.RED + "Permission manquante: outlawclans.create"); return true; }
        if (args.length < 1) { p.sendMessage(ChatColor.YELLOW + "Usage: /create clan <name> | /create clan npc"); return true; }

        if (args[0].equalsIgnoreCase("clan")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("npc")) {
                Location loc = p.getLocation();
                plugin.npcs().spawnTerritoryMerchant(loc.getBlock().getLocation());
                p.sendMessage(ChatColor.GREEN + "NPC Marchand de Territoire créé à votre position.");
                return true;
            }
            if (args.length < 2) { p.sendMessage(ChatColor.YELLOW + "Usage: /create clan <name>"); return true; }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args,1,args.length));
            var existing = plugin.clans().getClanByPlayer(p.getUniqueId());
            if (existing.isPresent()) { p.sendMessage(ChatColor.RED + "Vous êtes déjà dans un clan."); return true; }
            if (!plugin.economy().chargeCreate(p)) { p.sendMessage(ChatColor.RED + "Fonds insuffisants."); return true; }
            Clan c = plugin.clans().createClan(name, p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "Clan créé: " + ChatColor.AQUA + c.getName() + ChatColor.GRAY + " (-" + plugin.economy().formatAmount(plugin.economy().costCreateClan()) + ")");
            return true;
        }
        p.sendMessage(ChatColor.YELLOW + "Usage: /create clan <name> | /create clan npc");
        return true;
    }
}
