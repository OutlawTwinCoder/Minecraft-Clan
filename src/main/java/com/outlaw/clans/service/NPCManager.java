package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.util.Keys;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class NPCManager implements Listener {

    public enum NpcType { TERRITORY_MERCHANT, BUILD_SPOT }

    static class Selection {
        String fileName;
        int plotIndex;
        int rotation;
        Selection(String f, int p, int r){fileName=f;plotIndex=p;rotation=r;}
    }

    private final OutlawClansPlugin plugin;
    private final NamespacedKey npcKey;
    private final NamespacedKey buildIndexKey;
    private final NamespacedKey clanMenuPlotKey;
    private final NamespacedKey clanMenuActionKey;
    private final Map<UUID, NpcType> npcTypes = new HashMap<>();
    private final Map<UUID, List<UUID>> activeDisplays = new HashMap<>();
    private final Map<UUID, Selection> selections = new HashMap<>();

    public NPCManager(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "npc-type");
        this.buildIndexKey = new NamespacedKey(plugin, "build-index");
        this.clanMenuPlotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
        this.clanMenuActionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ACTION);
    }

    public Villager spawnTerritoryMerchant(Location loc) {
        Location spawn = loc.clone(); spawn.setY(loc.getY() + 1.0);
        Villager v = spawn.getWorld().spawn(spawn, Villager.class, CreatureSpawnEvent.SpawnReason.CUSTOM);
        v.setAI(false); v.setAdult(); v.setProfession(Villager.Profession.NONE);
        v.setInvulnerable(true); v.setPersistent(true);
        v.setCustomName(ChatColor.GOLD + "Courtier de Territoire");
        v.setCustomNameVisible(true);
        v.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, NpcType.TERRITORY_MERCHANT.name());
        npcTypes.put(v.getUniqueId(), NpcType.TERRITORY_MERCHANT);
        return v;
    }

    public Villager spawnBuildingNpc(Location loc, int index) {
        Location spawn = loc.clone(); spawn.setY(loc.getY());
        Villager v = spawn.getWorld().spawn(spawn, Villager.class, CreatureSpawnEvent.SpawnReason.CUSTOM);
        v.setAI(false); v.setAdult(); v.setProfession(Villager.Profession.NONE);
        v.setInvulnerable(true); v.setPersistent(true);
        v.setCustomName(ChatColor.AQUA + "Plot #" + (index+1));
        v.setCustomNameVisible(true);
        v.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, NpcType.BUILD_SPOT.name());
        v.getPersistentDataContainer().set(buildIndexKey, PersistentDataType.INTEGER, index);
        npcTypes.put(v.getUniqueId(), NpcType.BUILD_SPOT);
        return v;
    }

    @EventHandler
    public void onNpcClick(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Villager v)) return;
        String type = v.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        if (type == null) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        NpcType npcType = NpcType.valueOf(type);
        switch (npcType) {
            case TERRITORY_MERCHANT -> openTerritoryShop(p);
            case BUILD_SPOT -> {
                Integer idx = v.getPersistentDataContainer().get(buildIndexKey, PersistentDataType.INTEGER);
                openSchematicShop(p, idx == null ? 0 : idx);
            }
        }
    }

    private ItemStack named(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore != null && lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
            meta.setLore(l);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
        return it;
    }

    private void openTerritoryShop(Player p) {
        String title = ChatColor.DARK_GREEN + "Achat de Territoire (" + (OutlawClansPlugin.get().economy().mode()==com.outlaw.clans.service.EconomyService.Mode.MONEY
                ? OutlawClansPlugin.get().economy().costTerritory() + "$"
                : OutlawClansPlugin.get().economy().costTerritory() + " " + OutlawClansPlugin.get().economy().itemType().name()) + ")";
        Inventory inv = Bukkit.createInventory(p, 27, title);
        int price = OutlawClansPlugin.get().economy().costTerritory();
        inv.setItem(11, named(Material.EMERALD_BLOCK, "&aAcheter un Territoire", "&7Prix: &e" + price));
        inv.setItem(15, named(Material.BARRIER, "&cFermer"));
        p.openInventory(inv);
    }

    public void openSchematicShop(Player p, int buildIndex) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.DARK_AQUA + "Shop Schematics (Plot #" + (buildIndex+1) + ")");
        File dir = new File(Bukkit.getPluginsFolder(), "WorldEdit/schematics");
        List<String> white = plugin.getConfig().getStringList("schematics.whitelist");
        boolean require = plugin.getConfig().getBoolean("schematics.require_whitelist", true);

        List<File> files = new ArrayList<>();
        if (require) {
            for (String n : white) {
                File f = new File(dir, n);
                if (f.exists()) files.add(f);
            }
            if (files.isEmpty()) {
                inv.setItem(22, named(Material.BARRIER, "&cSchematic manquante", "&7Placez " + String.join(", ", white) + " dans WorldEdit/schematics"));
                p.openInventory(inv); return;
            }
        } else {
            File[] all = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".schem") || name.toLowerCase().endsWith(".schematic"));
            if (all != null) { java.util.Arrays.sort(all); files.addAll(java.util.Arrays.asList(all)); }
            if (files.isEmpty()) {
                inv.setItem(22, named(Material.BARRIER, "&cAucun schematic trouvé", "&7Placez des .schem dans plugins/WorldEdit/schematics"));
                p.openInventory(inv); return;
            }
        }

        int slot = 0;
        for (File f : files) {
            if (slot >= inv.getSize()-1) break;
            String name = f.getName();
            inv.setItem(slot++, named(Material.PAPER, "&f" + name, "&7Clique pour construire"));
        }

        inv.setItem(45, named(Material.ARROW, "&e\u25C0 Rotation -90\u00B0", "&7Tourne et reconstruit"));
        inv.setItem(53, named(Material.ARROW, "&eRotation +90\u00B0 \u25B6", "&7Tourne et reconstruit"));
        Selection sel = selections.get(p.getUniqueId());
        if (sel != null && sel.plotIndex == buildIndex) {
            inv.setItem(49, named(Material.COMPASS, "&bActuel:", "&7"+sel.fileName, "&7Rotation: "+(sel.rotation*90)+"\u00B0"));
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title == null) return;

        if (ChatColor.stripColor(title).startsWith("Achat de Territoire")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;
            if (clicked.getType() == Material.EMERALD_BLOCK) {
                var opt = OutlawClansPlugin.get().clans().getClanByPlayer(p.getUniqueId());
                if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return; }
                Clan clan = opt.get();
                if (!clan.isLeader(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Seul le leader peut acheter un territoire."); return; }
                if (clan.hasTerritory()) { p.sendMessage(ChatColor.YELLOW + "Votre clan possède déjà un territoire."); return; }

                if (!OutlawClansPlugin.get().economy().chargeTerritory(p)) { p.sendMessage(ChatColor.RED+"Fonds insuffisants."); return; }
                giveClaimStick(p);
                p.closeInventory();
                p.sendMessage(ChatColor.GREEN + "Achat validé. Vous avez reçu le Claim Clan Stick.");
                p.sendMessage(ChatColor.GRAY + "Clique droit sur le bloc au centre de votre territoire.");
            }
            if (clicked.getType() == Material.BARRIER) p.closeInventory();
            return;
        }

        if (ChatColor.stripColor(title).startsWith("Shop Schematics (Plot #")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;
            int plotIndex = parsePlotIndexFromTitle(title);

            if (clicked.getType() == Material.PAPER) {
                String fileName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                selections.put(p.getUniqueId(), new Selection(fileName, plotIndex, 0));
                applyBuild(p, plotIndex, fileName, 0);
                openSchematicShop(p, plotIndex);
                return;
            }
            if (clicked.getType() == Material.ARROW) {
                Selection sel = selections.get(p.getUniqueId());
                if (sel == null || sel.plotIndex != plotIndex) { p.sendMessage(ChatColor.RED + "Sélectionnez d'abord un schematic."); return; }
                String dn = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                if (dn.contains("-90")) sel.rotation = ((sel.rotation - 1) % 4 + 4) % 4; else sel.rotation = (sel.rotation + 1) % 4;
                applyBuild(p, sel.plotIndex, sel.fileName, sel.rotation);
                openSchematicShop(p, plotIndex);
                return;
            }
        }

        if (ChatColor.stripColor(title).startsWith("Clan:")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            var container = meta.getPersistentDataContainer();
            if (container.has(clanMenuActionKey, PersistentDataType.STRING)) {
                String action = container.get(clanMenuActionKey, PersistentDataType.STRING);
                handleClanMenuAction(p, action);
                return;
            }
            if (!container.has(clanMenuPlotKey, PersistentDataType.INTEGER)) return;
            Integer plotIndex = container.get(clanMenuPlotKey, PersistentDataType.INTEGER);
            if (plotIndex != null) {
                openSchematicShop(p, plotIndex);
            }
        }
    }

    private void handleClanMenuAction(Player player, String action) {
        if (action == null) {
            return;
        }

        switch (action) {
            case "close" -> player.closeInventory();
            case "home", "members", "plots" -> {
                var optClan = plugin.clans().getClanByPlayer(player.getUniqueId());
                if (optClan.isEmpty()) {
                    player.closeInventory();
                    return;
                }
                Clan clan = optClan.get();
                switch (action) {
                    case "home" -> plugin.menuUI().openHome(player, clan);
                    case "members" -> plugin.menuUI().openMembers(player, clan);
                    case "plots" -> plugin.menuUI().openPlots(player, clan);
                }
            }
        }
    }

    private int parsePlotIndexFromTitle(String title) {
        try {
            int hash = title.indexOf("#"); int close = title.indexOf(")", hash);
            String num = title.substring(hash+1, close); return Integer.parseInt(num) - 1;
        } catch (Exception e) { return 0; }
    }

    private void applyBuild(Player p, int plotIndex, String fileName, int rotation) {
        var opt = plugin.clans().getClanByPlayer(p.getUniqueId());
        if (opt.isEmpty()) { p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan."); return; }
        Clan clan = opt.get();
        if (!clan.hasTerritory()) { p.sendMessage(ChatColor.RED + "Aucun territoire défini."); return; }
        if (plotIndex < 0 || plotIndex >= clan.getSpots().size()) { p.sendMessage(ChatColor.RED + "Plot invalide."); return; }
        BuildingSpot spot = clan.getSpots().get(plotIndex);

        int size = plugin.getConfig().getInt("building.plot_size", 35);
        int thickness = plugin.getConfig().getInt("terraform.thickness", 10);
        int clear = plugin.getConfig().getInt("terraform.clear_above", 24);
        int perTick = plugin.getConfig().getInt("terraform.blocks_per_tick", 1500);
        org.bukkit.Material foundation = org.bukkit.Material.valueOf(plugin.getConfig().getString("terraform.material","DIRT").toUpperCase());

        plugin.terraform().clearPlotEntities(spot.getBaseLocation(), size, thickness, clear);
        plugin.terraform().buildPlatform(spot.getBaseLocation(), size, thickness, foundation);

        int wait = plugin.terraform().estimateTicksForPlatform(size, thickness, clear, perTick);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File dir = new File(Bukkit.getPluginsFolder(), "WorldEdit/schematics");
            File file = new File(dir, fileName);
            if (!file.exists()) { p.sendMessage(ChatColor.RED + "Fichier introuvable: " + fileName); return; }
            if (!plugin.schematics().paste(file, spot.getBaseLocation(), rotation)) {
                p.sendMessage(ChatColor.RED + "Erreur WorldEdit pendant le collage.");
            } else {
                p.sendMessage(ChatColor.GREEN + "Construit " + fileName + " (rotation " + (rotation*90) + "°) sur Plot #" + (plotIndex+1));
            }
        }, wait);
    }

    private void giveClaimStick(Player p) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        String name = plugin.getConfig().getString("locales.claim_stick_name","&dClaim Clan Stick");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        java.util.List<String> lore = plugin.getConfig().getStringList("locales.claim_stick_lore");
        if (lore != null && !lore.isEmpty()) {
            java.util.List<String> l = new java.util.ArrayList<>();
            for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
            meta.setLore(l);
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "claim-stick"), PersistentDataType.BYTE, (byte)1);
        stick.setItemMeta(meta);
        p.getInventory().addItem(stick);
    }

    public void showTerritory(Player p, Clan clan) {
        if (!clan.hasTerritory()) { p.sendMessage(ChatColor.RED + "Ce clan n'a pas de territoire."); return; }
        Territory t = clan.getTerritory();
        World w = Bukkit.getWorld(t.getWorldName());
        if (w == null) { p.sendMessage(ChatColor.RED + "Monde introuvable: " + t.getWorldName()); return; }

        int r = t.getRadius();
        int cx = t.getCenterX(), cy = t.getCenterY(), cz = t.getCenterZ();
        int step = Math.max(10, r / 10);

        java.util.List<Location> ring = new java.util.ArrayList<>();
        for (int x = cx - r; x <= cx + r; x += step) {
            ring.add(new Location(w, x + 0.5, cy + 1.0, cz - r + 0.5));
            ring.add(new Location(w, x + 0.5, cy + 1.0, cz + r + 0.5));
        }
        for (int z = cz - r; z <= cz + r; z += step) {
            ring.add(new Location(w, cx - r + 0.5, cy + 1.0, z + 0.5));
            ring.add(new Location(w, cx + r + 0.5, cy + 1.0, z + 0.5));
        }

        java.util.List<UUID> spawned = new java.util.ArrayList<>();
        for (Location loc : ring) {
            BlockDisplay bd = w.spawn(loc, BlockDisplay.class);
            bd.setBlock(Material.GLOWSTONE.createBlockData());
            bd.setGlowing(true);
            spawned.add(bd.getUniqueId());
        }
        activeDisplays.put(p.getUniqueId(), spawned);
        p.sendMessage(ChatColor.AQUA + "Affichage des limites pendant " + plugin.getConfig().getInt("ui.show_duration_seconds", 30) + "s.");

        int ticks = plugin.getConfig().getInt("ui.show_duration_seconds", 30) * 20;
        new BukkitRunnable() { @Override public void run() { despawnDisplaysFor(p.getUniqueId()); } }.runTaskLater(plugin, ticks);
    }

    public void despawnDisplaysFor(UUID player) {
        List<UUID> ids = activeDisplays.remove(player);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity e = null;
            for (World w : Bukkit.getWorlds()) { e = w.getEntity(id); if (e != null) break; }
            if (e != null) e.remove();
        }
    }
    public void despawnAllDisplays() { for (UUID u : new ArrayList<>(activeDisplays.keySet())) despawnDisplaysFor(u); }
}
