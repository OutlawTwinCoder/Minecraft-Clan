package com.outlaw.clans.model;

import org.bukkit.Location;
import java.util.UUID;

public class BuildingSpot {
    private Location baseLocation;
    private UUID npcUuid;
    public BuildingSpot(Location baseLocation) { this.baseLocation = baseLocation; }
    public Location getBaseLocation() { return baseLocation; }
    public void setBaseLocation(Location baseLocation) { this.baseLocation = baseLocation; }
    public UUID getNpcUuid() { return npcUuid; }
    public void setNpcUuid(UUID npcUuid) { this.npcUuid = npcUuid; }
}
