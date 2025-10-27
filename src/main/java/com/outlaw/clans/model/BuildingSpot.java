package com.outlaw.clans.model;

import org.bukkit.Location;
import java.util.UUID;

public class BuildingSpot {
    private Location baseLocation;
    private UUID npcUuid;
    private String farmTypeId;
    private String schematicName;
    private Location farmChestLocation;
    private String resourcePreference;

    public BuildingSpot(Location baseLocation) {
        this.baseLocation = baseLocation;
    }

    public Location getBaseLocation() { return baseLocation; }
    public void setBaseLocation(Location baseLocation) { this.baseLocation = baseLocation; }
    public UUID getNpcUuid() { return npcUuid; }
    public void setNpcUuid(UUID npcUuid) { this.npcUuid = npcUuid; }

    public String getFarmTypeId() { return farmTypeId; }
    public void setFarmTypeId(String farmTypeId) { this.farmTypeId = farmTypeId; }

    public String getSchematicName() { return schematicName; }
    public void setSchematicName(String schematicName) { this.schematicName = schematicName; }

    public Location getFarmChestLocation() { return farmChestLocation; }
    public void setFarmChestLocation(Location farmChestLocation) { this.farmChestLocation = farmChestLocation; }

    public String getResourcePreference() { return resourcePreference; }
    public void setResourcePreference(String resourcePreference) { this.resourcePreference = resourcePreference; }
}
