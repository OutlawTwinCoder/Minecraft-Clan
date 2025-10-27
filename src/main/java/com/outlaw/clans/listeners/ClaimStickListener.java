package com.outlaw.clans.listeners;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.util.Keys;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.BlockFace;

import java.io.File;

public class ClaimStickListener implements Listener {

    private final OutlawClansPlugin plugin;
    private final NamespacedKey key;
    private final NamespacedKey plotSignKey;

    public ClaimStickListener(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "claim-stick");
        this.plotSignKey = new NamespacedKey(plugin, Keys.PLOT_SIGN);
    }

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

        bases.add(new Location(w, xLeft   + 0.5, by, zTop    + 0.5));
        bases.add(new Location(w, xCenter + 0.5, by, zTop    + 0.5));
        bases.add(new Location(w, xRight  + 0.5, by, zTop    + 0.5));
        bases.add(new Location(w, xLeft   + 0.5, by, zBottom + 0.5));
        bases.add(new Location(w, xCenter + 0.5, by, zBottom + 0.5));
        bases.add(new Location(w, xRight  + 0.5, by, zBottom + 0.5));

        plugin.clans().setTerritory(clan, t, bases);
        p.sendMessage(ChatColor.GREEN + "Centre de territoire défini. Rayon: " + r + " (≈" + (r*2) + "x" + (r*2) + ").");

        int thickness  = plugin.getConfig().getInt("terraform.thickness", 10);
        int clearAbove = plugin.getConfig().getInt("terraform.clear_above", 24);
        int perTick = plugin.getConfig().getInt("terraform.blocks_per_tick", 1500);
        boolean terraformFull = plugin.getConfig().getBoolean("terraform.full_territory", true);

        scheduleFenceGeneration(bases, t, thickness, clearAbove, perTick, terraformFull);

        if (terraformFull) {
            org.bukkit.Material topMat = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.surface_top_material","GRASS_BLOCK").toUpperCase());
            org.bukkit.Material foundation = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.material","DIRT").toUpperCase());
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
                if (center == null || center.getWorld() == null) continue;

                int phalf = plugin.getConfig().getInt("building.plot_size", 35) / 2;

                int dx = t.getCenterX() - center.getBlockX();
                int dz = t.getCenterZ() - center.getBlockZ();
                int sx = Integer.signum(dx);
                int sz = Integer.signum(dz);
                if (sx == 0) sx = (sz == 0 ? 1 : sz);
                if (sz == 0) sz = (sx == 0 ? 1 : sx);

                int cornerX = center.getBlockX() + sx * (phalf - 1);
                int cornerZ = center.getBlockZ() + sz * (phalf - 1);
                int cornerY = center.getBlockY() + 2;

                org.bukkit.Location signLoc = new org.bukkit.Location(center.getWorld(), cornerX, cornerY, cornerZ);
                placePlotSign(signLoc, dx, dz, i);
            }
        }, delay);

        if (plugin.getConfig().getBoolean("building.center_schematic.enabled", true)) {
            String fileName = plugin.getConfig().getString("building.center_schematic.file", "ClanCenter.schem");
            if (fileName != null && !fileName.trim().isEmpty()) {
                long wait = plugin.getConfig().getInt("building.center_schematic.extra_delay_ticks", 40);
                if (terraformFull) {
                    int size = t.getRadius() * 2 + 1;
                    wait += plugin.terraform().estimateTicksForPlatform(size, thickness, clearAbove, perTick);
                }
                long delayTicks = Math.max(0, wait);
                org.bukkit.Location center = new org.bukkit.Location(w, t.getCenterX() + 0.5, t.getCenterY(), t.getCenterZ() + 0.5);
                p.sendMessage(ChatColor.GRAY + "Construction du centre du clan programmée (" + fileName + ").");
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    File dir = new File(org.bukkit.Bukkit.getPluginsFolder(), "WorldEdit/schematics");
                    File file = new File(dir, fileName);
                    if (!file.exists()) {
                        p.sendMessage(ChatColor.RED + "Fichier central introuvable: " + fileName);
                        return;
                    }
                    if (!plugin.schematics().paste(file, center)) {
                        p.sendMessage(ChatColor.RED + "Erreur WorldEdit pendant le collage du centre.");
                    } else {
                        p.sendMessage(ChatColor.GREEN + "Centre de clan collé: " + fileName + ".");
                    }
                }, delayTicks);
            }
        }

        var stack = e.getItem();
        if (stack != null) {
            if (stack.getAmount() <= 1) {
                if (e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                    p.getInventory().setItemInMainHand(null);
                } else if (e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                    p.getInventory().setItemInOffHand(null);
                }
            } else {
                stack.setAmount(stack.getAmount() - 1);
            }
        }
    }

    private void placePlotSign(Location loc, int dx, int dz, int index) {
        if (loc.getWorld() == null) return;
        Block block = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();
        above.setType(Material.AIR, false);

        block.setType(Material.OAK_SIGN, false);
        var state = block.getState();
        if (!(state instanceof Sign signState)) return;

        signState.setLine(0, ChatColor.AQUA + "Terrain #" + (index + 1));
        signState.setLine(1, ChatColor.YELLOW + "Clique pour");
        signState.setLine(2, ChatColor.YELLOW + "les plans");
        signState.setLine(3, ChatColor.GRAY + "Rotation incluse");
        signState.getPersistentDataContainer().set(plotSignKey, PersistentDataType.INTEGER, index);
        signState.update(true, false);

        orientSignTowardsCenter(block, dx, dz);
    }

    private void scheduleFenceGeneration(java.util.List<Location> bases, Territory territory, int thickness, int clearAbove, int perTick, boolean terraformFull) {
        if (bases.isEmpty()) return;
        long delay = 40L;
        if (terraformFull && territory != null) {
            int size = territory.getRadius() * 2 + 1;
            delay += plugin.terraform().estimateTicksForPlatform(size, thickness, clearAbove, perTick);
        }
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location base : bases) {
                buildTerrainFences(base);
            }
        }, delay);
    }

    private void buildTerrainFences(Location center) {
        if (center == null || center.getWorld() == null) return;
        if (!plugin.getConfig().getBoolean("building.fence.enabled", true)) return;

        org.bukkit.World world = center.getWorld();
        int plotSize = plugin.getConfig().getInt("building.plot_size", 35);
        int offset = Math.max(0, plugin.getConfig().getInt("building.fence.offset", 1));
        int half = plotSize / 2 + offset;
        int minX = center.getBlockX() - half;
        int maxX = center.getBlockX() + half;
        int minZ = center.getBlockZ() - half;
        int maxZ = center.getBlockZ() + half;

        int baseYOffset = plugin.getConfig().getInt("building.fence.y_offset", 1);
        String materialKey = plugin.getConfig().getString("building.fence.material", "OAK_FENCE");
        org.bukkit.Material fenceMaterial = org.bukkit.Material.OAK_FENCE;
        if (materialKey != null) {
            org.bukkit.Material candidate = org.bukkit.Material.matchMaterial(materialKey.toUpperCase());
            if (candidate != null) fenceMaterial = candidate;
        }

        int segment = Math.max(1, plugin.getConfig().getInt("building.fence.segment_length", 6));
        int gap = Math.max(0, plugin.getConfig().getInt("building.fence.gap_length", 6));
        int cycle = Math.max(segment + gap, 1);
        int height = Math.max(1, plugin.getConfig().getInt("building.fence.height", 1));

        int fenceY = center.getBlockY() + baseYOffset;
        fenceY = Math.max(world.getMinHeight(), Math.min(fenceY, world.getMaxHeight() - height));

        int index = 0;
        for (int x = minX; x <= maxX; x++) {
            applyFenceColumn(world, x, fenceY, minZ, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            applyFenceColumn(world, maxX, fenceY, z, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int x = maxX; x >= minX; x--) {
            applyFenceColumn(world, x, fenceY, maxZ, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int z = maxZ - 1; z >= minZ + 1; z--) {
            applyFenceColumn(world, minX, fenceY, z, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
    }

    private void applyFenceColumn(org.bukkit.World world, int x, int y, int z, int height, org.bukkit.Material fenceMaterial, boolean place) {
        for (int dy = 0; dy < height; dy++) {
            org.bukkit.block.Block block = world.getBlockAt(x, y + dy, z);
            if (place) {
                block.setType(fenceMaterial, false);
            } else {
                org.bukkit.Material current = block.getType();
                if (current == fenceMaterial || current.name().endsWith("_FENCE") || current.name().endsWith("_WALL")) {
                    block.setType(org.bukkit.Material.AIR, false);
                }
            }
        }
    }

    private boolean shouldPlaceFence(int index, int segment, int cycle) {
        if (cycle <= 0) return true;
        int mod = index % cycle;
        return mod < segment;
    }

    private void orientSignTowardsCenter(Block block, int dx, int dz) {
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.type.Sign signData) {
            BlockFace face = facingFromVector(dx, dz); // ← utilise BlockFace
            signData.setRotation(face);
            block.setBlockData(signData, false);
        } else if (data instanceof WallSign wall) {
            BlockFace face = facingFromVector(dx, dz);
            wall.setFacing(face);
            block.setBlockData(wall, false);
        }
    }

    private BlockFace facingFromVector(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        if (Math.abs(dz) > Math.abs(dx)) {
            return dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
        if (dx >= 0 && dz >= 0) return BlockFace.SOUTH_EAST;
        if (dx >= 0) return BlockFace.NORTH_EAST;
        if (dz >= 0) return BlockFace.SOUTH_WEST;
        return BlockFace.NORTH_WEST;
    }
}
