package com.outlaw.clans.util;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class TerrainFenceBuilder {

    private TerrainFenceBuilder() {
    }

    public static void build(OutlawClansPlugin plugin, Location center) {
        if (plugin == null || center == null || center.getWorld() == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("building.fence.enabled", true)) {
            return;
        }

        World world = center.getWorld();
        int plotSize = plugin.getConfig().getInt("building.plot_size", 35);
        int offset = Math.max(0, plugin.getConfig().getInt("building.fence.offset", 1));
        int half = plotSize / 2 + offset;

        int minX = center.getBlockX() - half;
        int maxX = center.getBlockX() + half;
        int minZ = center.getBlockZ() - half;
        int maxZ = center.getBlockZ() + half;

        int baseYOffset = plugin.getConfig().getInt("building.fence.y_offset", 1);
        String materialKey = plugin.getConfig().getString("building.fence.material", "OAK_FENCE");
        Material fenceMaterial = Material.OAK_FENCE;
        if (materialKey != null) {
            Material candidate = Material.matchMaterial(materialKey.toUpperCase());
            if (candidate != null) {
                fenceMaterial = candidate;
            }
        }

        int segment = Math.max(1, plugin.getConfig().getInt("building.fence.segment_length", 6));
        int gap = Math.max(0, plugin.getConfig().getInt("building.fence.gap_length", 6));
        int cycle = Math.max(segment + gap, 1);
        int height = Math.max(1, plugin.getConfig().getInt("building.fence.height", 1));

        int fenceY = center.getBlockY() + baseYOffset;
        fenceY = Math.max(world.getMinHeight(), Math.min(fenceY, world.getMaxHeight() - height));

        int index = 0;
        for (int x = minX; x <= maxX; x++) {
            applyColumn(world, x, fenceY, minZ, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int z = minZ + 1; z <= maxZ - 1; z++) {
            applyColumn(world, maxX, fenceY, z, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int x = maxX; x >= minX; x--) {
            applyColumn(world, x, fenceY, maxZ, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
        for (int z = maxZ - 1; z >= minZ + 1; z--) {
            applyColumn(world, minX, fenceY, z, height, fenceMaterial, shouldPlaceFence(index++, segment, cycle));
        }
    }

    private static void applyColumn(World world, int x, int y, int z, int height, Material fenceMaterial, boolean place) {
        for (int dy = 0; dy < height; dy++) {
            org.bukkit.block.Block block = world.getBlockAt(x, y + dy, z);
            if (place) {
                block.setType(fenceMaterial, false);
            } else {
                Material current = block.getType();
                if (current == fenceMaterial || current.name().endsWith("_FENCE") || current.name().endsWith("_WALL")) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private static boolean shouldPlaceFence(int index, int segment, int cycle) {
        if (cycle <= 0) {
            return true;
        }
        int mod = index % cycle;
        return mod < segment;
    }
}
