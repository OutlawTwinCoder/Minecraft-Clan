package com.outlaw.clans.service;

import com.outlaw.clans.OutlawClansPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;

public class SchematicManager {

    private final OutlawClansPlugin plugin;

    public SchematicManager(OutlawClansPlugin plugin) { this.plugin = plugin; }

    public boolean paste(File file, Location loc) { return paste(file, loc, 0); }

    public boolean paste(File file, Location loc, int turns90) {
        try {
            var format = ClipboardFormats.findByFile(file);
            if (format == null) return false;
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();

                var region = clipboard.getRegion();
                BlockVector3 min = region.getMinimumPoint();
                BlockVector3 max = region.getMaximumPoint();
                BlockVector3 origin = clipboard.getOrigin();

                int centerX = (min.getX() + max.getX()) / 2;
                int centerZ = (min.getZ() + max.getZ()) / 2;
                int yOffset = plugin.getConfig().getInt("building.paste_y_offset", 1);

                int ox = origin.getX(); int oz = origin.getZ();
                int vx = centerX - ox; int vz = centerZ - oz;
                int t = ((turns90 % 4) + 4) % 4;
                for (int i=0;i<t;i++) { int nvx = vz; int nvz = -vx; vx = nvx; vz = nvz; }
                int centerRotX = ox + vx; int centerRotZ = oz + vz;

                BlockVector3 adjust = origin.subtract(BlockVector3.at(centerRotX, origin.getY(), centerRotZ));
                BlockVector3 dest = BlockVector3.at(loc.getBlockX(), loc.getBlockY() + yOffset, loc.getBlockZ()).add(adjust);

                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(loc.getWorld()))) {
                    com.sk89q.worldedit.math.transform.AffineTransform transform = new com.sk89q.worldedit.math.transform.AffineTransform();
                    transform = transform.rotateY(90.0 * t);
                    ClipboardHolder holder = new ClipboardHolder(clipboard);
                    holder.setTransform(transform);
                    var operation = holder.createPaste(editSession).to(dest).build();
                    Operations.complete(operation);
                    return true;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}
