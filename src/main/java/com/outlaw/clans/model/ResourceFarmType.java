package com.outlaw.clans.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceFarmType {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final List<String> description;
    private final List<String> schematics;
    private final LinkedHashMap<Material, Integer> outputs;

    public ResourceFarmType(String id,
                            String displayName,
                            Material icon,
                            List<String> description,
                            List<String> schematics,
                            LinkedHashMap<Material, Integer> outputs) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.schematics = schematics;
        this.outputs = outputs;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<String> getSchematics() {
        return schematics;
    }

    public Map<Material, Integer> getOutputs() {
        return Collections.unmodifiableMap(outputs);
    }

    public Material getDefaultOutput() {
        return outputs.isEmpty() ? null : outputs.keySet().iterator().next();
    }

    public Integer getAmountFor(Material material) {
        return outputs.get(material);
    }
}
