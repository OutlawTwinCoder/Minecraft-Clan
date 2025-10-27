package com.outlaw.clans;

import com.outlaw.clans.listeners.ClaimStickListener;
import com.outlaw.clans.listeners.ClanCenterInteractionListener;
import com.outlaw.clans.listeners.TerrainProtectionListener;
import com.outlaw.clans.service.ClanManager;
import com.outlaw.clans.service.NPCManager;
import com.outlaw.clans.service.SchematicManager;
import com.outlaw.clans.service.EconomyService;
import com.outlaw.clans.service.TerraformService;
import com.outlaw.clans.service.FarmManager;
import com.outlaw.clans.ui.ClanMenuUI;
import com.outlaw.clans.commands.CreateClanCommand;
import com.outlaw.clans.commands.ClanMenuCommand;
import com.outlaw.clans.commands.ShowTerritoryCommand;
import com.outlaw.clans.commands.CreateClanCostCommand;
import com.outlaw.clans.commands.ClanTerritoryCostCommand;
import com.outlaw.clans.commands.DeleteClanCommand;
import com.outlaw.clans.commands.LeaveClanCommand;
import com.outlaw.clans.commands.ClanUpgradeCostCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OutlawClansPlugin extends JavaPlugin {

    private static OutlawClansPlugin instance;
    private com.outlaw.clans.service.ClanManager clanManager;
    private com.outlaw.clans.service.NPCManager npcManager;
    private com.outlaw.clans.service.SchematicManager schematicManager;
    private com.outlaw.clans.service.EconomyService economyService;
    private com.outlaw.clans.service.TerraformService terraformService;
    private com.outlaw.clans.service.FarmManager farmManager;
    private ClanMenuUI clanMenuUI;

    public static OutlawClansPlugin get() { return instance; }
    public ClanManager clans() { return clanManager; }
    public NPCManager npcs() { return npcManager; }
    public SchematicManager schematics() { return schematicManager; }
    public EconomyService economy() { return economyService; }
    public TerraformService terraform() { return terraformService; }
    public ClanMenuUI menuUI() { return clanMenuUI; }
    public FarmManager farms() { return farmManager; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.clanManager = new ClanManager(this);
        this.schematicManager = new SchematicManager(this);
        this.economyService = new EconomyService(this);
        this.terraformService = new TerraformService(this);
        this.npcManager = new NPCManager(this);
        this.clanMenuUI = new ClanMenuUI(this);
        this.farmManager = new FarmManager(this);
        bootstrapSchematics();

        getCommand("create").setExecutor(new CreateClanCommand(this));
        getCommand("clan").setExecutor(new ClanMenuCommand(this));
        getCommand("show").setExecutor(new ShowTerritoryCommand(this));
        getCommand("createclancost").setExecutor(new CreateClanCostCommand(this));
        getCommand("clanterritorycost").setExecutor(new ClanTerritoryCostCommand(this));
        getCommand("clanupgrade").setExecutor(new ClanUpgradeCostCommand(this));
        getCommand("deleteclan").setExecutor(new DeleteClanCommand(this));
        getCommand("leaveclan").setExecutor(new LeaveClanCommand(this));

        Bukkit.getPluginManager().registerEvents(new ClaimStickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanCenterInteractionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TerrainProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(farmManager, this);
        getLogger().info("OutlawClans v0.3.8 enabled.");
    }

    @Override
    public void onDisable() {
        npcManager.despawnAllDisplays();
        if (farmManager != null) {
            farmManager.shutdown();
        }
        clanManager.saveAll();
        getLogger().info("OutlawClans disabled.");
    }

    private void bootstrapSchematics() {
        java.io.File dataSchems = new java.io.File(getDataFolder(), "schematics");
        if (!dataSchems.exists()) dataSchems.mkdirs();
        java.io.File weDir = new java.io.File(getDataFolder().getParentFile(), "WorldEdit/schematics");
        if (!weDir.exists()) weDir.mkdirs();
        java.util.List<String> wl = getConfig().getStringList("schematics.whitelist");
        for (String name : wl) {
            try {
                java.io.File src = new java.io.File(dataSchems, name);
                if (src.exists()) {
                    java.io.File dst = new java.io.File(weDir, name);
                    if (!dst.exists()) java.nio.file.Files.copy(src.toPath(), dst.toPath());
                }
            } catch (Exception ignored) {}
        }
    }
}
