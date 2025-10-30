package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.ResourceFarmType;
import com.outlaw.clans.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FarmManager implements Listener {

    private final OutlawClansPlugin plugin;
    private final NamespacedKey chestClanKey;
    private final NamespacedKey chestTerrainKey;
    private final NamespacedKey chestTypeKey;
    private final Map<String, ResourceFarmType> types = new LinkedHashMap<>();
    private int payoutIntervalTicks;
    private String chestDisplayName;
    private List<String> chestLore;
    private int payoutTaskId = -1;

    public FarmManager(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.chestClanKey = new NamespacedKey(plugin, Keys.FARM_CHEST_CLAN);
        this.chestTerrainKey = new NamespacedKey(plugin, Keys.FARM_CHEST_TERRAIN);
        this.chestTypeKey = new NamespacedKey(plugin, Keys.FARM_CHEST_TYPE);
        reload();
    }

    public void reload() {
        shutdown();
        types.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("resource_farms");
        if (root == null) {
            plugin.getLogger().warning("Missing resource_farms section in config.yml");
            return;
        }

        payoutIntervalTicks = root.getInt("payout_interval_ticks", 20 * 60 * 15);
        chestDisplayName = root.getString("chest_item.name", ChatColor.GOLD + "Coffre de récolte");
        chestLore = root.getStringList("chest_item.lore");

        ConfigurationSection typesSection = root.getConfigurationSection("types");
        if (typesSection != null) {
            for (String id : typesSection.getKeys(false)) {
                ConfigurationSection typeSection = typesSection.getConfigurationSection(id);
                if (typeSection == null) {
                    continue;
                }

                String displayName = ChatColor.translateAlternateColorCodes('&', typeSection.getString("name", id));
                String iconName = typeSection.getString("icon", "CHEST");
                Material icon = Material.matchMaterial(iconName == null ? "CHEST" : iconName.toUpperCase());
                if (icon == null) {
                    plugin.getLogger().warning("Invalid icon material for farm type " + id + ": " + iconName);
                    icon = Material.CHEST;
                }

                List<String> description = new ArrayList<>();
                for (String line : typeSection.getStringList("description")) {
                    description.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                List<String> schematics = typeSection.getStringList("schematics");
                LinkedHashMap<Material, Integer> outputs = new LinkedHashMap<>();
                ConfigurationSection outputSection = typeSection.getConfigurationSection("outputs");
                if (outputSection != null) {
                    for (String materialKey : outputSection.getKeys(false)) {
                        Material outputMaterial = Material.matchMaterial(materialKey.toUpperCase());
                        if (outputMaterial == null) {
                            plugin.getLogger().warning("Invalid output material for farm type " + id + ": " + materialKey);
                            continue;
                        }
                        int amount = Math.max(1, outputSection.getInt(materialKey, 1));
                        outputs.put(outputMaterial, amount);
                    }
                }

                if (outputs.isEmpty()) {
                    plugin.getLogger().warning("Farm type " + id + " has no outputs defined; skipping");
                    continue;
                }

                ResourceFarmType type = new ResourceFarmType(id, displayName, icon, description, schematics, outputs);
                types.put(id, type);
            }
        }

        restartTask();
    }

    private void restartTask() {
        if (payoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(payoutTaskId);
            payoutTaskId = -1;
        }
        if (payoutIntervalTicks > 0) {
            payoutTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::deliverResources, payoutIntervalTicks, payoutIntervalTicks);
        }
    }

    public void shutdown() {
        if (payoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(payoutTaskId);
            payoutTaskId = -1;
        }
    }

    public Map<String, ResourceFarmType> getTypes() {
        return types;
    }

    public Optional<ResourceFarmType> getType(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(types.get(id));
    }

    public void giveFarmChest(Player player, Clan clan, int terrainIndex, ResourceFarmType type) {
        if (clan == null || type == null) {
            return;
        }

        ItemStack chest = createChestItem(clan, terrainIndex, type);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(chest);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(ChatColor.GREEN + "Tu as reçu le coffre de récolte pour " + type.getDisplayName() + ".");
    }

    public void buildStructure(Player player, Clan clan, int terrainIndex, ResourceFarmType type, String schematic) {
        if (player == null || clan == null || type == null || schematic == null) {
            return;
        }
        if (!clan.hasTerritory()) {
            player.sendMessage(ChatColor.RED + "Ton clan n'a pas de territoire assigné.");
            return;
        }
        if (terrainIndex < 0 || terrainIndex >= clan.getSpots().size()) {
            player.sendMessage(ChatColor.RED + "Terrain invalide.");
            return;
        }

        BuildingSpot spot = clan.getSpots().get(terrainIndex);

        int size = plugin.getConfig().getInt("building.plot_size", 35);
        int thickness = plugin.getConfig().getInt("terraform.thickness", 10);
        int clear = plugin.getConfig().getInt("terraform.clear_above", 24);
        int perTick = plugin.getConfig().getInt("terraform.blocks_per_tick", 1500);
        String foundationName = plugin.getConfig().getString("terraform.material", "DIRT");
        Material foundation = Material.matchMaterial(foundationName == null ? "DIRT" : foundationName.toUpperCase());
        if (foundation == null) {
            foundation = Material.DIRT;
        }

        plugin.terraform().clearPlotEntities(spot.getBaseLocation(), size, thickness, clear);
        plugin.terraform().buildPlatform(spot.getBaseLocation(), size, thickness, foundation);

        int wait = plugin.terraform().estimateTicksForPlatform(size, thickness, clear, perTick);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File dir = new File(Bukkit.getPluginsFolder(), "WorldEdit/schematics");
            File file = new File(dir, schematic);
            if (!file.exists()) {
                player.sendMessage(ChatColor.RED + "Schematic introuvable: " + schematic);
                return;
            }
            int rotation = Math.floorMod(spot.getRotationTurns(), 4);
            if (!plugin.schematics().paste(file, spot.getBaseLocation(), rotation)) {
                player.sendMessage(ChatColor.RED + "Erreur WorldEdit pendant le collage.");
                return;
            }

            spot.setFarmTypeId(type.getId());
            spot.setSchematicName(schematic);
            spot.setFarmChestLocation(null);
            Material defaultOutput = type.getDefaultOutput();
            spot.setResourcePreference(defaultOutput != null ? defaultOutput.name() : null);
            plugin.clans().saveAll();

            player.sendMessage(ChatColor.GREEN + "" + type.getDisplayName() + " construit sur Terrain #" + (terrainIndex + 1) + ".");
            player.sendMessage(ChatColor.GRAY + "Place le coffre de récolte dans ton territoire pour recevoir les ressources.");
            giveFarmChest(player, clan, terrainIndex, type);
        }, wait);
    }

    private void deliverResources() {
        for (Clan clan : plugin.clans().allClans()) {
            if (clan == null) {
                continue;
            }
            for (int i = 0; i < clan.getSpots().size(); i++) {
                BuildingSpot spot = clan.getSpots().get(i);
                if (spot.getFarmTypeId() == null) {
                    continue;
                }
                Optional<ResourceFarmType> optType = getType(spot.getFarmTypeId());
                if (optType.isEmpty()) {
                    continue;
                }
                ResourceFarmType type = optType.get();
                Material chosen = null;
                if (spot.getResourcePreference() != null) {
                    chosen = Material.matchMaterial(spot.getResourcePreference());
                }
                if (chosen == null) {
                    chosen = type.getDefaultOutput();
                }
                if (chosen == null) {
                    continue;
                }
                Integer amount = type.getAmountFor(chosen);
                if (amount == null || amount <= 0) {
                    continue;
                }
                Location chestLoc = spot.getFarmChestLocation();
                if (chestLoc == null) {
                    continue;
                }
                Block block = chestLoc.getBlock();
                BlockState state = block.getState();
                if (!(state instanceof Container container)) {
                    continue;
                }
                ItemStack toAdd = new ItemStack(chosen, amount);
                Inventory inv = container.getInventory();
                Map<Integer, ItemStack> leftover = inv.addItem(toAdd);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(item -> chestLoc.getWorld().dropItemNaturally(chestLoc, item));
                }
            }
        }
    }

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        ItemStack item = event.getItemInHand();
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        var container = meta.getPersistentDataContainer();
        if (!container.has(chestClanKey, PersistentDataType.STRING)) {
            return;
        }

        String clanIdStr = container.get(chestClanKey, PersistentDataType.STRING);
        Integer terrainIndex = container.get(chestTerrainKey, PersistentDataType.INTEGER);
        String typeId = container.get(chestTypeKey, PersistentDataType.STRING);
        if (clanIdStr == null || terrainIndex == null || typeId == null) {
            return;
        }

        UUID clanId = UUID.fromString(clanIdStr);
        Optional<Clan> optClan = plugin.clans().getClan(clanId);
        if (optClan.isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Clan introuvable pour ce coffre.");
            return;
        }
        Clan clan = optClan.get();

        Optional<ResourceFarmType> optType = getType(typeId);
        if (optType.isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Type de ferme inconnu.");
            return;
        }

        if (!clan.isMember(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Ce coffre appartient à un autre clan.");
            return;
        }

        if (!clan.canManageTerrains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Ton rôle ne peut pas lier ce coffre au terrain.");
            return;
        }

        Location placeLoc = event.getBlockPlaced().getLocation().add(0.5, 0, 0.5);
        if (!clan.isInside(placeLoc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Le coffre doit être placé dans ton territoire.");
            return;
        }

        if (terrainIndex < 0 || terrainIndex >= clan.getSpots().size()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Terrain invalide pour ce coffre.");
            return;
        }

        BuildingSpot spot = clan.getSpots().get(terrainIndex);
        spot.setFarmChestLocation(placeLoc);
        plugin.clans().saveAll();
        event.getPlayer().sendMessage(ChatColor.GREEN + "Coffre de " + optType.get().getDisplayName() + " lié au terrain.");
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Location loc = event.getBlock().getLocation();
        for (Clan clan : plugin.clans().allClans()) {
            for (int i = 0; i < clan.getSpots().size(); i++) {
                BuildingSpot spot = clan.getSpots().get(i);
                Location chest = spot.getFarmChestLocation();
                if (chest == null || chest.getWorld() == null) {
                    continue;
                }
                if (!chest.getWorld().equals(loc.getWorld())) {
                    continue;
                }
                if (chest.getBlockX() == loc.getBlockX() && chest.getBlockY() == loc.getBlockY() && chest.getBlockZ() == loc.getBlockZ()) {
                    if (!clan.isMember(event.getPlayer().getUniqueId())) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Ce coffre appartient à " + clan.getName() + ".");
                        return;
                    }
                    if (!clan.canManageTerrains(event.getPlayer().getUniqueId())) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Ton rôle ne peut pas retirer ce coffre.");
                        return;
                    }
                    event.setDropItems(false);
                    ResourceFarmType type = getType(spot.getFarmTypeId()).orElse(null);
                    Location dropLoc = loc.clone().add(0.5, 0.5, 0.5);
                    if (type != null) {
                        ItemStack chestItem = createChestItem(clan, i, type);
                        dropLoc.getWorld().dropItemNaturally(dropLoc, chestItem);
                    }
                    spot.setFarmChestLocation(null);
                    plugin.clans().saveAll();
                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Coffre dissocié du terrain. Replace-le pour continuer la production.");
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        Location loc = clicked.getLocation();
        Clan targetClan = null;
        for (Clan clan : plugin.clans().allClans()) {
            for (BuildingSpot spot : clan.getSpots()) {
                Location chest = spot.getFarmChestLocation();
                if (chest == null || chest.getWorld() == null) {
                    continue;
                }
                if (!chest.getWorld().equals(loc.getWorld())) {
                    continue;
                }
                if (chest.getBlockX() == loc.getBlockX() && chest.getBlockY() == loc.getBlockY() && chest.getBlockZ() == loc.getBlockZ()) {
                    targetClan = clan;
                    break;
                }
            }
            if (targetClan != null) {
                break;
            }
        }

        if (targetClan == null) {
            return;
        }

        if (!targetClan.isMember(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Ce coffre appartient à " + targetClan.getName() + ".");
            return;
        }

        if (!targetClan.canAccessFarmChests(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Ton rôle ne peut pas accéder aux coffres de ferme.");
        }
    }

    private ItemStack createChestItem(Clan clan, int terrainIndex, ResourceFarmType type) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', chestDisplayName.replace("{type}", type.getDisplayName())));
        if (chestLore != null && !chestLore.isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (String line : chestLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("{type}", type.getDisplayName())));
            }
            meta.setLore(lore);
        }
        meta.getPersistentDataContainer().set(chestClanKey, PersistentDataType.STRING, clan.getId().toString());
        meta.getPersistentDataContainer().set(chestTerrainKey, PersistentDataType.INTEGER, terrainIndex);
        meta.getPersistentDataContainer().set(chestTypeKey, PersistentDataType.STRING, type.getId());
        chest.setItemMeta(meta);
        return chest;
    }
}
