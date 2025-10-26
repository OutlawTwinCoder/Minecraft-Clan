package com.outlaw.clans.service;

import com.outlaw.clans.model.Territory;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.List;

public class TerraformService {
    private final com.outlaw.clans.OutlawClansPlugin plugin;
    public TerraformService(com.outlaw.clans.OutlawClansPlugin plugin) { this.plugin = plugin; }

    private static class Change { final Block b; final Material m; Change(Block b, Material m){ this.b=b; this.m=m; }}

    // Helpers for smarter "natural ground" detection (ignores foliage & decor)
    private boolean isFoliageOrDecor(Material m) {
        String n = m.name();
        if (n.endsWith("_LEAVES") || n.endsWith("_SAPLING") || n.endsWith("_LOG") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")
            || n.endsWith("_WOOD") || n.endsWith("_PLANKS") || n.contains("MUSHROOM") || n.contains("VINE")
            || n.contains("BAMBOO") || n.contains("HANGING_ROOTS") || n.contains("GRASS")
            || n.contains("FLOWER") || n.contains("FERN") || n.contains("SEAGRASS") || n.contains("KELP")
            || n.contains("CARPET") || n.contains("LEVER") || n.contains("BUTTON") || n.contains("TORCH")
            || n.contains("FENCE") || n.contains("GLASS") || n.contains("ICE"))
            return true;
        return false;
    }
    private int findNaturalGroundY(World w, int x, int z) {
        int y = w.getMaxHeight() - 1;
        while (y >= w.getMinHeight()) {
            Material m = w.getBlockAt(x,y,z).getType();
            if (!m.isAir() && !isFoliageOrDecor(m) && m != Material.WATER && m != Material.LAVA) {
                return y;
            }
            y--;
        }
        return Math.max(w.getMinHeight(), w.getSeaLevel());
    }
    private int median3x3NaturalGroundY(World w, int x, int z) {
        int[] vals = new int[9]; int k=0;
        for (int dx=-1; dx<=1; dx++) for (int dz=-1; dz<=1; dz++)
            vals[k++] = findNaturalGroundY(w, x+dx, z+dz);
        java.util.Arrays.sort(vals);
        return vals[4];
    }

    public int estimateTicksForPlatform(int size, int thickness, int clearAbove, int perTick) {
        long columns = (long)size * (long)size;
        long perCol = (long)(thickness + clearAbove + 1);
        long blocks = columns * perCol;
        return (int)Math.max(1L, blocks / Math.max(1, perTick) + 10L);
    }

    public void buildPlatform(Location base, int size, int thickness, Material foundation) {
        if (base==null || base.getWorld()==null) return;
        World w = base.getWorld();
        int half = size/2;
        int surfaceY = base.getBlockY();
        int topClear = surfaceY + Math.max(0, plugin.getConfig().getInt("terraform.clear_above", 24));
        int bottomY = Math.max(w.getMinHeight(), surfaceY - thickness + 1);

        Material topMat;
        try { topMat = Material.valueOf(plugin.getConfig().getString("terraform.surface_top_material","GRASS_BLOCK").toUpperCase()); }
        catch (Exception e) { topMat = Material.GRASS_BLOCK; }

        Queue<Block> q = new ArrayDeque<>();
        for (int x = base.getBlockX()-half; x <= base.getBlockX()+half; x++) {
            for (int z = base.getBlockZ()-half; z <= base.getBlockZ()+half; z++) {
                for (int y = topClear; y >= surfaceY+1; y--) q.add(w.getBlockAt(x,y,z));
                for (int y = surfaceY; y >= bottomY; y--) q.add(w.getBlockAt(x,y,z));
            }
        }

        int perTick = plugin.getConfig().getInt("terraform.blocks_per_tick", 1500);
        final int sY = surfaceY; final Material top = topMat;
        new BukkitRunnable() {
            @Override public void run() {
                int n=0;
                while (!q.isEmpty() && n<perTick) {
                    Block b = q.poll();
                    if (b.getY() >= sY+1)       b.setType(Material.AIR, false);
                    else if (b.getY() == sY)     b.setType(top, false);
                    else                         b.setType(foundation, false);
                    n++;
                }
                if (q.isEmpty()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void buildTerritoryPlate(Territory t, int centerY, int thickness, Material topMat, Material foundation) {
        World w = Bukkit.getWorld(t.getWorldName());
        if (w == null) return;
        int r = t.getRadius();
        int minX = t.getCenterX()-r, maxX = t.getCenterX()+r;
        int minZ = t.getCenterZ()-r, maxZ = t.getCenterZ()+r;

        int bottomY = Math.max(w.getMinHeight(), centerY - thickness + 1);
        int topClear = Math.min(w.getMaxHeight()-1, centerY + Math.max(0, plugin.getConfig().getInt("terraform.clear_above", 24)));

        Queue<Block> q = new ArrayDeque<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = topClear; y >= centerY+1; y--) q.add(w.getBlockAt(x,y,z));
                for (int y = centerY; y >= bottomY; y--) q.add(w.getBlockAt(x,y,z));
            }
        }

        int perTick = plugin.getConfig().getInt("terraform.blocks_per_tick", 1500);
        new BukkitRunnable() {
            @Override public void run() {
                int n=0;
                while (!q.isEmpty() && n<perTick) {
                    Block b = q.poll();
                    if (b.getY() >= centerY+1)          b.setType(Material.AIR, false);
                    else if (b.getY() == centerY)        b.setType(topMat, false);
                    else                                  b.setType(foundation, false);
                    n++;
                }
                if (q.isEmpty()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void clearPlotEntities(Location base, int size, int thickness, int clearAbove) {
        if (!plugin.getConfig().getBoolean("terraform.kill_entities", true)) return;
        if (base==null || base.getWorld()==null) return;
        World w = base.getWorld();
        int half = size/2;
        int minX = base.getBlockX()-half, maxX = base.getBlockX()+half;
        int minZ = base.getBlockZ()-half, maxZ = base.getBlockZ()+half;
        int minY = Math.max(w.getMinHeight(), base.getBlockY() - thickness + 1);
        int maxY = Math.min(w.getMaxHeight()-1, base.getBlockY() + Math.max(0, clearAbove));

        BoundingBox bb = new BoundingBox(minX, minY, minZ, maxX+1, maxY+1, maxZ+1);
        for (Entity e : w.getNearbyEntities(bb)) {
            if (e instanceof Player) continue;
            if (e instanceof Villager v) {
                String type = v.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "npc-type"), PersistentDataType.STRING);
                if (type != null) continue;
            }
            e.remove();
        }
    }

    public void featherTerritoryEdges(Territory t, int centerY, int thickness, Material topMat, Material foundation, int width) {
        if (t == null || width <= 0) return;
        World w = Bukkit.getWorld(t.getWorldName());
        if (w == null) return;
        int r = t.getRadius();
        int cx = t.getCenterX(), cz = t.getCenterZ();
        int extraBelow = plugin.getConfig().getInt("feather.extra_depth_below", 20);
        int bottomY = Math.max(w.getMinHeight(), centerY - thickness - Math.max(0, extraBelow) + 1);
        int clearAbove = plugin.getConfig().getInt("terraform.clear_above", 24);

        ArrayDeque<Change> q = new ArrayDeque<>();

        int ext = r + width;
        for (int x = cx - ext; x <= cx + ext; x++) {
            for (int z = cz - ext; z <= cz + ext; z++) {
                int dx = Math.max(0, Math.abs(x - cx) - r);
                int dz = Math.max(0, Math.abs(z - cz) - r);
                int d = Math.max(dx, dz);
                if (d <= 0 || d > width) continue;

                double tBlend = d / (double) width;
                int naturalY = median3x3NaturalGroundY(w, x, z);
                int targetY = (int)Math.round((1.0 - tBlend) * centerY + tBlend * naturalY);

                int topClear = Math.min(w.getMaxHeight() - 1, Math.max(centerY, naturalY) + clearAbove);

                for (int y = topClear; y >= targetY + 1; y--) q.add(new Change(w.getBlockAt(x,y,z), Material.AIR));
                for (int y = targetY; y >= bottomY; y--) q.add(new Change(w.getBlockAt(x,y,z), y==targetY ? topMat : foundation));
            }
        }

        int perTick = Math.max(200, plugin.getConfig().getInt("terraform.blocks_per_tick", 1500));
        new BukkitRunnable() {
            @Override public void run() {
                int n = 0;
                while (!q.isEmpty() && n < perTick) {
                    Change c = q.poll();
                    c.b.setType(c.m, false);
                    n++;
                }
                if (q.isEmpty()) cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
