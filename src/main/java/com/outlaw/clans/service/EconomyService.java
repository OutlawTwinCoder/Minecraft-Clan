package com.outlaw.clans.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.lang.reflect.Method;
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
            moneyHook.deposit(player, amount);
            return;
        }
        setBalance(player, getBalance(player) + amount);
    }

    public void take(UUID player, double amount) {
        if (moneyHook.isActive()) {
            moneyHook.withdraw(player, amount);
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

    public boolean chargeCreate(Player p) { return charge(p, costCreateClan()); }
    public boolean chargeTerritory(Player p) { return charge(p, costTerritory()); }

    private boolean charge(Player p, int cost) {
        if (!canAfford(p, cost)) return false;
        switch (mode()) {
            case MONEY -> {
                if (moneyHook.isActive()) {
                    if (!moneyHook.withdraw(p.getUniqueId(), cost)) {
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
        private enum HookType { NONE, SERVICE, STATIC }

        private HookType type = HookType.NONE;
        private Object serviceInstance;
        private Method serviceGetBalance;
        private Method serviceSetBalance;
        private Method serviceDeposit;
        private Method serviceWithdraw;
        private Method serviceHas;

        private Method staticGetBalance;
        private Method staticDeposit;
        private Method staticWithdraw;

        void tryHook() {
            hookServiceManager();
            if (type == HookType.NONE) {
                hookStaticApi();
            }
            if (type == HookType.NONE) {
                plugin.getLogger().warning("OutlawEconomy introuvable, utilisation du stockage interne pour l'argent des clans.");
            }
        }

        private void hookServiceManager() {
            try {
                Class<?> serviceClass = Class.forName("com.outlaweco.api.EconomyService");
                Object provider = Bukkit.getServicesManager().load((Class) serviceClass);
                if (provider != null) {
                    serviceInstance = provider;
                    serviceGetBalance = serviceClass.getMethod("getBalance", UUID.class);
                    serviceSetBalance = serviceClass.getMethod("setBalance", UUID.class, double.class);
                    serviceDeposit = serviceClass.getMethod("deposit", UUID.class, double.class);
                    serviceWithdraw = serviceClass.getMethod("withdraw", UUID.class, double.class);
                    serviceHas = serviceClass.getMethod("has", UUID.class, double.class);
                    type = HookType.SERVICE;
                    plugin.getLogger().info("Connexion à OutlawEconomy via ServicesManager réussie.");
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Impossible de se connecter à OutlawEconomy via ServicesManager", ex);
            }
        }

        private void hookStaticApi() {
            try {
                Class<?> apiClass = Class.forName("com.outlaweco.api.EconomyAPI");
                staticGetBalance = apiClass.getMethod("getBalance", UUID.class);
                staticDeposit = apiClass.getMethod("deposit", UUID.class, double.class);
                staticWithdraw = apiClass.getMethod("withdraw", UUID.class, double.class);
                type = HookType.STATIC;
                plugin.getLogger().info("Connexion à OutlawEconomy via l'API statique.");
            } catch (ClassNotFoundException ignored) {
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Impossible d'utiliser l'API statique OutlawEconomy", ex);
            }
        }

        boolean isActive() {
            return type != HookType.NONE;
        }

        double getBalance(UUID player) {
            try {
                return switch (type) {
                    case SERVICE -> ((Number) serviceGetBalance.invoke(serviceInstance, player)).doubleValue();
                    case STATIC -> ((Number) staticGetBalance.invoke(null, player)).doubleValue();
                    default -> 0.0;
                };
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors de la récupération du solde", ex);
                return 0.0;
            }
        }

        void setBalance(UUID player, double amount) {
            try {
                switch (type) {
                    case SERVICE -> serviceSetBalance.invoke(serviceInstance, player, amount);
                    case STATIC -> {
                        double current = getBalance(player);
                        double delta = amount - current;
                        if (Math.abs(delta) < 1e-6) {
                            return;
                        }
                        if (delta > 0) {
                            deposit(player, delta);
                        } else {
                            withdraw(player, -delta);
                        }
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors de la mise à jour du solde", ex);
            }
        }

        void deposit(UUID player, double amount) {
            if (amount <= 0) return;
            try {
                switch (type) {
                    case SERVICE -> serviceDeposit.invoke(serviceInstance, player, amount);
                    case STATIC -> staticDeposit.invoke(null, player, amount);
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors du dépôt d'argent", ex);
            }
        }

        boolean withdraw(UUID player, double amount) {
            if (amount <= 0) return true;
            try {
                return switch (type) {
                    case SERVICE -> (Boolean) serviceWithdraw.invoke(serviceInstance, player, amount);
                    case STATIC -> (Boolean) staticWithdraw.invoke(null, player, amount);
                    default -> false;
                };
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors du retrait d'argent", ex);
                return false;
            }
        }

        boolean canAfford(UUID player, double amount) {
            if (!isActive()) {
                return false;
            }
            try {
                return switch (type) {
                    case SERVICE -> serviceHas != null ? (Boolean) serviceHas.invoke(serviceInstance, player, amount) : getBalance(player) >= amount;
                    case STATIC -> getBalance(player) >= amount;
                    default -> false;
                };
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur lors de la vérification du solde", ex);
                return getBalance(player) >= amount;
            }
        }
    }
}
