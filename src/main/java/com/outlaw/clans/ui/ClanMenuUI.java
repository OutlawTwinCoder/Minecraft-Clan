package com.outlaw.clans.ui;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import com.outlaw.clans.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanMenuUI {

    private final OutlawClansPlugin plugin;
    private final NamespacedKey plotKey;
    public ClanMenuUI(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.plotKey = new NamespacedKey(plugin, Keys.CLAN_MENU_PLOT);
    }

    public void openFor(Player p, Clan clan) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.DARK_PURPLE + "Clan: " + clan.getName());
        int slot = 10;
        for (UUID u : clan.getMembers()) {
            if (slot >= 44) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
            ItemStack it = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = it.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + (op.getName() == null ? u.toString() : op.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + (clan.getLeader().equals(u) ? "Leader" : "Membre"));
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot, it);
            slot++;
        }
        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Territoire");
        List<String> lore = new ArrayList<>();
        if (clan.getTerritory() == null) lore.add(ChatColor.RED + "Non défini");
        else {
            lore.add(ChatColor.GRAY + "Monde: " + clan.getTerritory().getWorldName());
            lore.add(ChatColor.GRAY + "Centre: " + clan.getTerritory().getCenterX() + "," + clan.getTerritory().getCenterY() + "," + clan.getTerritory().getCenterZ());
            lore.add(ChatColor.GRAY + "Rayon: " + clan.getTerritory().getRadius());
        }
        meta.setLore(lore); info.setItemMeta(meta); inv.setItem(4, info);

        if (!clan.getSpots().isEmpty()) {
            int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
            for (int i = 0; i < clan.getSpots().size() && i < slots.length; i++) {
                ItemStack plotItem = new ItemStack(Material.CRAFTING_TABLE);
                ItemMeta plotMeta = plotItem.getItemMeta();
                plotMeta.setDisplayName(ChatColor.AQUA + "Plot #" + (i + 1));
                List<String> plotLore = new ArrayList<>();
                plotLore.add(ChatColor.GRAY + "Clique pour ouvrir le shop.");
                plotLore.add(ChatColor.DARK_GRAY + "Rotation disponible.");
                plotMeta.setLore(plotLore);
                plotMeta.getPersistentDataContainer().set(plotKey, PersistentDataType.INTEGER, i);
                plotItem.setItemMeta(plotMeta);
                inv.setItem(slots[i], plotItem);
            }
        }

        ItemStack center = new ItemStack(Material.LECTERN);
        ItemMeta centerMeta = center.getItemMeta();
        centerMeta.setDisplayName(ChatColor.GOLD + "Centre du Clan");
        List<String> centerLore = new ArrayList<>();
        centerLore.add(ChatColor.GRAY + "Utilise les pupitres pour ouvrir ce menu.");
        centerLore.add(ChatColor.GRAY + "Les pancartes des plots remplacent les NPC.");
        centerMeta.setLore(centerLore);
        center.setItemMeta(centerMeta);
        inv.setItem(13, center);

        p.openInventory(inv);
    }
}
