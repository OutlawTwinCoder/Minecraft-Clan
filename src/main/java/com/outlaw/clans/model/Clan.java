package com.outlaw.clans.model;

import org.bukkit.Location;
import java.util.*;

public class Clan {
    private final java.util.UUID id;
    private String name;
    private java.util.UUID leader;
    private final java.util.Set<java.util.UUID> members = new java.util.HashSet<>();
    private Territory territory;
    private final java.util.List<BuildingSpot> spots = new java.util.ArrayList<>();

    public Clan(java.util.UUID id, String name, java.util.UUID leader) {
        this.id = id; this.name = name; this.leader = leader; this.members.add(leader);
    }

    public java.util.UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public java.util.UUID getLeader() { return leader; }
    public void setLeader(java.util.UUID leader) { this.leader = leader; }
    public java.util.Set<java.util.UUID> getMembers() { return members; }
    public Territory getTerritory() { return territory; }
    public void setTerritory(Territory territory) { this.territory = territory; }
    public java.util.List<BuildingSpot> getSpots() { return spots; }

    public boolean hasTerritory() { return territory != null; }
    public boolean isMember(java.util.UUID uuid) { return members.contains(uuid); }
    public boolean isLeader(java.util.UUID uuid) { return leader != null && leader.equals(uuid); }
    public boolean isInside(Location loc) { return territory != null && territory.isInside(loc); }
}
