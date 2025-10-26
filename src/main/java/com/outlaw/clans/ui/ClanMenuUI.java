package com.outlaw.clans.ui;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanMenuUI {

    private final OutlawClansPlugin plugin;
    public ClanMenuUI(OutlawClansPlugin plugin) { this.plugin = plugin; }

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
        p.openInventory(inv);
    }
}
