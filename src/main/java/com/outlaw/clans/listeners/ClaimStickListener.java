package com.outlaw.clans.listeners;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;

public class ClaimStickListener implements Listener {

    private final OutlawClansPlugin plugin;
    private final NamespacedKey key;

    public ClaimStickListener(OutlawClansPlugin plugin) { this.plugin = plugin; this.key = new NamespacedKey(plugin, "claim-stick"); }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getItem() == null || e.getItem().getItemMeta() == null) return;
        ItemMeta meta = e.getItem().getItemMeta();
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return; }
        Clan clan = opt.get();
        if (!clan.isLeader(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Seul le leader peut définir le territoire."); return; }
        if (clan.hasTerritory()) { p.sendMessage(ChatColor.YELLOW + "Le territoire est déjà défini."); return; }

        Block clicked = e.getClickedBlock(); if (clicked == null) return;
        World w = clicked.getWorld();
        String allowed = plugin.getConfig().getString("territory.world","world");
        if (!w.getName().equalsIgnoreCase(allowed)) { p.sendMessage(ChatColor.RED + "Les claims sont limités au monde: " + allowed); return; }

        int r = plugin.getConfig().getInt("territory.radius", 75);
        Territory t = new Territory(w.getName(), r, clicked.getX(), clicked.getY(), clicked.getZ());

        int plot = plugin.getConfig().getInt("building.plot_size", 35);
        int half = plot / 2;
        int edge = plugin.getConfig().getInt("building.edge_margin", 12);
        edge = Math.max(edge, half + 2);
        java.util.List<Location> bases = new java.util.ArrayList<>();

        int zTop    = clicked.getZ() - r + edge + half;
        int zBottom = clicked.getZ() + r - edge - half;
        int xLeft   = clicked.getX() - r + edge + half;
        int xCenter = clicked.getX();
        int xRight  = clicked.getX() + r - edge - half;
        int by = clicked.getY();

        bases.add(new Location(w, xLeft  + 0.5, by, zTop    + 0.5));
        bases.add(new Location(w, xRight + 0.5, by, zTop    + 0.5));
        bases.add(new Location(w, xLeft  + 0.5, by, zBottom + 0.5));
        bases.add(new Location(w, xCenter+ 0.5, by, zBottom + 0.5));
        bases.add(new Location(w, xRight + 0.5, by, zBottom + 0.5));

        plugin.clans().setTerritory(clan, t, bases);
        p.sendMessage(ChatColor.GREEN + "Centre de territoire défini. Rayon: " + r + " (≈" + (r*2) + "x" + (r*2) + ").");

        if (plugin.getConfig().getBoolean("terraform.full_territory", true)) {
            int thickness  = plugin.getConfig().getInt("terraform.thickness", 10);
            org.bukkit.Material topMat = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.surface_top_material","GRASS_BLOCK").toUpperCase());
            org.bukkit.Material foundation = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.material","DIRT").toUpperCase());
            int clearAbove = plugin.getConfig().getInt("terraform.clear_above", 24);
            int r2 = t.getRadius();
            p.sendMessage(ChatColor.GRAY + "Terraforming du claim lancé (" + (r2*2) + "x" + (r2*2) + ").");
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.terraform().buildTerritoryPlate(t, t.getCenterY(), thickness, topMat, foundation);
                if (plugin.getConfig().getBoolean("feather.enabled", true)) {
                    int width = plugin.getConfig().getInt("feather.width", 20);
                    org.bukkit.Material ft = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.top_material","GRASS_BLOCK").toUpperCase());
                    org.bukkit.Material ff = org.bukkit.Material.valueOf(plugin.getConfig().getString("feather.foundation_material","DIRT").toUpperCase());
                    plugin.terraform().featherTerritoryEdges(t, t.getCenterY(), thickness, ft, ff, width);
                }
            }, 1L);
        }

        int delay = plugin.getConfig().getInt("terraform.spawn_npc_delay_ticks", 120);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < clan.getSpots().size(); i++) {
                var spot = clan.getSpots().get(i);
                org.bukkit.Location center = spot.getBaseLocation();
                int phalf = plugin.getConfig().getInt("building.plot_size", 35) / 2;

                int dx = t.getCenterX() - center.getBlockX();
                int dz = t.getCenterZ() - center.getBlockZ();
                int sx = Integer.signum(dx);
                int sz = Integer.signum(dz);
                if (sx == 0) sx = (sz == 0 ? 1 : sz);
                if (sz == 0) sz = (sx == 0 ? 1 : sx);

                int cornerX = center.getBlockX() + sx * (phalf - 1);
                int cornerZ = center.getBlockZ() + sz * (phalf - 1);
                int cornerY = center.getBlockY();

                org.bukkit.Location corner = new org.bukkit.Location(center.getWorld(), cornerX + 0.5, cornerY + 1.0, cornerZ + 0.5);
                var villager = plugin.npcs().spawnBuildingNpc(corner, i);
                spot.setNpcUuid(villager.getUniqueId());
            }
        }, delay);

        var stack = e.getItem(); stack.setAmount(stack.getAmount() - 1);
    }
}
