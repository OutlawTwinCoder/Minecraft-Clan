package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.outlaw.clans.model.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyService {

    public enum Mode { EXP, ITEM }

    private final OutlawClansPlugin plugin;
    private final File balancesFile;
    private YamlConfiguration balancesCfg;
    private final Map<UUID, Integer> balances = new ConcurrentHashMap<>();

    public CurrencyService(OutlawClansPlugin plugin) {
        this.plugin = plugin;
        this.balancesFile = new File(plugin.getDataFolder(), "currency.yml");
        loadBalances();
    }

    private void loadBalances() {
        if (!balancesFile.exists()) {
            try {
                balancesFile.getParentFile().mkdirs();
                balancesFile.createNewFile();
            } catch (IOException ignored) {}
        }
        balancesCfg = YamlConfiguration.loadConfiguration(balancesFile);
        balances.clear();
        if (balancesCfg.isConfigurationSection("clans")) {
            for (String key : balancesCfg.getConfigurationSection("clans").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    int amount = Math.max(0, balancesCfg.getInt("clans." + key, 0));
                    balances.put(id, amount);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void ensureClanAccount(UUID clanId) {
        balances.putIfAbsent(clanId, 0);
    }

    public void removeClanAccount(UUID clanId) {
        balances.remove(clanId);
        if (balancesCfg != null) {
            balancesCfg.set("clans." + clanId, null);
            saveBalances();
        }
    }

    public int getClanBalance(UUID clanId) {
        return balances.getOrDefault(clanId, 0);
    }

    public void setClanBalance(UUID clanId, int amount) {
        balances.put(clanId, Math.max(0, amount));
        if (balancesCfg != null) {
            balancesCfg.set("clans." + clanId, Math.max(0, amount));
        }
        saveBalances();
    }

    public void addClanBalance(UUID clanId, int amount) {
        if (amount <= 0) {
            return;
        }
        setClanBalance(clanId, getClanBalance(clanId) + amount);
    }

    public boolean withdrawClanBalance(UUID clanId, int amount) {
        if (amount <= 0) {
            return false;
        }
        int current = getClanBalance(clanId);
        if (current < amount) {
            return false;
        }
        setClanBalance(clanId, current - amount);
        return true;
    }

    public void saveBalances() {
        if (balancesCfg == null) {
            return;
        }
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            balancesCfg.set("clans." + entry.getKey(), Math.max(0, entry.getValue()));
        }
        try {
            balancesCfg.save(balancesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'enregistrer currency.yml: " + e.getMessage());
        }
    }

    public Mode mode() {
        String raw = plugin.getConfig().getString("currency.mode", "EXP");
        try {
            return Mode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Mode.EXP;
        }
    }

    public Material itemType() {
        String raw = plugin.getConfig().getString("currency.item_type", "DIAMOND");
        Material material = Material.matchMaterial(raw == null ? "DIAMOND" : raw.trim().toUpperCase());
        return material != null ? material : Material.DIAMOND;
    }

    public int costCreateClan() {
        return Math.max(0, plugin.getConfig().getInt("currency.create_cost", 500));
    }

    public int costTerritory() {
        return Math.max(0, plugin.getConfig().getInt("currency.territory_cost", 1500));
    }

    public int costUpgrade() {
        return Math.max(0, plugin.getConfig().getInt("currency.upgrade_cost", 5000));
    }

    public boolean chargeCreate(Player player) {
        int cost = costCreateClan();
        if (cost <= 0) {
            return true;
        }
        return withdrawFromPlayer(player, cost);
    }

    public boolean chargeTerritory(Player player) {
        int cost = costTerritory();
        if (cost <= 0) {
            return true;
        }
        return withdrawFromPlayer(player, cost);
    }

    public boolean withdrawFromPlayer(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (mode() == Mode.EXP) {
            if (getTotalExperience(player) < amount) {
                return false;
            }
            removeExperience(player, amount);
            return true;
        }
        Material material = itemType();
        if (countItem(player, material) < amount) {
            return false;
        }
        removeItems(player, material, amount);
        return true;
    }

    public void giveToPlayer(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        if (mode() == Mode.EXP) {
            player.giveExp(amount);
            return;
        }
        Material material = itemType();
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(material.getMaxStackSize(), remaining);
            ItemStack stack = new ItemStack(material, stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
            remaining -= stackAmount;
        }
        player.updateInventory();
    }

    public boolean depositIntoClan(Player player, Clan clan, int amount) {
        if (clan == null || amount <= 0) {
            return false;
        }
        if (!withdrawFromPlayer(player, amount)) {
            return false;
        }
        addClanBalance(clan.getId(), amount);
        return true;
    }

    public boolean withdrawFromClan(Player player, Clan clan, int amount) {
        if (clan == null || amount <= 0) {
            return false;
        }
        if (!withdrawClanBalance(clan.getId(), amount)) {
            return false;
        }
        giveToPlayer(player, amount);
        return true;
    }

    public boolean transferToMember(Clan clan, Player receiver, int amount) {
        if (clan == null || receiver == null || amount <= 0) {
            return false;
        }
        if (!withdrawClanBalance(clan.getId(), amount)) {
            return false;
        }
        giveToPlayer(receiver, amount);
        return true;
    }

    public String formatAmount(int amount) {
        amount = Math.max(0, amount);
        if (mode() == Mode.EXP) {
            return ChatColor.YELLOW + String.valueOf(amount) + ChatColor.GRAY + " XP";
        }
        return ChatColor.YELLOW + String.valueOf(amount) + ChatColor.GRAY + " " + ChatColor.YELLOW + prettyMaterial(itemType());
    }

    private int countItem(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
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

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int progress = Math.round(player.getExp() * player.getExpToLevel());
        return xpAtLevel(level) + progress;
    }

    private void removeExperience(Player player, int amount) {
        int total = Math.max(0, getTotalExperience(player) - amount);
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.giveExp(total);
    }

    private int xpAtLevel(int level) {
        if (level <= 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        }
        if (level <= 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        }
        return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
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
}
