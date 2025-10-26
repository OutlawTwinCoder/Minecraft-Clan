package com.outlaw.clans.model;

import org.bukkit.Location;

public class Territory {
    private final String worldName;
    private final int radius;
    private final int cx, cy, cz;

    public Territory(String worldName, int radius, int cx, int cy, int cz) {
        this.worldName = worldName; this.radius = radius; this.cx = cx; this.cy = cy; this.cz = cz;
    }

    public String getWorldName() { return worldName; }
    public int getRadius() { return radius; }
    public int getCenterX() { return cx; }
    public int getCenterY() { return cy; }
    public int getCenterZ() { return cz; }

    public boolean isInside(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int dx = Math.abs(loc.getBlockX() - cx);
        int dz = Math.abs(loc.getBlockZ() - cz);
        return dx <= radius && dz <= radius;
    }
}
