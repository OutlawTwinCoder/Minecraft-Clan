package com.outlaw.clans.model;

import java.util.EnumSet;
import java.util.Set;

public class ClanRole {

    private final String id;
    private String displayName;
    private final EnumSet<ClanRolePermission> permissions;

    public ClanRole(String id, String displayName) {
        this(id, displayName, EnumSet.noneOf(ClanRolePermission.class));
    }

    public ClanRole(String id, String displayName, Set<ClanRolePermission> permissions) {
        this.id = id;
        this.displayName = displayName;
        this.permissions = permissions == null || permissions.isEmpty()
                ? EnumSet.noneOf(ClanRolePermission.class)
                : EnumSet.copyOf(permissions);
    }

    public ClanRole copy() {
        return new ClanRole(id, displayName, permissions);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public EnumSet<ClanRolePermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(ClanRolePermission permission) {
        return permissions.contains(permission);
    }

    public void setPermission(ClanRolePermission permission, boolean enabled) {
        if (enabled) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }
}
