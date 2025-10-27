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
            if (!p.hasPermission("outlawclans.admin") && !p.hasPermission("outlawclan.admin")) {
                p.sendMessage(ChatColor.RED + "Permission manquante.");
                return true;
            }
            if (args.length < 2) {
                p.sendMessage(ChatColor.YELLOW + "Usage: /clan currency exp | /clan currency item <ITEM_NAME>");
                return true;
            }
            String choice = args[1];
            if (choice.equalsIgnoreCase("item")) {
                if (args.length < 3) {
                    p.sendMessage(ChatColor.YELLOW + "Usage: /clan currency item <ITEM_NAME>");
                    return true;
                }
                choice = args[2];
            }
            if (choice.equalsIgnoreCase("exp") || choice.equalsIgnoreCase("xp")) {
                plugin.getConfig().set("currency.mode", "EXP");
                plugin.saveConfig();
                p.sendMessage(ChatColor.GREEN + "Le clan utilise désormais l'XP comme monnaie.");
                return true;
            }
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(choice);
            if (material == null) {
                p.sendMessage(ChatColor.RED + "Item inconnu: " + choice);
                return true;
            }
            plugin.getConfig().set("currency.mode", "ITEM");
            plugin.getConfig().set("currency.item_type", material.name());
            plugin.saveConfig();
            p.sendMessage(ChatColor.GREEN + "Le clan utilise désormais l'item " + material.name() + ".");
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

        if (args.length >= 1 && args[0].equalsIgnoreCase("upgrade")) {
            var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
            if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return true; }
            Clan clan = opt.get();
            if (!clan.isLeader(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Seul le leader peut améliorer le territoire."); return true; }
            if (!clan.hasTerritory()) { p.sendMessage(ChatColor.RED + "Votre clan n'a pas encore de territoire."); return true; }
            int cost = plugin.currency().costUpgrade();
            if (cost > 0 && !plugin.currency().withdrawClanBalance(clan.getId(), cost)) {
                p.sendMessage(ChatColor.RED + "La banque du clan n'a pas assez de fonds (" + plugin.currency().formatAmount(cost) + ChatColor.RED + ").");
                return true;
            }
            int increase = Math.max(1, plugin.getConfig().getInt("territory.upgrade_increase", 50));
            var updated = plugin.clans().expandTerritory(clan, increase);
            if (updated == null) {
                p.sendMessage(ChatColor.RED + "Impossible d'agrandir le territoire.");
                return true;
            }
            if (plugin.getConfig().getBoolean("feather.enabled", true)) {
                int thickness = plugin.getConfig().getInt("terraform.thickness", 10);
                org.bukkit.Material topMat = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.top_material", "GRASS_BLOCK").toUpperCase());
                org.bukkit.Material foundation = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.foundation_material", "DIRT").toUpperCase());
                int width = plugin.getConfig().getInt("feather.width", 20);
                plugin.terraform().featherTerritoryEdges(updated, updated.getCenterY(), thickness, topMat, foundation, width);
            }
            String costInfo = cost > 0 ? ChatColor.GRAY + " (" + plugin.currency().formatAmount(cost) + ChatColor.GRAY + ")" : "";
            p.sendMessage(ChatColor.GREEN + "Territoire agrandi. Nouveau rayon: " + updated.getRadius() + "." + costInfo);
            return true;
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
        p.sendMessage(ChatColor.YELLOW + "Usage: /clan menu | /clan show territory | /clan currency <exp|item> | /clan upgrade | /clan terraform");
        return true;
    }
}
