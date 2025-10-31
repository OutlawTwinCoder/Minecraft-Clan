package com.outlaw.clans.service;

import com.outlaw.economy.api.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class EconomyService {

    public enum Mode { MONEY, ITEM, EXPERIENCE }

    private final com.outlaw.clans.OutlawClansPlugin plugin;
    private final MoneyHook moneyHook = new MoneyHook();
    private File moneyFile;
    private YamlConfiguration moneyCfg;

    public EconomyService(com.outlaw.clans.OutlawClansPlugin plugin) {
        this.plugin = plugin;
        moneyHook.tryHook();
        if (!moneyHook.isActive()) {
            loadMoney();
        }
    }

    private void loadMoney() {
        moneyFile = new File(plugin.getDataFolder(), "money.yml");
        if (!moneyFile.exists()) { try { moneyFile.getParentFile().mkdirs(); moneyFile.createNewFile(); } catch (Exception ignored) {} }
        moneyCfg = YamlConfiguration.loadConfiguration(moneyFile);
    }

    public void saveMoney() {
        if (moneyCfg == null || moneyFile == null) {
            return;
        }
        try { moneyCfg.save(moneyFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public double getBalance(UUID player) {
        if (moneyHook.isActive()) {
            return moneyHook.getBalance(player);
        }
        return moneyCfg != null ? moneyCfg.getDouble("balances."+player, 0.0) : 0.0;
    }

    public void setBalance(UUID player, double amount) {
        if (moneyHook.isActive()) {
            moneyHook.setBalance(player, amount);
            return;
        }
        if (moneyCfg == null) {
            loadMoney();
        }
        moneyCfg.set("balances."+player, amount);
        saveMoney();
    }

    public void give(UUID player, double amount) {
        if (moneyHook.isActive()) {
            moneyHook.deposit(player, amount, "OutlawClans: deposit");
            return;
        }
        setBalance(player, getBalance(player) + amount);
    }

    public void take(UUID player, double amount) {
        if (moneyHook.isActive()) {
            moneyHook.withdraw(player, amount, "OutlawClans: withdraw");
            return;
        }
        setBalance(player, Math.max(0, getBalance(player) - amount));
    }

    public Mode mode() {
        try {
            String raw = plugin.getConfig().getString("economy.mode", "MONEY");
            if (raw == null) raw = "MONEY";
            if (raw.equalsIgnoreCase("XP")) raw = "EXPERIENCE";
            return Mode.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return Mode.MONEY;
        }
    }
    public Material itemType() { try { return Material.valueOf(plugin.getConfig().getString("economy.item_type","DIAMOND").toUpperCase()); } catch (Exception e) { return Material.DIAMOND; }
    }
    public int costCreateClan() {
        return switch (mode()) {
            case ITEM -> plugin.getConfig().getInt("economy.item.create_clan_cost", 500);
            case EXPERIENCE -> plugin.getConfig().getInt("economy.experience.create_clan_cost", 500);
            case MONEY -> plugin.getConfig().getInt("economy.money.create_clan_cost", 500);
        };
    }

    public int costTerritory() {
        return switch (mode()) {
            case ITEM -> plugin.getConfig().getInt("economy.item.territory_cost", 1500);
            case EXPERIENCE -> plugin.getConfig().getInt("economy.experience.territory_cost", 1500);
            case MONEY -> plugin.getConfig().getInt("economy.money.territory_cost", 1500);
        };
    }

    public boolean chargeCreate(Player p) { return charge(p, costCreateClan(), "create clan"); }
    public boolean chargeTerritory(Player p) { return charge(p, costTerritory(), "claim territory"); }

    private boolean charge(Player p, int cost, String action) {
        if (!canAfford(p, cost)) return false;
        switch (mode()) {
            case MONEY -> {
                if (moneyHook.isActive()) {
                    if (!moneyHook.withdraw(p.getUniqueId(), cost, "OutlawClans: " + action)) {
                        return false;
                    }
                } else {
                    take(p.getUniqueId(), cost);
                }
            }
            case ITEM -> removeItems(p, itemType(), cost);
            case EXPERIENCE -> removeExperience(p, cost);
        }
        return true;
    }

    public boolean canAfford(Player p, int cost) {
        return switch (mode()) {
            case MONEY -> moneyHook.canAfford(p.getUniqueId(), cost) || (!moneyHook.isActive() && getBalance(p.getUniqueId()) >= cost);
            case ITEM -> countItem(p, itemType()) >= cost;
            case EXPERIENCE -> p.getTotalExperience() >= cost;
        };
    }

    private int countItem(Player p, Material m) { int c=0; for (ItemStack is : p.getInventory().getContents()) if (is!=null && is.getType()==m) c+=is.getAmount(); return c; }
    private void removeItems(Player p, Material m, int count) {
        for (int i=0; i<p.getInventory().getSize() && count>0; i++) {
            ItemStack is = p.getInventory().getItem(i);
            if (is==null || is.getType()!=m) continue;
            int take = Math.min(count, is.getAmount());
            is.setAmount(is.getAmount()-take); if (is.getAmount()<=0) p.getInventory().setItem(i, null);
            count -= take;
        }
    }

    private void removeExperience(Player p, int amount) {
        int available = p.getTotalExperience();
        int toRemove = Math.min(amount, available);
        if (toRemove != 0) {
            p.giveExp(-toRemove);
        }
    }

    private final class MoneyHook {
        private EconomyAdapter adapter;

        void tryHook() {
            if (hookOutlawEconomy()) {
                return;
            }
            if (hookVault()) {
                return;
            }
            plugin.getLogger().warning("Aucun service d'économie trouvé, utilisation du stockage interne pour l'argent des clans.");
        }

        private boolean hookOutlawEconomy() {
            try {
                RegisteredServiceProvider<EconomyService> registration =
                        Bukkit.getServicesManager().getRegistration(EconomyService.class);
                if (registration == null) {
                    return false;
                }
                adapter = new OutlawEconomyAdapter(registration.getProvider());
                plugin.getLogger().info("Connexion à OutlawEconomy via ServicesManager réussie.");
                return true;
            } catch (NoClassDefFoundError ignored) {
                return false;
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Impossible de se connecter à OutlawEconomy via ServicesManager", ex);
                return false;
            }
        }

        private boolean hookVault() {
            try {
                RegisteredServiceProvider<Economy> registration =
                        Bukkit.getServicesManager().getRegistration(Economy.class);
                if (registration == null) {
                    return false;
                }
                adapter = new VaultAdapter(registration.getProvider());
                plugin.getLogger().info("Connexion à Vault réussie (compatibilité économie globale).");
                return true;
            } catch (NoClassDefFoundError ignored) {
                return false;
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Impossible de se connecter à Vault", ex);
                return false;
            }
        }

        boolean isActive() {
            return adapter != null;
        }

        double getBalance(UUID player) {
            return adapter != null ? adapter.getBalance(player) : 0.0;
        }

        void setBalance(UUID player, double amount) {
            if (adapter != null) {
                adapter.setBalance(player, amount);
            }
        }

        void deposit(UUID player, double amount, String reason) {
            if (adapter != null && amount > 0) {
                adapter.deposit(player, amount, reason);
            }
        }

        boolean withdraw(UUID player, double amount, String reason) {
            if (adapter == null || amount <= 0) {
                return adapter != null;
            }
            return adapter.withdraw(player, amount, reason);
        }

        boolean canAfford(UUID player, double amount) {
            return adapter != null && adapter.canAfford(player, amount);
        }
    }

    private interface EconomyAdapter {
        double getBalance(UUID player);

        boolean deposit(UUID player, double amount, String reason);

        boolean withdraw(UUID player, double amount, String reason);

        default boolean canAfford(UUID player, double amount) {
            return getBalance(player) >= amount;
        }

        default void setBalance(UUID player, double amount) {
            double current = getBalance(player);
            double delta = amount - current;
            if (Math.abs(delta) < 1e-6) {
                return;
            }
            if (delta > 0) {
                deposit(player, delta, "OutlawClans: sync");
            } else {
                withdraw(player, -delta, "OutlawClans: sync");
            }
        }
    }

    private final class OutlawEconomyAdapter implements EconomyAdapter {
        private final EconomyService delegate;

        private OutlawEconomyAdapter(EconomyService delegate) {
            this.delegate = delegate;
        }

        @Override
        public double getBalance(UUID player) {
            return delegate.getBalance(player);
        }

        @Override
        public boolean deposit(UUID player, double amount, String reason) {
            return delegate.deposit(player, amount, reason);
        }

        @Override
        public boolean withdraw(UUID player, double amount, String reason) {
            return delegate.withdraw(player, amount, reason);
        }
    }

    private final class VaultAdapter implements EconomyAdapter {
        private final Economy delegate;

        private VaultAdapter(Economy delegate) {
            this.delegate = delegate;
        }

        @Override
        public double getBalance(UUID player) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
            return delegate.getBalance(offlinePlayer);
        }

        @Override
        public boolean deposit(UUID player, double amount, String reason) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
            EconomyResponse response = delegate.depositPlayer(offlinePlayer, amount);
            if (!response.transactionSuccess()) {
                plugin.getLogger().log(Level.WARNING, "Vault deposit échoué pour {0}: {1}", new Object[]{offlinePlayer.getName(), response.errorMessage});
            }
            return response.transactionSuccess();
        }

        @Override
        public boolean withdraw(UUID player, double amount, String reason) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
            EconomyResponse response = delegate.withdrawPlayer(offlinePlayer, amount);
            if (!response.transactionSuccess()) {
                plugin.getLogger().log(Level.WARNING, "Vault withdraw échoué pour {0}: {1}", new Object[]{offlinePlayer.getName(), response.errorMessage});
            }
            return response.transactionSuccess();
        }

        @Override
        public boolean canAfford(UUID player, double amount) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
            return delegate.has(offlinePlayer, amount);
        }
    }
}
