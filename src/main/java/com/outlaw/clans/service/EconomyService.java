package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EconomyService {

    public enum Mode {
        XP,
        ITEM
    }

    private final OutlawClansPlugin plugin;

    public EconomyService(OutlawClansPlugin plugin) {
        this.plugin = plugin;
    }

    public Mode mode() {
        try {
            return Mode.valueOf(plugin.getConfig().getString("economy.mode", "XP").toUpperCase());
        } catch (Exception ignored) {
            return Mode.XP;
        }
    }

    public Material itemType() {
        try {
            return Material.valueOf(plugin.getConfig().getString("economy.item_type", "DIAMOND").toUpperCase());
        } catch (Exception ignored) {
            return Material.DIAMOND;
        }
    }

    public int costCreateClan() {
        return mode() == Mode.ITEM
                ? plugin.getConfig().getInt("economy.item.create_clan_cost", 500)
                : plugin.getConfig().getInt("economy.xp.create_clan_cost", 500);
    }

    public int costTerritory() {
        return mode() == Mode.ITEM
                ? plugin.getConfig().getInt("economy.item.territory_cost", 1500)
                : plugin.getConfig().getInt("economy.xp.territory_cost", 1500);
    }

    public int costUpgrade() {
        return mode() == Mode.ITEM
                ? plugin.getConfig().getInt("economy.item.upgrade_cost", 2000)
                : plugin.getConfig().getInt("economy.xp.upgrade_cost", 2000);
    }

    public boolean chargeCreate(Player player) {
        return withdraw(player, costCreateClan());
    }

    public boolean chargeTerritory(Player player) {
        return withdraw(player, costTerritory());
    }

    public boolean withdraw(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!canAfford(player, amount)) {
            return false;
        }
        if (mode() == Mode.XP) {
            takeExperience(player, amount);
        } else {
            removeItems(player, itemType(), amount);
        }
        return true;
    }

    public void deposit(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (mode() == Mode.XP) {
            giveExperience(player, amount);
        } else {
            giveItems(player, itemType(), amount);
        }
    }

    public boolean canAfford(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return mode() == Mode.XP
                ? getTotalExperience(player) >= amount
                : countItem(player, itemType()) >= amount;
    }

    public int getPlayerCurrency(Player player) {
        return mode() == Mode.XP ? getTotalExperience(player) : countItem(player, itemType());
    }

    public List<Integer> amountButtons() {
        List<Integer> configured = plugin.getConfig().getIntegerList("economy.amount_buttons");
        if (configured == null || configured.isEmpty()) {
            return List.of(16, 32, 64, 128);
        }
        List<Integer> sanitized = new ArrayList<>();
        for (Integer value : configured) {
            if (value != null && value > 0 && !sanitized.contains(value)) {
                sanitized.add(value);
            }
        }
        Collections.sort(sanitized);
        return sanitized.isEmpty() ? List.of(16, 32, 64, 128) : sanitized;
    }

    public String formatAmount(int amount) {
        if (mode() == Mode.XP) {
            return amount + " XP";
        }
        return amount + " " + prettify(itemType());
    }

    public Material displayMaterial() {
        return mode() == Mode.XP ? Material.EXPERIENCE_BOTTLE : itemType();
    }

    private int countItem(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeItems(Player player, Material material, int count) {
        for (int slot = 0; slot < player.getInventory().getSize() && count > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(count, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(slot, null);
            }
            count -= take;
        }
    }

    private void giveItems(Player player, Material material, int amount) {
        while (amount > 0) {
            int give = Math.min(material.getMaxStackSize(), amount);
            ItemStack stack = new ItemStack(material, give);
            var leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
            amount -= give;
        }
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int progress = Math.round(player.getExp() * player.getExpToLevel());
        return experienceToReachLevel(level) + progress;
    }

    private void takeExperience(Player player, int amount) {
        int remaining = Math.max(0, getTotalExperience(player) - amount);
        setTotalExperience(player, remaining);
    }

    private void giveExperience(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        player.giveExp(amount);
    }

    private void setTotalExperience(Player player, int amount) {
        int clamped = Math.max(0, amount);
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        player.giveExp(clamped);
    }

    private int experienceToReachLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    private String prettify(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }
}
