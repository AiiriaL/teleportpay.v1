package net.aiirial.teleportpay.waypoint;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class WaypointData {
    public String name;
    public int x, y, z;
    public String dimension; // z.B. "minecraft:overworld"

    public WaypointData() {
    }

    public WaypointData(String name, Vec3i pos, ResourceLocation dimension) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.dimension = dimension.toString();
    }

    public WaypointData(String name, Vec3i pos, String dimension) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.dimension = dimension;
    }

    public ResourceLocation getDimensionLocation() {
        return parseResourceLocation(dimension);
    }

    public net.minecraft.resources.ResourceKey<Level> getDimensionKey() {
        return net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                parseResourceLocation(dimension)
        );
    }

    private static ResourceLocation parseResourceLocation(String dimString) {
        if (dimString == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(dimString);
        if (rl != null) {
            return rl;
        } else {
            // Fallback mit String im Format "namespace:path"
            return ResourceLocation.tryParse("minecraft:overworld");
        }
    }

}

