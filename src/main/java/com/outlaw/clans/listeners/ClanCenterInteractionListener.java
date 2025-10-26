package com.outlaw.clans.listeners;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.ui.ClanMenuUI;
import com.outlaw.clans.util.Keys;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class ClanCenterInteractionListener implements Listener {

    private final OutlawClansPlugin plugin;
    private final NamespacedKey plotSignKey;
    private final ClanMenuUI menuUI;
    private final int centerRadius;

    public ClanCenterInteractionListener(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.plotSignKey = new NamespacedKey(plugin, Keys.PLOT_SIGN);
        this.menuUI = plugin.menuUI();
        this.centerRadius = Math.max(1, plugin.getConfig().getInt("building.center_schematic.lectern_radius", 4));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getState() instanceof Sign sign) {
            handlePlotSign(event, sign);
            return;
        }

        if (block.getType() == Material.LECTERN) {
            handleLectern(event, block);
        }
    }

    private void handlePlotSign(PlayerInteractEvent event, Sign sign) {
        Integer index = sign.getPersistentDataContainer().get(plotSignKey, PersistentDataType.INTEGER);
        if (index == null) return;

        Player player = event.getPlayer();
        Optional<Clan> optClan = plugin.clans().getClanByLocation(sign.getLocation());
        if (optClan.isEmpty()) return;

        Clan clan = optClan.get();
        Optional<Clan> playerClan = plugin.clans().getClanByPlayer(player.getUniqueId());
        if (playerClan.isEmpty() || !playerClan.get().getId().equals(clan.getId())) {
            player.sendMessage(ChatColor.RED + "Ce plot appartient au clan " + clan.getName() + ".");
            return;
        }

        event.setCancelled(true);
        plugin.menuUI().openTerrainSettings(player, clan, index);
    }

    private void handleLectern(PlayerInteractEvent event, Block block) {
        Location loc = block.getLocation();
        Optional<Clan> optClan = plugin.clans().getClanByLocation(loc);
        if (optClan.isEmpty()) return;

        Clan clan = optClan.get();
        Territory territory = clan.getTerritory();
        if (territory == null) return;
        if (!isWithinCenter(loc, territory)) return;

        Player player = event.getPlayer();
        Optional<Clan> playerClan = plugin.clans().getClanByPlayer(player.getUniqueId());
        if (playerClan.isEmpty() || !playerClan.get().getId().equals(clan.getId())) {
            player.sendMessage(ChatColor.RED + "Ce centre appartient au clan " + clan.getName() + ".");
            return;
        }

        event.setCancelled(true);
        menuUI.openFor(player, clan);
    }

    private boolean isWithinCenter(Location loc, Territory territory) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(territory.getWorldName())) return false;
        int dx = Math.abs(loc.getBlockX() - territory.getCenterX());
        int dz = Math.abs(loc.getBlockZ() - territory.getCenterZ());
        if (dx > centerRadius || dz > centerRadius) return false;
        int dy = Math.abs(loc.getBlockY() - territory.getCenterY());
        return dy <= 6;
    }
}
