package com.outlaw.clans.ui;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.ResourceFarmType;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClanMenuUI {

    private static final String ACTION_HOME = "home";
    private static final String ACTION_MEMBERS = "members";
    private static final String ACTION_TERRAINS = "terrains";
    private static final String ACTION_TERRAIN_SETTINGS = "terrain-settings";
    private static final String ACTION_TERRAIN_BUILDINGS = "terrain-buildings";
    private static final String ACTION_TERRAIN_RESOURCES = "terrain-resources";
    private static final String ACTION_TERRAIN_SELECT_TYPE = "terrain-select-type";
    private static final String ACTION_TERRAIN_SELECT_SCHEMATIC = "terrain-select-schematic";
    private static final String ACTION_TERRAIN_SET_RESOURCE = "terrain-set-resource";
    private static final String ACTION_CLOSE = "close";

    private final OutlawClansPlugin plugin;
    private final NamespacedKey plotKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey resourceKey;
    private final NamespacedKey schematicKey;
    private final ItemStack filler;

    public ClanMenuUI(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.plotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
        this.actionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ACTION);
        this.typeKey = new NamespacedKey(plugin, Keys.CLAN_MENU_TYPE);
        this.resourceKey = new NamespacedKey(plugin, Keys.CLAN_MENU_RESOURCE);
        this.schematicKey = new NamespacedKey(plugin, Keys.CLAN_MENU_SCHEMATIC);
        this.filler = createFiller();
    }

    public void openFor(Player player, Clan clan) {
        openHome(player, clan);
    }

    public void openHome(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Accueil");

        inv.setItem(20, actionItem(Material.BOOK, ChatColor.AQUA + "Membres du clan",
                lore(ChatColor.GRAY + "Voir tous les membres et leur rôle."), ACTION_MEMBERS));

        inv.setItem(24, actionItem(Material.CRAFTING_TABLE, ChatColor.GOLD + "Gestion des terrains",
                lore(ChatColor.GRAY + "Choisir les bâtiments, coffres et ressources."), ACTION_TERRAINS));

        inv.setItem(22, territoryInfoItem(clan));
        inv.setItem(31, centerInfoItem());

        player.openInventory(inv);
    }

    public void openMembers(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Membres");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        List<UUID> members = new ArrayList<>(clan.getMembers());
        members.sort(new MemberComparator(clan));

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        if (members.isEmpty()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Aucun membre",
                    lore(ChatColor.GRAY + "Invite des joueurs pour agrandir ton clan.")));
        } else {
            for (int i = 0; i < members.size() && i < slots.length; i++) {
                inv.setItem(slots[i], memberItem(members.get(i), clan));
            }
        }

        player.openInventory(inv);
    }

    public void openTerrains(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Terrains");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        if (clan.getSpots().isEmpty()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Aucun terrain",
                    lore(ChatColor.GRAY + "Contacte un administrateur pour en obtenir.")));
        } else {
            int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
            for (int i = 0; i < clan.getSpots().size() && i < slots.length; i++) {
                inv.setItem(slots[i], terrainItem(clan.getSpots().get(i), i));
            }
        }

        player.openInventory(inv);
    }

    public void openTerrainSettings(Player player, Clan clan, int terrainIndex) {
        Inventory inv = baseInventory(player, clan, "Terrain #" + (terrainIndex + 1));
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir à la liste des terrains."), ACTION_TERRAINS));

        if (terrainIndex < 0 || terrainIndex >= clan.getSpots().size()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Terrain invalide",
                    lore(ChatColor.GRAY + "Merci de réessayer.")));
            player.openInventory(inv);
            return;
        }

        BuildingSpot spot = clan.getSpots().get(terrainIndex);
        Optional<ResourceFarmType> optType = plugin.farms().getType(spot.getFarmTypeId());
        ResourceFarmType type = optType.orElse(null);

        ItemStack buildingButton = actionItem(type != null ? type.getIcon() : Material.CRAFTING_TABLE,
                ChatColor.GOLD + "Bâtiment",
                type != null
                        ? lore(ChatColor.GRAY + "Actuel: " + ChatColor.GOLD + type.getDisplayName(),
                        ChatColor.YELLOW + "Clique pour changer.")
                        : lore(ChatColor.GRAY + "Aucun bâtiment sélectionné.",
                        ChatColor.YELLOW + "Clique pour choisir."),
                ACTION_TERRAIN_BUILDINGS);
        withPlotIndex(buildingButton, terrainIndex);
        inv.setItem(20, buildingButton);

        if (type != null) {
            ItemStack resourceButton = actionItem(Material.HOPPER, ChatColor.AQUA + "Préférence de ressource",
                    lore(ChatColor.GRAY + "Choisir la ressource générée."), ACTION_TERRAIN_RESOURCES);
            withPlotIndex(resourceButton, terrainIndex);
            inv.setItem(24, resourceButton);
        } else {
            inv.setItem(24, infoItem(Material.BARRIER, ChatColor.RED + "Sélectionne un bâtiment",
                    lore(ChatColor.GRAY + "Choisis un bâtiment avant de configurer les ressources.")));
        }

        inv.setItem(22, terrainSummaryItem(spot, type));
        inv.setItem(31, chestInfoItem(spot));

        player.openInventory(inv);
    }

    public void openBuildingTypes(Player player, Clan clan, int terrainIndex) {
        Inventory inv = baseInventory(player, clan, "Type de bâtiment");
        ItemStack back = actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir aux réglages du terrain."), ACTION_TERRAIN_SETTINGS);
        withPlotIndex(back, terrainIndex);
        inv.setItem(45, back);

        int slot = 19;
        Map<String, ResourceFarmType> types = plugin.farms().getTypes();
        BuildingSpot spot = terrainIndex >= 0 && terrainIndex < clan.getSpots().size() ? clan.getSpots().get(terrainIndex) : null;
        for (ResourceFarmType type : types.values()) {
            ItemStack item = new ItemStack(type.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + type.getDisplayName());
            List<String> lore = new ArrayList<>();
            if (!type.getDescription().isEmpty()) {
                lore.addAll(type.getDescription());
            }
            if (!type.getOutputs().isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "---");
                lore.add(ChatColor.GRAY + "Ressources possibles:");
                for (Map.Entry<Material, Integer> entry : type.getOutputs().entrySet()) {
                    lore.add(ChatColor.GRAY + " - " + ChatColor.YELLOW + prettyMaterial(entry.getKey()) + ChatColor.GRAY + " x" + entry.getValue());
                }
            }
            if (spot != null && type.getId().equals(spot.getFarmTypeId())) {
                lore.add(ChatColor.GREEN + "Actuellement sélectionné");
            } else {
                lore.add(ChatColor.YELLOW + "Clique pour sélectionner");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TERRAIN_SELECT_TYPE);
            meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, terrainIndex);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.getId());
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        player.openInventory(inv);
    }

    public void openBuildingSchematics(Player player, Clan clan, int terrainIndex, ResourceFarmType type) {
        Inventory inv = baseInventory(player, clan, "Plans " + type.getDisplayName());
        ItemStack back = actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au choix du bâtiment."), ACTION_TERRAIN_BUILDINGS);
        withPlotIndex(back, terrainIndex);
        inv.setItem(45, back);

        List<String> schematics = type.getSchematics();
        if (schematics.isEmpty()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Aucun schematic",
                    lore(ChatColor.GRAY + "Configure des plans dans le config.")));
        } else {
            int slot = 19;
            for (String name : schematics) {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + name);
                meta.setLore(lore(ChatColor.GRAY + "Clique pour construire."));
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TERRAIN_SELECT_SCHEMATIC);
                meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, terrainIndex);
                meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.getId());
                meta.getPersistentDataContainer().set(schematicKey, PersistentDataType.STRING, name);
                item.setItemMeta(meta);
                inv.setItem(slot++, item);
                if (slot % 9 == 8) {
                    slot += 2;
                }
            }
        }

        player.openInventory(inv);
    }

    public void openResourcePreferences(Player player, Clan clan, int terrainIndex) {
        Inventory inv = baseInventory(player, clan, "Ressources");
        ItemStack back = actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir aux réglages du terrain."), ACTION_TERRAIN_SETTINGS);
        withPlotIndex(back, terrainIndex);
        inv.setItem(45, back);

        if (terrainIndex < 0 || terrainIndex >= clan.getSpots().size()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Terrain invalide",
                    lore(ChatColor.GRAY + "Merci de réessayer.")));
            player.openInventory(inv);
            return;
        }

        BuildingSpot spot = clan.getSpots().get(terrainIndex);
        Optional<ResourceFarmType> optType = plugin.farms().getType(spot.getFarmTypeId());
        if (optType.isEmpty()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Aucun bâtiment",
                    lore(ChatColor.GRAY + "Sélectionne un bâtiment avant les ressources.")));
            player.openInventory(inv);
            return;
        }

        ResourceFarmType type = optType.get();
        String current = spot.getResourcePreference();
        int slot = 19;
        for (Map.Entry<Material, Integer> entry : type.getOutputs().entrySet()) {
            Material material = entry.getKey();
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + prettyMaterial(material));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Quantité: " + ChatColor.YELLOW + entry.getValue());
            if (material.name().equalsIgnoreCase(current)) {
                lore.add(ChatColor.GREEN + "Actuellement sélectionné");
            } else {
                lore.add(ChatColor.YELLOW + "Clique pour sélectionner");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TERRAIN_SET_RESOURCE);
            meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, terrainIndex);
            meta.getPersistentDataContainer().set(resourceKey, PersistentDataType.STRING, material.name());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        player.openInventory(inv);
    }

    private Inventory baseInventory(Player player, Clan clan, String subtitle) {
        String title = ChatColor.DARK_PURPLE + "Clan: " + clan.getName();
        if (subtitle != null && !subtitle.isEmpty()) {
            title += ChatColor.DARK_GRAY + " » " + ChatColor.GRAY + subtitle;
        }
        Inventory inv = Bukkit.createInventory(player, 54, title);
        decorate(inv);
        inv.setItem(49, actionItem(Material.BARRIER, ChatColor.RED + "Fermer",
                lore(ChatColor.GRAY + "Fermer le menu."), ACTION_CLOSE));
        return inv;
    }

    private void decorate(Inventory inv) {
        for (int slot = 0; slot < inv.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                inv.setItem(slot, filler.clone());
            }
        }
    }

    private ItemStack memberItem(UUID uuid, Clan clan) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = head.getItemMeta();
        if (baseMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(offlinePlayer);
            baseMeta = skullMeta;
        }

        baseMeta.setDisplayName(ChatColor.AQUA + displayName(offlinePlayer, uuid));
        List<String> lore = lore(ChatColor.GRAY + (clan.getLeader().equals(uuid) ? "Leader" : "Membre"));
        baseMeta.setLore(lore);
        head.setItemMeta(baseMeta);
        return head;
    }

    private ItemStack terrainItem(BuildingSpot spot, int index) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Terrain #" + (index + 1));

        List<String> lore = new ArrayList<>();
        Optional<ResourceFarmType> optType = plugin.farms().getType(spot.getFarmTypeId());
        if (optType.isPresent()) {
            ResourceFarmType type = optType.get();
            lore.add(ChatColor.GOLD + "Bâtiment: " + ChatColor.YELLOW + type.getDisplayName());
        } else {
            lore.add(ChatColor.RED + "Bâtiment: Aucun");
        }

        if (spot.getResourcePreference() != null && optType.isPresent()) {
            Material pref = Material.matchMaterial(spot.getResourcePreference());
            lore.add(ChatColor.GOLD + "Ressource: " + ChatColor.YELLOW + (pref != null ? prettyMaterial(pref) : spot.getResourcePreference()));
        } else {
            lore.add(ChatColor.GOLD + "Ressource: " + ChatColor.YELLOW + "Défaut");
        }

        lore.add(spot.getFarmChestLocation() != null
                ? ChatColor.GREEN + "Coffre: placé"
                : ChatColor.YELLOW + "Coffre: à placer");
        lore.add(ChatColor.DARK_GRAY + "---");
        lore.add(ChatColor.YELLOW + "Clique pour gérer le terrain");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TERRAIN_SETTINGS);
        meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack terrainSummaryItem(BuildingSpot spot, ResourceFarmType type) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Résumé du terrain");
        List<String> lore = new ArrayList<>();
        if (type != null) {
            lore.add(ChatColor.GRAY + "Bâtiment: " + ChatColor.YELLOW + type.getDisplayName());
            if (spot.getSchematicName() != null) {
                lore.add(ChatColor.GRAY + "Plan: " + ChatColor.YELLOW + spot.getSchematicName());
            }
        } else {
            lore.add(ChatColor.GRAY + "Bâtiment: " + ChatColor.RED + "Aucun");
        }

        if (spot.getResourcePreference() != null && type != null) {
            Material mat = Material.matchMaterial(spot.getResourcePreference());
            lore.add(ChatColor.GRAY + "Ressource: " + ChatColor.YELLOW + (mat != null ? prettyMaterial(mat) : spot.getResourcePreference()));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack chestInfoItem(BuildingSpot spot) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Coffre de récolte");
        List<String> lore = new ArrayList<>();
        if (spot.getFarmChestLocation() != null) {
            lore.add(ChatColor.GRAY + "Position: " + formatLocation(spot.getFarmChestLocation()));
            lore.add(ChatColor.GREEN + "Prêt à collecter les ressources.");
        } else {
            lore.add(ChatColor.YELLOW + "Place le coffre reçu après la construction.");
            lore.add(ChatColor.GRAY + "Il doit être dans ton territoire.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack territoryInfoItem(Clan clan) {
        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Territoire");
        List<String> lore = new ArrayList<>();
        Territory territory = clan.getTerritory();
        if (territory == null) {
            lore.add(ChatColor.RED + "Non défini");
        } else {
            lore.add(ChatColor.GRAY + "Monde: " + territory.getWorldName());
            lore.add(ChatColor.GRAY + "Centre: " + territory.getCenterX() + ", " + territory.getCenterY() + ", " + territory.getCenterZ());
            lore.add(ChatColor.GRAY + "Rayon: " + territory.getRadius());
        }
        meta.setLore(lore);
        info.setItemMeta(meta);
        return info;
    }

    private ItemStack centerInfoItem() {
        ItemStack center = new ItemStack(Material.LECTERN);
        ItemMeta meta = center.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Centre du Clan");
        meta.setLore(lore(ChatColor.GRAY + "Utilise les pupitres pour ouvrir ce menu.",
                ChatColor.GRAY + "Les pancartes des terrains remplacent les NPC."));
        center.setItemMeta(meta);
        return center;
    }

    private ItemStack actionItem(Material material, String displayName, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void withPlotIndex(ItemStack item, int plotIndex) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, plotIndex);
        item.setItemMeta(meta);
    }

    private String formatLocation(Location loc) {
        if (loc == null) {
            return "?";
        }
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
        return ChatColor.YELLOW + world + ChatColor.GRAY + " @ " + ChatColor.YELLOW + loc.getBlockX() + ChatColor.GRAY + ", "
                + ChatColor.YELLOW + loc.getBlockY() + ChatColor.GRAY + ", " + ChatColor.YELLOW + loc.getBlockZ();
    }

    private String prettyMaterial(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private List<String> lore(String... lines) {
        List<String> lore = new ArrayList<>();
        Collections.addAll(lore, lines);
        return lore;
    }

    private String displayName(OfflinePlayer player, UUID uuid) {
        String name = player.getName();
        return name == null ? uuid.toString() : name;
    }

    private static class MemberComparator implements Comparator<UUID> {
        private final UUID leaderId;

        private MemberComparator(Clan clan) {
            this.leaderId = clan.getLeader();
        }

        @Override
        public int compare(UUID first, UUID second) {
            if (first.equals(second)) {
                return 0;
            }
            if (first.equals(leaderId)) {
                return -1;
            }
            if (second.equals(leaderId)) {
                return 1;
            }
            OfflinePlayer firstPlayer = Bukkit.getOfflinePlayer(first);
            OfflinePlayer secondPlayer = Bukkit.getOfflinePlayer(second);
            String firstName = firstPlayer.getName();
            String secondName = secondPlayer.getName();
            if (firstName == null && secondName == null) {
                return first.toString().compareTo(second.toString());
            }
            if (firstName == null) {
                return 1;
            }
            if (secondName == null) {
                return -1;
            }
            return firstName.compareToIgnoreCase(secondName);
        }
    }
}
