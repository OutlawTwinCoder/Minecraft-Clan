package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EconomyService {

    public enum Mode { XP, ITEM }

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

    public boolean chargeCreate(Player player) {
        return charge(player, costCreateClan());
    }

    public boolean chargeTerritory(Player player) {
        return charge(player, costTerritory());
    }

    public boolean charge(Player player, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (!hasPlayerCurrency(player, cost)) {
            return false;
        }
        withdrawPlayerCurrency(player, cost);
        return true;
    }

    public boolean hasPlayerCurrency(Player player, int amount) {
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

    public void withdrawPlayerCurrency(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (mode() == Mode.XP) {
            removeExperience(player, amount);
        } else {
            removeItems(player, itemType(), amount);
        }
    }

    public void givePlayerCurrency(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (mode() == Mode.XP) {
            player.giveExp(amount);
        } else {
            addItems(player, itemType(), amount);
        }
    }

    public String currencyName() {
        return mode() == Mode.XP ? "points d'expérience" : prettyMaterial(itemType());
    }

    public String formatAmount(int amount) {
        return amount + " " + currencyName();
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
        player.updateInventory();
    }

    private void addItems(Player player, Material material, int count) {
        while (count > 0) {
            int stackSize = Math.min(count, material.getMaxStackSize());
            ItemStack itemStack = new ItemStack(material, stackSize);
            player.getInventory().addItem(itemStack).values()
                    .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
            count -= stackSize;
        }
        player.updateInventory();
    }

    private void removeExperience(Player player, int amount) {
        int current = getTotalExperience(player);
        int target = Math.max(0, current - amount);
        setTotalExperience(player, target);
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int total = getTotalExperienceToLevel(level);
        total += Math.round(player.getExp() * player.getExpToLevel());
        return total;
    }

    private void setTotalExperience(Player player, int total) {
        int level = 0;
        int exp = total;
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0f);

        while (exp > 0) {
            int expToNext = getExpToLevel(level);
            if (exp >= expToNext) {
                exp -= expToNext;
                level++;
                continue;
            }
            player.setLevel(level);
            player.setExp(exp / (float) expToNext);
            player.setTotalExperience(total);
            return;
        }

        player.setLevel(level);
        player.setExp(0f);
        player.setTotalExperience(total);
    }

    private int getExpToLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    private int getTotalExperienceToLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
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

    public void refreshCurrencyModeForAll() {
        Bukkit.getOnlinePlayers().forEach(player -> player.updateInventory());
    }
}
