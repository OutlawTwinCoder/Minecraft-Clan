package com.outlaw.clans.commands;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanMenuCommand implements CommandExecutor {

    private final OutlawClansPlugin plugin;
    public ClanMenuCommand(OutlawClansPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }

        if (args.length >= 1 && args[0].equalsIgnoreCase("currency")) {
            if (!p.hasPermission("outlawclans.admin") && !p.hasPermission("outlawclan.admin")) { p.sendMessage(ChatColor.RED + "Permission manquante."); return true; }
            if (args.length == 2 && args[1].equalsIgnoreCase("money")) {
                plugin.getConfig().set("economy.mode","MONEY"); plugin.saveConfig();
                p.sendMessage(ChatColor.GREEN + "Monnaie: MONEY"); return true;
            }
            if (args.length >= 2) {
                try {
                    org.bukkit.Material m = org.bukkit.Material.valueOf(args[1].toUpperCase());
                    plugin.getConfig().set("economy.mode","ITEM");
                    plugin.getConfig().set("economy.item_type", m.name());
                    plugin.saveConfig();
                    p.sendMessage(ChatColor.GREEN + "Monnaie: ITEM ("+m.name()+")");
                } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Item inconnu: " + args[1]); }
                return true;
            }
            p.sendMessage(ChatColor.YELLOW + "Usage: /clan currency money | /clan currency <ITEM_NAME>");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("money")) {
            if (!p.hasPermission("outlawclans.admin") && !p.hasPermission("outlawclan.admin")) { p.sendMessage(ChatColor.RED + "Permission manquante."); return true; }
            if (args.length == 2 && args[1].equalsIgnoreCase("get")) {
                p.sendMessage(ChatColor.AQUA + "Votre solde: " + plugin.economy().getBalance(p.getUniqueId())); return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("set")) {
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
                double amount = Double.parseDouble(args[3]);
                plugin.economy().setBalance(t.getUniqueId(), amount);
                p.sendMessage(ChatColor.GREEN + "Solde défini pour " + args[2] + ": " + amount); return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("give")) {
                org.bukkit.OfflinePlayer t = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
                double amount = Double.parseDouble(args[3]);
                plugin.economy().give(t.getUniqueId(), amount);
                p.sendMessage(ChatColor.GREEN + "Ajouté " + amount + " à " + args[2]); return true;
            }
            p.sendMessage(ChatColor.YELLOW + "Usage: /clan money get | /clan money set <player> <amount> | /clan money give <player> <amount>");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("show")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("territory")) {
                var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
                if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
                plugin.npcs().showTerritory(p, opt.get());
                return true;
            }
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("terraform")) {
            if (!(p.hasPermission("outlawclans.admin") || p.hasPermission("outlawclan.admin"))) {
                p.sendMessage(ChatColor.RED + "Permission manquante."); return true;
            }
            var opt2 = plugin.clans().getClanByPlayer(p.getUniqueId());
            if (opt2.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
            var clan2 = opt2.get();
            if (!clan2.hasTerritory()) { p.sendMessage(ChatColor.RED + "Aucun territoire."); return true; }
            var t = clan2.getTerritory();
            int centerY = t.getCenterY();
            int thickness = plugin.getConfig().getInt("terraform.thickness", 10);
            org.bukkit.Material topMat = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.surface_top_material","GRASS_BLOCK").toUpperCase());
            org.bukkit.Material foundation = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.material","DIRT").toUpperCase());
            int clearAbove = plugin.getConfig().getInt("terraform.clear_above", 24);
            int r = t.getRadius();
            plugin.terraform().buildTerritoryPlate(t, centerY, thickness, topMat, foundation);
            if (plugin.getConfig().getBoolean("feather.enabled", true)) {
                int width = plugin.getConfig().getInt("feather.width", 20);
                org.bukkit.Material ft = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.top_material","GRASS_BLOCK").toUpperCase());
                org.bukkit.Material ff = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.foundation_material","DIRT").toUpperCase());
                plugin.terraform().featherTerritoryEdges(t, centerY, thickness, ft, ff, width);
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("menu")) {
            var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
            if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
            Clan clan = opt.get();
            plugin.menuUI().openFor(p, clan);
            return true;
        }
        p.sendMessage(ChatColor.YELLOW + "Usage: /clan menu | /clan show territory | /clan currency | /clan money ... | /clan terraform");
        return true;
    }
}
