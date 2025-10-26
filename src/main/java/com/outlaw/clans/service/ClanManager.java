package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final OutlawClansPlugin plugin;
    private final Map<java.util.UUID, Clan> clans = new HashMap<>();
    private final Map<java.util.UUID, java.util.UUID> playerClan = new HashMap<>();

    private File clansFile;
    private YamlConfiguration clansCfg;

    public ClanManager(OutlawClansPlugin plugin) { this.plugin = plugin; loadAll(); }

    public Optional<Clan> getClan(java.util.UUID id) { return Optional.ofNullable(clans.get(id)); }
    public Optional<Clan> getClanByPlayer(java.util.UUID player) {
        java.util.UUID id = playerClan.get(player);
        return id == null ? Optional.empty() : Optional.ofNullable(clans.get(id));
    }

    public Clan createClan(String name, java.util.UUID leader) {
        java.util.UUID id = java.util.UUID.randomUUID();
        Clan c = new Clan(id, name, leader);
        clans.put(id, c); playerClan.put(leader, id); saveAll(); return c;
    }

    public void addMember(Clan clan, java.util.UUID player) { clan.getMembers().add(player); playerClan.put(player, clan.getId()); saveAll(); }

    public void removeMember(Clan clan, java.util.UUID player) {
        clan.getMembers().remove(player); playerClan.remove(player);
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
                    c.getSpots().add(spot);
                }
            }
            clans.put(id, c);
        }
    }
}
