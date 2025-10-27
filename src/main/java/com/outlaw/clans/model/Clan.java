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
    private final java.util.Map<String, ClanRole> roles = new java.util.LinkedHashMap<>();
    private final java.util.Map<java.util.UUID, String> memberRoles = new java.util.HashMap<>();
    private String defaultRoleId;
    private int currencyBalance;

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
    public java.util.Map<String, ClanRole> getRoles() { return roles; }
    public java.util.Map<java.util.UUID, String> getMemberRoles() { return memberRoles; }
    public String getDefaultRoleId() { return defaultRoleId; }
    public void setDefaultRoleId(String defaultRoleId) { this.defaultRoleId = defaultRoleId; }
    public int getCurrencyBalance() { return currencyBalance; }
    public void setCurrencyBalance(int currencyBalance) { this.currencyBalance = Math.max(0, currencyBalance); }
    public void addCurrency(int amount) { setCurrencyBalance(currencyBalance + Math.max(0, amount)); }
    public boolean withdrawCurrency(int amount) {
        if (amount <= 0) return true;
        if (currencyBalance < amount) return false;
        currencyBalance -= amount;
        return true;
    }

    public boolean hasTerritory() { return territory != null; }
    public boolean isMember(java.util.UUID uuid) { return members.contains(uuid); }
    public boolean isLeader(java.util.UUID uuid) { return leader != null && leader.equals(uuid); }
    public boolean isInside(Location loc) { return territory != null && territory.isInside(loc); }

    public ClanRole getRole(String id) {
        if (id == null) return null;
        return roles.get(id);
    }

    public String getRoleId(java.util.UUID member) {
        if (member == null) return null;
        return memberRoles.get(member);
    }

    public ClanRole getRoleFor(java.util.UUID member) {
        String roleId = getRoleId(member);
        return roleId == null ? null : getRole(roleId);
    }

    public void assignRole(java.util.UUID member, String roleId) {
        if (member == null) return;
        if (roleId == null || !roles.containsKey(roleId)) {
            memberRoles.remove(member);
        } else {
            memberRoles.put(member, roleId);
        }
    }

    public boolean hasPermission(java.util.UUID member, ClanRolePermission permission) {
        if (member == null || permission == null) {
            return false;
        }
        if (isLeader(member)) {
            return true;
        }
        ClanRole role = getRoleFor(member);
        return role != null && role.hasPermission(permission);
    }

    public boolean canManageTerrains(java.util.UUID member) {
        return isLeader(member) || hasPermission(member, ClanRolePermission.MANAGE_TERRAINS);
    }

    public boolean canManageRoles(java.util.UUID member) {
        return isLeader(member) || hasPermission(member, ClanRolePermission.MANAGE_ROLES);
    }

    public boolean canBuild(java.util.UUID member) {
        return isLeader(member) || hasPermission(member, ClanRolePermission.BUILD_TERRAIN);
    }

    public boolean canAccessFarmChests(java.util.UUID member) {
        return isLeader(member) || hasPermission(member, ClanRolePermission.ACCESS_FARM_CHEST);
    }

    public long countMembersWithRole(String roleId) {
        if (roleId == null) {
            return 0;
        }
        return memberRoles.values().stream().filter(roleId::equals).count();
    }
}
