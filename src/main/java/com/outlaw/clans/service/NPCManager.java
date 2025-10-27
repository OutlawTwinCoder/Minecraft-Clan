package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.ClanRole;
import com.outlaw.clans.model.ClanRolePermission;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.ui.ClanMenuUI;
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

import java.util.*;

public class NPCManager implements Listener {

    public enum NpcType { TERRITORY_MERCHANT, BUILD_SPOT }

    private final OutlawClansPlugin plugin;
    private final NamespacedKey npcKey;
    private final NamespacedKey buildIndexKey;
    private final NamespacedKey clanMenuPlotKey;
    private final NamespacedKey clanMenuActionKey;
    private final NamespacedKey clanMenuTypeKey;
    private final NamespacedKey clanMenuResourceKey;
    private final NamespacedKey clanMenuSchematicKey;
    private final NamespacedKey clanMenuRoleKey;
    private final NamespacedKey clanMenuPermissionKey;
    private final NamespacedKey clanMenuMemberKey;
    private final NamespacedKey clanMenuBankActionKey;
    private final NamespacedKey clanMenuBankAmountKey;
    private final Map<UUID, NpcType> npcTypes = new HashMap<>();
    private final Map<UUID, List<UUID>> activeDisplays = new HashMap<>();

    public NPCManager(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "npc-type");
        this.buildIndexKey = new NamespacedKey(plugin, "build-index");
        this.clanMenuPlotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
        this.clanMenuActionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ACTION);
        this.clanMenuTypeKey = new NamespacedKey(plugin, Keys.CLAN_MENU_TYPE);
        this.clanMenuResourceKey = new NamespacedKey(plugin, Keys.CLAN_MENU_RESOURCE);
        this.clanMenuSchematicKey = new NamespacedKey(plugin, Keys.CLAN_MENU_SCHEMATIC);
        this.clanMenuRoleKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ROLE);
        this.clanMenuPermissionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PERMISSION);
        this.clanMenuMemberKey = new NamespacedKey(plugin, Keys.CLAN_MENU_MEMBER);
        this.clanMenuBankActionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_BANK_ACTION);
        this.clanMenuBankAmountKey = new NamespacedKey(plugin, Keys.CLAN_MENU_BANK_AMOUNT);
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
        v.setCustomName(ChatColor.AQUA + "Terrain #" + (index+1));
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
                int terrainIndex = idx == null ? 0 : idx;
                var optClan = plugin.clans().getClanByPlayer(p.getUniqueId());
                if (optClan.isEmpty()) {
                    p.sendMessage(ChatColor.RED + "Vous n'êtes dans aucun clan.");
                    return;
                }
                plugin.menuUI().openTerrainSettings(p, optClan.get(), terrainIndex);
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
        int price = OutlawClansPlugin.get().economy().costTerritory();
        String title = ChatColor.DARK_GREEN + "Achat de Territoire (" + OutlawClansPlugin.get().economy().formatAmount(price) + ")";
        Inventory inv = Bukkit.createInventory(p, 27, title);
        inv.setItem(11, named(Material.EMERALD_BLOCK, "&aAcheter un Territoire",
                "&7Prix: &e" + OutlawClansPlugin.get().economy().formatAmount(price)));
        inv.setItem(15, named(Material.BARRIER, "&cFermer"));
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

        if (ChatColor.stripColor(title).startsWith("Clan:")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null) return;
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            var container = meta.getPersistentDataContainer();
            if (container.has(clanMenuActionKey, PersistentDataType.STRING)) {
                String action = container.get(clanMenuActionKey, PersistentDataType.STRING);
                handleClanMenuAction(p, meta, action);
            }
        }
    }

    private void handleClanMenuAction(Player player, ItemMeta meta, String action) {
        if (action == null) {
            return;
        }

        switch (action) {
            case "close" -> player.closeInventory();
            case "home", "members", "terrains", "terrain-settings", "terrain-buildings", "terrain-resources",
                    "terrain-select-type", "terrain-select-schematic", "terrain-set-resource", "roles",
                    "role-settings", "role-toggle", "member-assign", "member-role-set",
                    "bank", "bank-pick", "bank-amount", "bank-give-member", "territory-upgrade" -> {
                var optClan = plugin.clans().getClanByPlayer(player.getUniqueId());
                if (optClan.isEmpty()) {
                    player.closeInventory();
                    return;
                }
                Clan clan = optClan.get();
                switch (action) {
                    case "home" -> plugin.menuUI().openHome(player, clan);
                    case "members" -> plugin.menuUI().openMembers(player, clan);
                    case "bank" -> plugin.menuUI().openBank(player, clan);
                    case "terrains" -> plugin.menuUI().openTerrains(player, clan);
                    case "terrain-settings" -> {
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        plugin.menuUI().openTerrainSettings(player, clan, idx == null ? 0 : idx);
                    }
                    case "terrain-buildings" -> {
                        if (!clan.canManageTerrains(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas modifier les terrains.");
                            return;
                        }
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        plugin.menuUI().openBuildingTypes(player, clan, idx == null ? 0 : idx);
                    }
                    case "terrain-resources" -> {
                        if (!clan.canManageTerrains(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas modifier les terrains.");
                            return;
                        }
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        plugin.menuUI().openResourcePreferences(player, clan, idx == null ? 0 : idx);
                    }
                    case "terrain-select-type" -> {
                        if (!clan.canManageTerrains(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas modifier les terrains.");
                            return;
                        }
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        String typeId = meta.getPersistentDataContainer().get(clanMenuTypeKey, PersistentDataType.STRING);
                        var optType = plugin.farms().getType(typeId);
                        if (optType.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Type de bâtiment inconnu.");
                            return;
                        }
                        plugin.menuUI().openBuildingSchematics(player, clan, idx == null ? 0 : idx, optType.get());
                    }
                    case "terrain-select-schematic" -> {
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        String typeId = meta.getPersistentDataContainer().get(clanMenuTypeKey, PersistentDataType.STRING);
                        String schematic = meta.getPersistentDataContainer().get(clanMenuSchematicKey, PersistentDataType.STRING);
                        if (idx == null || typeId == null || schematic == null) {
                            player.sendMessage(ChatColor.RED + "Données manquantes pour la construction.");
                            return;
                        }
                        var optType = plugin.farms().getType(typeId);
                        if (optType.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Type de bâtiment inconnu.");
                            return;
                        }
                        plugin.farms().buildStructure(player, clan, idx, optType.get(), schematic);
                        plugin.menuUI().openTerrainSettings(player, clan, idx);
                    }
                    case "terrain-set-resource" -> {
                        if (!clan.canManageTerrains(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas modifier les terrains.");
                            return;
                        }
                        Integer idx = meta.getPersistentDataContainer().get(clanMenuPlotKey, PersistentDataType.INTEGER);
                        String resource = meta.getPersistentDataContainer().get(clanMenuResourceKey, PersistentDataType.STRING);
                        if (idx == null || resource == null) {
                            player.sendMessage(ChatColor.RED + "Sélection invalide.");
                            return;
                        }
                        if (idx < 0 || idx >= clan.getSpots().size()) {
                            player.sendMessage(ChatColor.RED + "Terrain invalide.");
                            return;
                        }
                        var spot = clan.getSpots().get(idx);
                        var optType = plugin.farms().getType(spot.getFarmTypeId());
                        if (optType.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "Aucun bâtiment configuré pour ce terrain.");
                            return;
                        }
                        Material mat = Material.matchMaterial(resource);
                        if (mat == null || !optType.get().getOutputs().containsKey(mat)) {
                            player.sendMessage(ChatColor.RED + "Ressource invalide pour cette ferme.");
                            return;
                        }
                        spot.setResourcePreference(resource);
                        plugin.clans().saveAll();
                        player.sendMessage(ChatColor.GREEN + "Préférence mise à jour: " + ChatColor.YELLOW + formatResourceName(resource));
                        plugin.menuUI().openTerrainSettings(player, clan, idx);
                    }
                    case "roles" -> {
                        if (!clan.canManageRoles(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas gérer les rôles.");
                            return;
                        }
                        plugin.menuUI().openRoles(player, clan);
                    }
                    case "bank-pick" -> {
                        String bankAction = meta.getPersistentDataContainer().get(clanMenuBankActionKey, PersistentDataType.STRING);
                        ClanMenuUI.BankAction selected = ClanMenuUI.BankAction.fromKey(bankAction);
                        if (selected == null) {
                            plugin.menuUI().openBank(player, clan);
                        } else {
                            plugin.menuUI().openBankAmounts(player, clan, selected);
                        }
                    }
                    case "bank-amount" -> {
                        String bankAction = meta.getPersistentDataContainer().get(clanMenuBankActionKey, PersistentDataType.STRING);
                        Integer amount = meta.getPersistentDataContainer().get(clanMenuBankAmountKey, PersistentDataType.INTEGER);
                        ClanMenuUI.BankAction selected = ClanMenuUI.BankAction.fromKey(bankAction);
                        if (amount == null) {
                            amount = 0;
                        }
                        plugin.menuUI().handleBankAmount(player, clan, selected, amount);
                    }
                    case "bank-give-member" -> {
                        Integer amount = meta.getPersistentDataContainer().get(clanMenuBankAmountKey, PersistentDataType.INTEGER);
                        String memberId = meta.getPersistentDataContainer().get(clanMenuMemberKey, PersistentDataType.STRING);
                        if (amount == null || memberId == null) {
                            player.sendMessage(ChatColor.RED + "Sélection invalide.");
                            plugin.menuUI().openBank(player, clan);
                            return;
                        }
                        try {
                            UUID target = UUID.fromString(memberId);
                            plugin.menuUI().handleBankTransfer(player, clan, amount, target);
                        } catch (IllegalArgumentException ex) {
                            player.sendMessage(ChatColor.RED + "Membre invalide.");
                            plugin.menuUI().openBank(player, clan);
                        }
                    }
                    case "territory-upgrade" -> plugin.menuUI().handleTerritoryUpgrade(player, clan);
                    case "role-settings" -> {
                        if (!clan.canManageRoles(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas gérer les rôles.");
                            return;
                        }
                        String roleId = meta.getPersistentDataContainer().get(clanMenuRoleKey, PersistentDataType.STRING);
                        plugin.menuUI().openRoleSettings(player, clan, roleId);
                    }
                    case "role-toggle" -> {
                        if (!clan.canManageRoles(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas gérer les rôles.");
                            return;
                        }
                        String roleId = meta.getPersistentDataContainer().get(clanMenuRoleKey, PersistentDataType.STRING);
                        String permKey = meta.getPersistentDataContainer().get(clanMenuPermissionKey, PersistentDataType.STRING);
                        if (roleId == null || permKey == null) {
                            player.sendMessage(ChatColor.RED + "Sélection invalide.");
                            return;
                        }
                        ClanRole role = clan.getRole(roleId);
                        if (role == null) {
                            player.sendMessage(ChatColor.RED + "Rôle introuvable.");
                            return;
                        }
                        try {
                            ClanRolePermission permission = ClanRolePermission.valueOf(permKey);
                            boolean enable = !role.hasPermission(permission);
                            role.setPermission(permission, enable);
                            plugin.clans().saveAll();
                            player.sendMessage(ChatColor.GREEN + "Permission " + permission.name() + (enable ? " activée" : " désactivée") + " pour " + ChatColor.stripColor(role.getDisplayName()) + ".");
                        } catch (IllegalArgumentException ex) {
                            player.sendMessage(ChatColor.RED + "Permission inconnue.");
                        }
                        plugin.menuUI().openRoleSettings(player, clan, roleId);
                    }
                    case "member-assign" -> {
                        if (!clan.canManageRoles(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas gérer les rôles.");
                            return;
                        }
                        String memberId = meta.getPersistentDataContainer().get(clanMenuMemberKey, PersistentDataType.STRING);
                        if (memberId == null) {
                            return;
                        }
                        try {
                            UUID target = UUID.fromString(memberId);
                            plugin.menuUI().openMemberRoleSelect(player, clan, target);
                        } catch (IllegalArgumentException ignored) {
                            player.sendMessage(ChatColor.RED + "Membre invalide.");
                        }
                    }
                    case "member-role-set" -> {
                        if (!clan.canManageRoles(player.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas gérer les rôles.");
                            return;
                        }
                        String memberId = meta.getPersistentDataContainer().get(clanMenuMemberKey, PersistentDataType.STRING);
                        String roleId = meta.getPersistentDataContainer().get(clanMenuRoleKey, PersistentDataType.STRING);
                        if (memberId == null || roleId == null) {
                            player.sendMessage(ChatColor.RED + "Sélection invalide.");
                            return;
                        }
                        try {
                            UUID target = UUID.fromString(memberId);
                            if (!clan.isMember(target)) {
                                player.sendMessage(ChatColor.RED + "Ce joueur n'est pas dans le clan.");
                                return;
                            }
                            if (clan.isLeader(target)) {
                                player.sendMessage(ChatColor.RED + "Le leader possède déjà tous les droits.");
                                return;
                            }
                            ClanRole newRole = clan.getRole(roleId);
                            if (newRole == null) {
                                player.sendMessage(ChatColor.RED + "Rôle introuvable.");
                                return;
                            }
                            clan.assignRole(target, roleId);
                            plugin.clans().saveAll();
                            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(target);
                            String targetName = offlineTarget.getName() != null ? offlineTarget.getName() : target.toString();
                            player.sendMessage(ChatColor.GREEN + "Rôle mis à jour pour " + ChatColor.YELLOW + targetName + ChatColor.GREEN + ".");
                            Player online = Bukkit.getPlayer(target);
                            if (online != null) {
                                online.sendMessage(ChatColor.AQUA + "Ton rôle de clan est maintenant " + newRole.getDisplayName());
                            }
                            plugin.menuUI().openMemberRoleSelect(player, clan, target);
                        } catch (IllegalArgumentException ignored) {
                            player.sendMessage(ChatColor.RED + "Membre invalide.");
                        }
                    }
                }
            }
        }
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

    private String formatResourceName(String resource) {
        if (resource == null) {
            return "";
        }
        Material mat = Material.matchMaterial(resource);
        String base = mat != null ? mat.name().toLowerCase() : resource.toLowerCase();
        String[] parts = base.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
