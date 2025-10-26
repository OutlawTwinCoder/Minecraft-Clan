package com.outlaw.clans.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EconomyService {

    public enum Mode { MONEY, ITEM }

    private final com.outlaw.clans.OutlawClansPlugin plugin;
    private File moneyFile;
    private YamlConfiguration moneyCfg;

    public EconomyService(com.outlaw.clans.OutlawClansPlugin plugin) { this.plugin = plugin; loadMoney(); }

    private void loadMoney() {
        moneyFile = new File(plugin.getDataFolder(), "money.yml");
        if (!moneyFile.exists()) { try { moneyFile.getParentFile().mkdirs(); moneyFile.createNewFile(); } catch (Exception ignored) {} }
        moneyCfg = YamlConfiguration.loadConfiguration(moneyFile);
    }

    public void saveMoney() { try { moneyCfg.save(moneyFile); } catch (IOException e) { e.printStackTrace(); } }

    public double getBalance(UUID player) { return moneyCfg.getDouble("balances."+player, 0.0); }
    public void setBalance(UUID player, double amount) { moneyCfg.set("balances."+player, amount); saveMoney(); }
    public void give(UUID player, double amount) { setBalance(player, getBalance(player) + amount); }
    public void take(UUID player, double amount) { setBalance(player, Math.max(0, getBalance(player) - amount)); }

    public Mode mode() { try { return Mode.valueOf(plugin.getConfig().getString("economy.mode","MONEY").toUpperCase()); } catch (Exception e) { return Mode.MONEY; } }
    public Material itemType() { try { return Material.valueOf(plugin.getConfig().getString("economy.item_type","DIAMOND").toUpperCase()); } catch (Exception e) { return Material.DIAMOND; }
    }
    public int costCreateClan() { return mode()==Mode.ITEM ? plugin.getConfig().getInt("economy.item.create_clan_cost",500) : plugin.getConfig().getInt("economy.money.create_clan_cost",500); }
    public int costTerritory() { return mode()==Mode.ITEM ? plugin.getConfig().getInt("economy.item.territory_cost",1500) : plugin.getConfig().getInt("economy.money.territory_cost",1500); }

    public boolean chargeCreate(Player p) { return charge(p, costCreateClan()); }
    public boolean chargeTerritory(Player p) { return charge(p, costTerritory()); }

    private boolean charge(Player p, int cost) {
        if (!canAfford(p, cost)) return false;
        if (mode()==Mode.MONEY) take(p.getUniqueId(), cost); else removeItems(p, itemType(), cost);
        return true;
    }
    public boolean canAfford(Player p, int cost) { return mode()==Mode.MONEY ? getBalance(p.getUniqueId())>=cost : countItem(p, itemType())>=cost; }

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
}
