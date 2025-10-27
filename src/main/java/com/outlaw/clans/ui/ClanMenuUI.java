package com.outlaw.clans.ui;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.BuildingSpot;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.ClanRole;
import com.outlaw.clans.model.ClanRolePermission;
import com.outlaw.clans.model.ResourceFarmType;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.util.Keys;
import com.outlaw.clans.util.TerrainFenceBuilder;
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
    private static final String ACTION_ROLES = "roles";
    private static final String ACTION_ROLE_SETTINGS = "role-settings";
    private static final String ACTION_ROLE_TOGGLE = "role-toggle";
    private static final String ACTION_MEMBER_ASSIGN = "member-assign";
    private static final String ACTION_MEMBER_ROLE_SET = "member-role-set";
    private static final String ACTION_BANK = "bank";
    private static final String ACTION_BANK_PICK = "bank-pick";
    private static final String ACTION_BANK_AMOUNT = "bank-amount";
    private static final String ACTION_BANK_GIVE_MEMBER = "bank-give-member";
    private static final String ACTION_TERRITORY_UPGRADE = "territory-upgrade";
    private static final String ACTION_CLOSE = "close";

    private final OutlawClansPlugin plugin;
    private final NamespacedKey plotKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey resourceKey;
    private final NamespacedKey schematicKey;
    private final NamespacedKey roleKey;
    private final NamespacedKey permissionKey;
    private final NamespacedKey memberKey;
    private final NamespacedKey bankActionKey;
    private final NamespacedKey bankAmountKey;
    private final ItemStack filler;

    public ClanMenuUI(OutlawClansPlugin plugin) {
        this.plotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
        this.actionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ACTION);
        this.typeKey = new NamespacedKey(plugin, Keys.CLAN_MENU_TYPE);
        this.resourceKey = new NamespacedKey(plugin, Keys.CLAN_MENU_RESOURCE);
        this.schematicKey = new NamespacedKey(plugin, Keys.CLAN_MENU_SCHEMATIC);
        this.roleKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ROLE);
        this.permissionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PERMISSION);
        this.memberKey = new NamespacedKey(plugin, Keys.CLAN_MENU_MEMBER);
        this.bankActionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_BANK_ACTION);
        this.bankAmountKey = new NamespacedKey(plugin, Keys.CLAN_MENU_BANK_AMOUNT);
        this.filler = createFiller();
    }

    public enum BankAction {
        DEPOSIT,
        WITHDRAW,
        GIVE;

        static BankAction fromKey(String key) {
            if (key == null) {
                return null;
            }
            try {
                return BankAction.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
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

        inv.setItem(22, territoryInfoItem(player, clan));
        inv.setItem(29, bankSummaryItem(player, clan));
        inv.setItem(31, centerInfoItem());

        if (clan.canManageRoles(player.getUniqueId())) {
            inv.setItem(26, actionItem(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Gestion des rôles",
                    lore(ChatColor.GRAY + "Configurer les permissions des rôles."), ACTION_ROLES));
        }

        player.openInventory(inv);
    }

    public void openMembers(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Membres");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        List<UUID> members = new ArrayList<>(clan.getMembers());
        members.sort(new MemberComparator(clan));

        boolean canManageRoles = clan.canManageRoles(player.getUniqueId());

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
                inv.setItem(slots[i], memberItem(members.get(i), clan, canManageRoles));
            }
        }

        player.openInventory(inv);
    }

    public void openBank(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Banque");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        inv.setItem(22, infoItem(Material.BARREL, ChatColor.GOLD + "Solde actuel",
                lore(ChatColor.GRAY + "Clan: " + ChatColor.YELLOW + plugin.economy().formatAmount(clan.getCurrencyBalance()),
                        ChatColor.GRAY + "Toi: " + ChatColor.YELLOW + plugin.economy().formatAmount(plugin.economy().getPlayerCurrency(player)))));

        inv.setItem(30, bankActionItem(Material.HOPPER, ChatColor.GREEN + "Déposer",
                lore(ChatColor.GRAY + "Transfère ta monnaie vers la banque."), BankAction.DEPOSIT));

        if (clan.canManageTreasury(player.getUniqueId())) {
            inv.setItem(32, bankActionItem(Material.ENDER_CHEST, ChatColor.AQUA + "Retirer",
                    lore(ChatColor.GRAY + "Récupère de la monnaie depuis la banque."), BankAction.WITHDRAW));
            inv.setItem(24, bankActionItem(Material.PLAYER_HEAD, ChatColor.LIGHT_PURPLE + "Donner à un membre",
                    lore(ChatColor.GRAY + "Envoie directement la monnaie à un membre."), BankAction.GIVE));
        } else {
            inv.setItem(32, infoItem(Material.BARRIER, ChatColor.RED + "Accès limité",
                    lore(ChatColor.GRAY + "Ton rôle ne peut pas retirer de la banque.")));
            inv.setItem(24, infoItem(Material.BARRIER, ChatColor.RED + "Accès limité",
                    lore(ChatColor.GRAY + "Ton rôle ne peut pas distribuer la banque.")));
        }

        player.openInventory(inv);
    }

    public void openBankAmounts(Player player, Clan clan, BankAction action) {
        String title = switch (action) {
            case DEPOSIT -> "Déposer";
            case WITHDRAW -> "Retirer";
            case GIVE -> "Donner";
        };
        Inventory inv = baseInventory(player, clan, title);
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu de la banque."), ACTION_BANK));

        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        List<Integer> amounts = plugin.economy().amountButtons();
        Material icon = plugin.economy().displayMaterial();
        for (int i = 0; i < amounts.size() && i < slots.length; i++) {
            int amount = amounts.get(i);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + plugin.economy().formatAmount(amount));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clique pour sélectionner ce montant.");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BANK_AMOUNT);
            meta.getPersistentDataContainer().set(bankActionKey, PersistentDataType.STRING, action.name());
            meta.getPersistentDataContainer().set(bankAmountKey, PersistentDataType.INTEGER, amount);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        player.openInventory(inv);
    }

    public void openBankMemberSelect(Player player, Clan clan, int amount) {
        Inventory inv = baseInventory(player, clan, "Choisir un membre");
        inv.setItem(45, bankAmountBackItem());

        ItemStack info = infoItem(Material.PAPER, ChatColor.GOLD + "Montant",
                lore(ChatColor.GRAY + "Envoyer: " + ChatColor.YELLOW + plugin.economy().formatAmount(amount)));
        inv.setItem(22, info);

        List<UUID> members = new ArrayList<>(clan.getMembers());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < members.size() && i < slots.length; i++) {
            UUID targetId = members.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(offline);
                meta = skull;
            }
            meta.setDisplayName(ChatColor.AQUA + displayName(offline, targetId));
            List<String> lore = new ArrayList<>();
            if (Bukkit.getPlayer(targetId) == null) {
                lore.add(ChatColor.RED + "Hors ligne - impossible de recevoir la monnaie");
            } else {
                lore.add(ChatColor.YELLOW + "Clique pour envoyer la monnaie");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BANK_GIVE_MEMBER);
            meta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, targetId.toString());
            meta.getPersistentDataContainer().set(bankAmountKey, PersistentDataType.INTEGER, amount);
            head.setItemMeta(meta);
            inv.setItem(slots[i], head);
        }

        player.openInventory(inv);
    }

    private ItemStack bankActionItem(Material icon, String name, List<String> lore, BankAction action) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BANK_PICK);
        meta.getPersistentDataContainer().set(bankActionKey, PersistentDataType.STRING, action.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack bankAmountBackItem() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Retour");
        meta.setLore(lore(ChatColor.GRAY + "Revenir au choix des montants."));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BANK_PICK);
        meta.getPersistentDataContainer().set(bankActionKey, PersistentDataType.STRING, BankAction.GIVE.name());
        back.setItemMeta(meta);
        return back;
    }

    public void handleBankAmount(Player player, Clan clan, BankAction action, int amount) {
        if (action == null || amount <= 0) {
            player.sendMessage(ChatColor.RED + "Sélection invalide.");
            openBank(player, clan);
            return;
        }

        switch (action) {
            case DEPOSIT -> {
                if (!plugin.economy().withdraw(player, amount)) {
                    player.sendMessage(ChatColor.RED + "Tu n'as pas assez de monnaie sur toi.");
                } else {
                    clan.depositCurrency(amount);
                    plugin.clans().saveAll();
                    player.sendMessage(ChatColor.GREEN + "Déposé " + ChatColor.YELLOW + plugin.economy().formatAmount(amount) + ChatColor.GREEN + " dans la banque du clan.");
                }
                openBank(player, clan);
            }
            case WITHDRAW -> {
                if (!clan.canManageTreasury(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas retirer de la banque.");
                    openBank(player, clan);
                    return;
                }
                if (!clan.withdrawCurrency(amount)) {
                    player.sendMessage(ChatColor.RED + "La banque n'a pas assez de monnaie.");
                } else {
                    plugin.economy().deposit(player, amount);
                    plugin.clans().saveAll();
                    player.sendMessage(ChatColor.GREEN + "Retiré " + ChatColor.YELLOW + plugin.economy().formatAmount(amount) + ChatColor.GREEN + " de la banque.");
                }
                openBank(player, clan);
            }
            case GIVE -> openBankMemberSelect(player, clan, amount);
        }
    }

    public void handleBankTransfer(Player player, Clan clan, int amount, UUID targetId) {
        if (!clan.canManageTreasury(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas distribuer la banque.");
            openBank(player, clan);
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Montant invalide.");
            openBank(player, clan);
            return;
        }
        if (!clan.isMember(targetId)) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'est pas dans ton clan.");
            openBank(player, clan);
            return;
        }
        if (!clan.withdrawCurrency(amount)) {
            player.sendMessage(ChatColor.RED + "La banque n'a pas assez de monnaie.");
            openBank(player, clan);
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            clan.depositCurrency(amount);
            plugin.clans().saveAll();
            player.sendMessage(ChatColor.RED + "Le joueur doit être en ligne pour recevoir la monnaie du clan.");
            openBankMemberSelect(player, clan, amount);
            return;
        }

        plugin.economy().deposit(target, amount);
        plugin.clans().saveAll();

        String formatted = plugin.economy().formatAmount(amount);
        player.sendMessage(ChatColor.GREEN + "Envoyé " + ChatColor.YELLOW + formatted + ChatColor.GREEN + " à " + target.getName() + ".");
        target.sendMessage(ChatColor.AQUA + "Tu as reçu " + ChatColor.YELLOW + formatted + ChatColor.AQUA + " de la banque du clan.");
        openBank(player, clan);
    }

    public void handleTerritoryUpgrade(Player player, Clan clan) {
        if (!clan.hasTerritory()) {
            player.sendMessage(ChatColor.RED + "Aucun territoire à agrandir.");
            return;
        }
        if (!clan.canManageTerrains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas modifier le territoire.");
            return;
        }
        if (!clan.canManageTreasury(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Ton rôle ne peut pas dépenser la banque du clan.");
            return;
        }

        int cost = Math.max(0, plugin.economy().costUpgrade());
        if (cost > 0 && clan.getCurrencyBalance() < cost) {
            player.sendMessage(ChatColor.RED + "La banque n'a pas assez de monnaie pour l'amélioration.");
            return;
        }

        int increase = Math.max(1, plugin.getConfig().getInt("territory.upgrade_radius_increase", 50));
        Territory current = clan.getTerritory();
        Territory upgraded = new Territory(current.getWorldName(), current.getRadius() + increase,
                current.getCenterX(), current.getCenterY(), current.getCenterZ());
        clan.setTerritory(upgraded);
        if (cost > 0) {
            clan.withdrawCurrency(cost);
        }
        plugin.clans().saveAll();

        int thickness = plugin.getConfig().getInt("terraform.thickness", 10);
        Material topMat = Material.GRASS_BLOCK;
        Material foundation = Material.DIRT;
        try {
            topMat = Material.valueOf(plugin.getConfig().getString("terraform.surface_top_material", "GRASS_BLOCK").toUpperCase());
        } catch (Exception ignored) {}
        try {
            foundation = Material.valueOf(plugin.getConfig().getString("terraform.material", "DIRT").toUpperCase());
        } catch (Exception ignored) {}

        plugin.terraform().buildTerritoryPlate(upgraded, upgraded.getCenterY(), thickness, topMat, foundation);
        if (plugin.getConfig().getBoolean("feather.enabled", true)) {
            int width = plugin.getConfig().getInt("feather.width", 20);
            Material featherTop = topMat;
            Material featherFoundation = foundation;
            try {
                featherTop = Material.valueOf(plugin.getConfig().getString("feather.top_material", featherTop.name()).toUpperCase());
            } catch (Exception ignored) {}
            try {
                featherFoundation = Material.valueOf(plugin.getConfig().getString("feather.foundation_material", featherFoundation.name()).toUpperCase());
            } catch (Exception ignored) {}
            plugin.terraform().featherTerritoryEdges(upgraded, upgraded.getCenterY(), thickness, featherTop, featherFoundation, width);
        }

        for (BuildingSpot spot : clan.getSpots()) {
            TerrainFenceBuilder.build(plugin, spot.getBaseLocation());
        }

        player.sendMessage(ChatColor.GREEN + "Territoire agrandi! Nouveau rayon: " + ChatColor.YELLOW + upgraded.getRadius());
        openHome(player, clan);
    }

    public void openRoles(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Rôles");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        if (!clan.canManageRoles(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès refusé",
                    lore(ChatColor.GRAY + "Seul un leader ou un rôle autorisé peut gérer les rôles.")));
            player.openInventory(inv);
            return;
        }

        int slot = 19;
        for (ClanRole role : clan.getRoles().values()) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(role.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Membres: " + ChatColor.YELLOW + clan.countMembersWithRole(role.getId()));
            if (role.getId().equals(clan.getDefaultRoleId())) {
                lore.add(ChatColor.AQUA + "Rôle par défaut");
            }
            lore.add(ChatColor.DARK_GRAY + "---");
            for (ClanRolePermission permission : ClanRolePermission.values()) {
                boolean enabled = role.hasPermission(permission);
                lore.add((enabled ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ") + ChatColor.GRAY + ChatColor.stripColor(permission.getDisplayName()));
            }
            lore.add(ChatColor.YELLOW + "Clique pour modifier les permissions");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_ROLE_SETTINGS);
            meta.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, role.getId());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        player.openInventory(inv);
    }

    public void openRoleSettings(Player player, Clan clan, String roleId) {
        Inventory inv = baseInventory(player, clan, "Rôle");
        ItemStack back = actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir à la liste des rôles."), ACTION_ROLES);
        inv.setItem(45, back);

        if (!clan.canManageRoles(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès refusé",
                    lore(ChatColor.GRAY + "Tu ne peux pas modifier les rôles.")));
            player.openInventory(inv);
            return;
        }

        ClanRole role = clan.getRole(roleId);
        if (role == null) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Rôle introuvable",
                    lore(ChatColor.GRAY + "Merci de réessayer.")));
            player.openInventory(inv);
            return;
        }

        List<String> headerLore = new ArrayList<>();
        headerLore.add(ChatColor.GRAY + "Membres: " + ChatColor.YELLOW + clan.countMembersWithRole(role.getId()));
        if (role.getId().equals(clan.getDefaultRoleId())) {
            headerLore.add(ChatColor.AQUA + "Rôle par défaut");
        }
        ItemStack header = infoItem(Material.NAME_TAG, role.getDisplayName(), headerLore);
        inv.setItem(22, header);

        int slot = 19;
        for (ClanRolePermission permission : ClanRolePermission.values()) {
            ItemStack item = new ItemStack(permission.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(permission.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(permission.getDescription());
            if (role.hasPermission(permission)) {
                lore.add(ChatColor.GREEN + "Actuellement activé");
            } else {
                lore.add(ChatColor.RED + "Actuellement désactivé");
            }
            lore.add(ChatColor.YELLOW + "Clique pour basculer");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_ROLE_TOGGLE);
            meta.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, role.getId());
            meta.getPersistentDataContainer().set(permissionKey, PersistentDataType.STRING, permission.name());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        player.openInventory(inv);
    }

    public void openMemberRoleSelect(Player player, Clan clan, UUID memberId) {
        Inventory inv = baseInventory(player, clan, "Attribuer un rôle");
        ItemStack back = actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir à la liste des membres."), ACTION_MEMBERS);
        inv.setItem(45, back);

        if (!clan.canManageRoles(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès refusé",
                    lore(ChatColor.GRAY + "Tu ne peux pas modifier les rôles.")));
            player.openInventory(inv);
            return;
        }

        if (memberId == null || !clan.isMember(memberId)) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Membre introuvable",
                    lore(ChatColor.GRAY + "Merci de réessayer.")));
            player.openInventory(inv);
            return;
        }

        if (clan.isLeader(memberId)) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Leader protégé",
                    lore(ChatColor.GRAY + "Le leader possède déjà tous les droits.")));
            player.openInventory(inv);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(memberId);
        String currentRole = clan.getRoleId(memberId);
        inv.setItem(22, memberItem(memberId, clan, false));

        int slot = 19;
        for (ClanRole role : clan.getRoles().values()) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(role.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Attribuer à: " + ChatColor.YELLOW + displayName(target, memberId));
            lore.add(ChatColor.DARK_GRAY + "---");
            if (role.getId().equals(currentRole)) {
                lore.add(ChatColor.GREEN + "Actuellement sélectionné");
            } else {
                lore.add(ChatColor.YELLOW + "Clique pour attribuer ce rôle");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MEMBER_ROLE_SET);
            meta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, memberId.toString());
            meta.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, role.getId());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
            if (slot % 9 == 8) {
                slot += 2;
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
        boolean canManage = clan.canManageTerrains(player.getUniqueId());

        if (canManage) {
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
        } else {
            inv.setItem(20, infoItem(Material.CRAFTING_TABLE, ChatColor.RED + "Accès restreint",
                    lore(ChatColor.GRAY + "Ton rôle ne permet pas de modifier les terrains.")));
        }

        if (type != null && canManage) {
            ItemStack resourceButton = actionItem(Material.HOPPER, ChatColor.AQUA + "Préférence de ressource",
                    lore(ChatColor.GRAY + "Choisir la ressource générée."), ACTION_TERRAIN_RESOURCES);
            withPlotIndex(resourceButton, terrainIndex);
            inv.setItem(24, resourceButton);
        } else if (!canManage) {
            inv.setItem(24, infoItem(Material.HOPPER, ChatColor.RED + "Accès restreint",
                    lore(ChatColor.GRAY + "Ton rôle ne permet pas de modifier les ressources.")));
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

        if (!clan.canManageTerrains(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès restreint",
                    lore(ChatColor.GRAY + "Ton rôle ne peut pas modifier les terrains.")));
            player.openInventory(inv);
            return;
        }

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

        if (!clan.canManageTerrains(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès restreint",
                    lore(ChatColor.GRAY + "Ton rôle ne peut pas modifier les terrains.")));
            player.openInventory(inv);
            return;
        }

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

        if (!clan.canManageTerrains(player.getUniqueId())) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Accès restreint",
                    lore(ChatColor.GRAY + "Ton rôle ne peut pas modifier les ressources.")));
            player.openInventory(inv);
            return;
        }

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

    private ItemStack memberItem(UUID uuid, Clan clan, boolean canManageRoles) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = head.getItemMeta();
        if (baseMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(offlinePlayer);
            baseMeta = skullMeta;
        }

        baseMeta.setDisplayName(ChatColor.AQUA + displayName(offlinePlayer, uuid));
        List<String> lore = new ArrayList<>();
        if (clan.getLeader().equals(uuid)) {
            lore.add(ChatColor.GOLD + "Leader du clan");
        } else {
            String roleId = clan.getRoleId(uuid);
            String roleName = roleId != null && clan.getRole(roleId) != null
                    ? clan.getRole(roleId).getDisplayName()
                    : ChatColor.GRAY + "Aucun rôle";
            lore.add(ChatColor.GRAY + "Rôle: " + ChatColor.RESET + roleName);
            if (canManageRoles) {
                lore.add(ChatColor.YELLOW + "Clique pour changer le rôle");
            }
        }
        baseMeta.setLore(lore);
        if (canManageRoles && !clan.getLeader().equals(uuid)) {
            baseMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MEMBER_ASSIGN);
            baseMeta.getPersistentDataContainer().set(memberKey, PersistentDataType.STRING, uuid.toString());
        }
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

    private ItemStack territoryInfoItem(Player player, Clan clan) {
        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Territoire");
        List<String> lore = new ArrayList<>();
        Territory territory = clan.getTerritory();
        if (territory == null) {
            lore.add(ChatColor.RED + "Non défini");
            lore.add(ChatColor.GRAY + "Achetez un terrain auprès du PNJ.");
        } else {
            lore.add(ChatColor.GRAY + "Monde: " + territory.getWorldName());
            lore.add(ChatColor.GRAY + "Centre: " + territory.getCenterX() + ", " + territory.getCenterY() + ", " + territory.getCenterZ());
            lore.add(ChatColor.GRAY + "Rayon: " + territory.getRadius());
            lore.add(ChatColor.DARK_GRAY + "---");
            lore.add(ChatColor.GRAY + "Banque: " + ChatColor.YELLOW + plugin.economy().formatAmount(clan.getCurrencyBalance()));
            int cost = plugin.economy().costUpgrade();
            int increase = Math.max(1, plugin.getConfig().getInt("territory.upgrade_radius_increase", 50));
            if (clan.canManageTerrains(player.getUniqueId())) {
                lore.add(ChatColor.GRAY + "Gain: +" + increase + " rayon");
                if (cost > 0) {
                    lore.add(ChatColor.GRAY + "Coût: " + ChatColor.YELLOW + plugin.economy().formatAmount(cost));
                }
                if (clan.canManageTreasury(player.getUniqueId())) {
                    if (clan.getCurrencyBalance() >= cost) {
                        lore.add(ChatColor.GREEN + "Clique pour agrandir le territoire.");
                    } else {
                        lore.add(ChatColor.RED + "Banque insuffisante pour l'amélioration.");
                    }
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_TERRITORY_UPGRADE);
                } else {
                    lore.add(ChatColor.RED + "Ton rôle ne peut pas dépenser la banque.");
                }
            }
        }
        meta.setLore(lore);
        info.setItemMeta(meta);
        return info;
    }

    private ItemStack bankSummaryItem(Player player, Clan clan) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Banque du clan");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Solde: " + ChatColor.YELLOW + plugin.economy().formatAmount(clan.getCurrencyBalance()));
        lore.add(ChatColor.GRAY + "Tes fonds: " + ChatColor.YELLOW + plugin.economy().formatAmount(plugin.economy().getPlayerCurrency(player)));
        lore.add(ChatColor.DARK_GRAY + "---");
        lore.add(ChatColor.YELLOW + "Clique pour gérer la banque");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_BANK);
        item.setItemMeta(meta);
        return item;
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
