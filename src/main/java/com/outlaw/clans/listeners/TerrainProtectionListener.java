package com.outlaw.clans.listeners;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class TerrainProtectionListener implements Listener {

    private final OutlawClansPlugin plugin;

    public TerrainProtectionListener(OutlawClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleTerrainProtection(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleTerrainProtection(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    private void handleTerrainProtection(Player player, Location location, Cancellable event) {
        if (player == null || location == null) {
            return;
        }
        Clan clan = findOwningClan(location);
        if (clan == null) {
            return;
        }
        event.setCancelled(true);
        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ce terrain appartient à " + clan.getName() + ".");
            return;
        }
        player.sendMessage(ChatColor.RED + "Tu ne peux pas modifier ce terrain protégé du clan.");
    }

    private Clan findOwningClan(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        int plotSize = plugin.getConfig().getInt("building.plot_size", 35);
        int half = Math.max(1, plotSize / 2);
        for (Clan clan : plugin.clans().allClans()) {
            for (BuildingSpot spot : clan.getSpots()) {
                Location base = spot.getBaseLocation();
                if (base == null || base.getWorld() == null) {
                    continue;
                }
                if (!base.getWorld().equals(location.getWorld())) {
                    continue;
                }
                int minX = base.getBlockX() - half;
                int maxX = base.getBlockX() + half;
                int minZ = base.getBlockZ() - half;
                int maxZ = base.getBlockZ() + half;
                if (location.getBlockX() >= minX && location.getBlockX() <= maxX
                        && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ) {
                    return clan;
                }
            }
        }
        return null;
    }
}
