package com.outlaw.clans.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum ClanRolePermission {
    BUILD_TERRAIN("Construire sur le territoire", "Autorise le placement et la casse de blocs sur les terrains du clan.", Material.IRON_PICKAXE),
    MANAGE_TERRAINS("Gérer les terrains", "Permet de sélectionner les bâtiments, coffres et ressources des terrains.", Material.CARTOGRAPHY_TABLE),
    ACCESS_FARM_CHEST("Accéder aux coffres de ferme", "Peut ouvrir les coffres de production et récupérer les ressources.", Material.CHEST),
    MANAGE_TREASURY("Gérer la banque", "Peut retirer, distribuer ou déposer la monnaie du clan.", Material.EMERALD),
    MANAGE_ROLES("Gérer les rôles", "Peut modifier les permissions des rôles et assigner les membres.", Material.NAME_TAG);

    private final String displayName;
    private final String description;
    private final Material icon;

    ClanRolePermission(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return ChatColor.GOLD + displayName;
    }

    public String getDescription() {
        return ChatColor.GRAY + description;
    }

    public Material getIcon() {
        return icon;
    }
}
