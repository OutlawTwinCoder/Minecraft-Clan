package com.outlaw.clans.ui;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.model.Territory;
import com.outlaw.clans.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.UUID;

public class ClanMenuUI {

    private static final String ACTION_HOME = "home";
    private static final String ACTION_MEMBERS = "members";
    private static final String ACTION_PLOTS = "plots";
    private static final String ACTION_CLOSE = "close";

    private final NamespacedKey plotKey;
    private final NamespacedKey actionKey;
    private final ItemStack filler;

    public ClanMenuUI(OutlawClansPlugin plugin) {
        this.plotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
        this.actionKey = new NamespacedKey(plugin, Keys.CLAN_MENU_ACTION);
        this.filler = createFiller();
    }

    public void openFor(Player player, Clan clan) {
        openHome(player, clan);
    }

    public void openHome(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Accueil");

        inv.setItem(20, actionItem(Material.BOOK, ChatColor.AQUA + "Membres du clan",
                lore(ChatColor.GRAY + "Voir tous les membres et leur rôle."), ACTION_MEMBERS));

        inv.setItem(24, actionItem(Material.CRAFTING_TABLE, ChatColor.GOLD + "Shops de schématique",
                lore(ChatColor.GRAY + "Accéder aux plots et aux schématiques."), ACTION_PLOTS));

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

    public void openPlots(Player player, Clan clan) {
        Inventory inv = baseInventory(player, clan, "Schématiques");
        inv.setItem(45, actionItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                lore(ChatColor.GRAY + "Revenir au menu principal."), ACTION_HOME));

        if (clan.getSpots().isEmpty()) {
            inv.setItem(22, infoItem(Material.BARRIER, ChatColor.RED + "Aucun plot",
                    lore(ChatColor.GRAY + "Contacte un administrateur pour en obtenir.")));
        } else {
            int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
            for (int i = 0; i < clan.getSpots().size() && i < slots.length; i++) {
                inv.setItem(slots[i], plotItem(i));
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

    private ItemStack plotItem(int index) {
        ItemStack item = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Plot #" + (index + 1));
        meta.setLore(lore(ChatColor.GRAY + "Clique pour ouvrir le shop.",
                ChatColor.DARK_GRAY + "Rotation disponible."));
        meta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, index);
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
                ChatColor.GRAY + "Les pancartes des plots remplacent les NPC."));
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
