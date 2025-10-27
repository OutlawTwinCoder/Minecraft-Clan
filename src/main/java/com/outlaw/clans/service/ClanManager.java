package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.ClanRole;
import com.outlaw.clans.model.ClanRolePermission;
import com.outlaw.clans.model.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final OutlawClansPlugin plugin;
    private final Map<java.util.UUID, Clan> clans = new HashMap<>();
    private final Map<java.util.UUID, java.util.UUID> playerClan = new HashMap<>();
    private final Map<String, ClanRole> roleDefaults = new LinkedHashMap<>();
    private String defaultRoleId;

    private File clansFile;
    private YamlConfiguration clansCfg;

    public ClanManager(OutlawClansPlugin plugin) { this.plugin = plugin; loadRoleDefaults(); loadAll(); }

    public Optional<Clan> getClan(java.util.UUID id) { return Optional.ofNullable(clans.get(id)); }
    public Optional<Clan> getClanByPlayer(java.util.UUID player) {
        java.util.UUID id = playerClan.get(player);
        return id == null ? Optional.empty() : Optional.ofNullable(clans.get(id));
    }

    public Optional<Clan> getClanByLocation(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        for (Clan clan : clans.values()) {
            if (clan.isInside(location)) return Optional.of(clan);
        }
        return Optional.empty();
    }

    public Clan createClan(String name, java.util.UUID leader) {
        java.util.UUID id = java.util.UUID.randomUUID();
        Clan c = new Clan(id, name, leader);
        applyRoleDefaults(c);
        clans.put(id, c); playerClan.put(leader, id); saveAll(); return c;
    }

    public void addMember(Clan clan, java.util.UUID player) {
        clan.getMembers().add(player);
        playerClan.put(player, clan.getId());
        if (!clan.isLeader(player)) {
            String roleId = clan.getDefaultRoleId();
            if (roleId == null || !clan.getRoles().containsKey(roleId)) {
                roleId = roleDefaults.isEmpty() ? null : roleDefaults.keySet().iterator().next();
            }
            if (roleId != null) {
                clan.assignRole(player, roleId);
            }
        }
        saveAll();
    }

    public void removeMember(Clan clan, java.util.UUID player) {
        clan.getMembers().remove(player); playerClan.remove(player);
        clan.getMemberRoles().remove(player);
        if (clan.getMembers().isEmpty()) { disbandClan(clan); return; }
        saveAll();
    }

    public void disbandClan(Clan clan) {
        try {
            for (var spot : clan.getSpots()) {
                if (spot.getNpcUuid() != null) {
                    java.util.UUID id = spot.getNpcUuid();
                    org.bukkit.entity.Entity e = null;
                    for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                        e = w.getEntity(id);
                        if (e != null) { e.remove(); break; }
                    }
                }
            }
        } catch (Throwable ignored) {}
        clans.remove(clan.getId());
        for (java.util.UUID u : new ArrayList<>(clan.getMembers())) playerClan.remove(u);
        saveAll();
    }

    public void setTerritory(Clan clan, Territory t, List<Location> spotBases) {
        clan.setTerritory(t); clan.getSpots().clear();
        for (Location loc : spotBases) clan.getSpots().add(new BuildingSpot(loc));
        saveAll();
    }

    public Collection<Clan> allClans() { return clans.values(); }

    public void saveAll() {
        if (clansFile == null) clansFile = new File(plugin.getDataFolder(), "clans.yml");
        clansCfg = new YamlConfiguration();
        for (Clan c : clans.values()) {
            String path = "clans." + c.getId();
            clansCfg.set(path + ".name", c.getName());
            clansCfg.set(path + ".leader", c.getLeader().toString());
            List<String> mems = new ArrayList<>();
            for (java.util.UUID u : c.getMembers()) mems.add(u.toString());
            clansCfg.set(path + ".members", mems);
            if (!c.getRoles().isEmpty()) {
                for (ClanRole role : c.getRoles().values()) {
                    String rp = path + ".roles." + role.getId();
                    clansCfg.set(rp + ".name", role.getDisplayName());
                    List<String> perms = new ArrayList<>();
                    for (ClanRolePermission perm : role.getPermissions()) {
                        perms.add(perm.name());
                    }
                    clansCfg.set(rp + ".permissions", perms);
                }
            }
            if (c.getDefaultRoleId() != null) {
                clansCfg.set(path + ".roles_default", c.getDefaultRoleId());
            }
            if (!c.getMemberRoles().isEmpty()) {
                for (Map.Entry<java.util.UUID, String> entry : c.getMemberRoles().entrySet()) {
                    clansCfg.set(path + ".member_roles." + entry.getKey(), entry.getValue());
                }
            }
            if (c.getTerritory() != null) {
                var t = c.getTerritory();
                clansCfg.set(path + ".territory.world", t.getWorldName());
                clansCfg.set(path + ".territory.radius", t.getRadius());
                clansCfg.set(path + ".territory.center", Arrays.asList(t.getCenterX(), t.getCenterY(), t.getCenterZ()));
            }
            for (int i=0;i<c.getSpots().size();i++) {
                var s = c.getSpots().get(i);
                String p = path + ".spots." + i;
                var l = s.getBaseLocation();
                if (l != null) {
                    clansCfg.set(p+".world", l.getWorld()!=null?l.getWorld().getName():plugin.getConfig().getString("territory.world","world"));
                    clansCfg.set(p+".x", l.getBlockX());
                    clansCfg.set(p+".y", l.getBlockY());
                    clansCfg.set(p+".z", l.getBlockZ());
                }
                if (s.getNpcUuid()!=null) clansCfg.set(p+".npc", s.getNpcUuid().toString());
                if (s.getFarmTypeId() != null) clansCfg.set(p+".farm_type", s.getFarmTypeId());
                if (s.getSchematicName() != null) clansCfg.set(p+".schematic", s.getSchematicName());
                if (s.getFarmChestLocation() != null) {
                    Location chest = s.getFarmChestLocation();
                    clansCfg.set(p+".chest.world", chest.getWorld() != null ? chest.getWorld().getName() : plugin.getConfig().getString("territory.world","world"));
                    clansCfg.set(p+".chest.x", chest.getBlockX());
                    clansCfg.set(p+".chest.y", chest.getBlockY());
                    clansCfg.set(p+".chest.z", chest.getBlockZ());
                }
                if (s.getResourcePreference() != null) clansCfg.set(p+".resource", s.getResourcePreference());
            }
        }
        try { clansCfg.save(clansFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadAll() {
        clansFile = new File(plugin.getDataFolder(), "clans.yml"); if (!clansFile.exists()) return;
        clansCfg = YamlConfiguration.loadConfiguration(clansFile);
        var sec = clansCfg.getConfigurationSection("clans"); if (sec == null) return;
        for (String idStr : sec.getKeys(false)) {
            java.util.UUID id = java.util.UUID.fromString(idStr);
            String base = "clans."+idStr;
            String name = clansCfg.getString(base+".name","Clan");
            java.util.UUID leader = java.util.UUID.fromString(clansCfg.getString(base+".leader"));
            Clan c = new Clan(id, name, leader);
            for (String m : clansCfg.getStringList(base+".members")) {
                try { java.util.UUID u = java.util.UUID.fromString(m); c.getMembers().add(u); playerClan.put(u, id); } catch (Exception ignored) {}
            }
            if (clansCfg.isConfigurationSection(base+".roles")) {
                ConfigurationSection rolesSection = clansCfg.getConfigurationSection(base+".roles");
                for (String roleId : rolesSection.getKeys(false)) {
                    String rp = base+".roles."+roleId;
                    String roleName = clansCfg.getString(rp+".name", roleId);
                    List<String> permList = clansCfg.getStringList(rp+".permissions");
                    EnumSet<ClanRolePermission> perms = EnumSet.noneOf(ClanRolePermission.class);
                    for (String permKey : permList) {
                        try {
                            perms.add(ClanRolePermission.valueOf(permKey));
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Permission inconnue " + permKey + " pour le rôle " + roleId + " du clan " + name);
                        }
                    }
                    c.getRoles().put(roleId, new ClanRole(roleId, ChatColor.translateAlternateColorCodes('&', roleName), perms));
                }
            }
            c.setDefaultRoleId(clansCfg.getString(base+".roles_default", null));
            if (clansCfg.isConfigurationSection(base+".member_roles")) {
                ConfigurationSection mr = clansCfg.getConfigurationSection(base+".member_roles");
                for (String key : mr.getKeys(false)) {
                    try {
                        java.util.UUID memberId = java.util.UUID.fromString(key);
                        c.getMemberRoles().put(memberId, mr.getString(key));
                    } catch (Exception ignored) {}
                }
            }
            if (clansCfg.isConfigurationSection(base+".territory")) {
                String w = clansCfg.getString(base+".territory.world");
                int r = clansCfg.getInt(base+".territory.radius", 75);
                var center = clansCfg.getIntegerList(base+".territory.center");
                if (center.size()==3) c.setTerritory(new Territory(w, r, center.get(0), center.get(1), center.get(2)));
            }
            if (clansCfg.isConfigurationSection(base+".spots")) {
                var sSec = clansCfg.getConfigurationSection(base+".spots");
                for (String key : sSec.getKeys(false)) {
                    String p = base+".spots."+key;
                    String w = clansCfg.getString(p+".world", plugin.getConfig().getString("territory.world","world"));
                    var world = Bukkit.getWorld(w);
                    int x = clansCfg.getInt(p+".x"), y = clansCfg.getInt(p+".y"), z = clansCfg.getInt(p+".z");
                    var spot = new BuildingSpot(new Location(world, x+0.5, y, z+0.5));
                    String npc = clansCfg.getString(p+".npc", null);
                    if (npc != null) try { spot.setNpcUuid(java.util.UUID.fromString(npc)); } catch (Exception ignored) {}
                    spot.setFarmTypeId(clansCfg.getString(p+".farm_type", null));
                    spot.setSchematicName(clansCfg.getString(p+".schematic", null));
                    if (clansCfg.isConfigurationSection(p+".chest")) {
                        String cw = clansCfg.getString(p+".chest.world", w);
                        var chestWorld = Bukkit.getWorld(cw);
                        int cx = clansCfg.getInt(p+".chest.x");
                        int cy = clansCfg.getInt(p+".chest.y");
                        int cz = clansCfg.getInt(p+".chest.z");
                        spot.setFarmChestLocation(new Location(chestWorld, cx + 0.5, cy, cz + 0.5));
                    }
                    spot.setResourcePreference(clansCfg.getString(p+".resource", null));
                    c.getSpots().add(spot);
                }
            }
            applyRoleDefaults(c);
            clans.put(id, c);
        }
    }

    private void loadRoleDefaults() {
        roleDefaults.clear();
        ConfigurationSection defaultsSection = plugin.getConfig().getConfigurationSection("roles.defaults");
        if (defaultsSection != null) {
            for (String roleId : defaultsSection.getKeys(false)) {
                String base = "roles.defaults." + roleId;
                String name = plugin.getConfig().getString(base + ".name", roleId);
                List<String> permStrings = plugin.getConfig().getStringList(base + ".permissions");
                EnumSet<ClanRolePermission> perms = EnumSet.noneOf(ClanRolePermission.class);
                for (String permKey : permStrings) {
                    try {
                        perms.add(ClanRolePermission.valueOf(permKey));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Permission inconnue " + permKey + " dans la configuration des rôles (" + roleId + ")");
                    }
                }
                roleDefaults.put(roleId, new ClanRole(roleId, ChatColor.translateAlternateColorCodes('&', name), perms));
            }
        }

        if (roleDefaults.isEmpty()) {
            ClanRole officer = new ClanRole("officer", ChatColor.GOLD + "Officier",
                    EnumSet.of(ClanRolePermission.BUILD_TERRAIN, ClanRolePermission.MANAGE_TERRAINS, ClanRolePermission.ACCESS_FARM_CHEST));
            ClanRole member = new ClanRole("member", ChatColor.GREEN + "Membre",
                    EnumSet.of(ClanRolePermission.BUILD_TERRAIN, ClanRolePermission.ACCESS_FARM_CHEST));
            ClanRole recruit = new ClanRole("recruit", ChatColor.GRAY + "Recrue", EnumSet.noneOf(ClanRolePermission.class));
            roleDefaults.put(officer.getId(), officer);
            roleDefaults.put(member.getId(), member);
            roleDefaults.put(recruit.getId(), recruit);
            defaultRoleId = recruit.getId();
        } else {
            defaultRoleId = plugin.getConfig().getString("roles.default");
        }

        if (defaultRoleId == null || !roleDefaults.containsKey(defaultRoleId)) {
            defaultRoleId = roleDefaults.keySet().stream().findFirst().orElse(null);
        }
    }

    private void applyRoleDefaults(Clan clan) {
        if (clan.getRoles().isEmpty()) {
            for (ClanRole template : roleDefaults.values()) {
                clan.getRoles().put(template.getId(), template.copy());
            }
        } else {
            for (Map.Entry<String, ClanRole> entry : roleDefaults.entrySet()) {
                clan.getRoles().putIfAbsent(entry.getKey(), entry.getValue().copy());
            }
        }

        if (clan.getDefaultRoleId() == null || !clan.getRoles().containsKey(clan.getDefaultRoleId())) {
            clan.setDefaultRoleId(defaultRoleId);
        }

        for (java.util.UUID member : clan.getMembers()) {
            if (clan.isLeader(member)) {
                continue;
            }
            if (!clan.getMemberRoles().containsKey(member)) {
                if (clan.getDefaultRoleId() != null) {
                    clan.assignRole(member, clan.getDefaultRoleId());
                }
            }
        }
    }
}
