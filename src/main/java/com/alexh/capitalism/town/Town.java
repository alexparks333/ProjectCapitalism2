package com.alexh.capitalism.town;

import com.alexh.capitalism.core.CapitalismConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

// The server's town (Town.md), set in the config ("town"): the streets the Whisperer wanders, and the protected town
// center where nobody but ops can break blocks (from TOWN_PROTECT_MIN_Y up). Overworld only.
public final class Town {

    private Town() {
    }

    // A flat area between two corners (inclusive, either order)
    public record Area(int minX, int maxX, int minZ, int maxZ) {
        public static Area of(int x1, int z1, int x2, int z2) {
            return new Area(Math.min(x1, x2), Math.max(x1, x2), Math.min(z1, z2), Math.max(z1, z2));
        }

        public boolean contains(int x, int z) {
            return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
        }

        public boolean contains(BlockPos pos) {
            return this.contains(pos.getX(), pos.getZ());
        }

        public BlockPos center(int y) {
            return new BlockPos((this.minX + this.maxX) / 2, y, (this.minZ + this.maxZ) / 2);
        }

        public AABB box(double minY, double maxY) {
            return new AABB(this.minX, minY, this.minZ, this.maxX + 1, maxY, this.maxZ + 1);
        }
    }

    public static Area walkArea() {
        return Area.of(CapitalismConfig.TOWN_WALK_MIN_X.get(), CapitalismConfig.TOWN_WALK_MIN_Z.get(),
            CapitalismConfig.TOWN_WALK_MAX_X.get(), CapitalismConfig.TOWN_WALK_MAX_Z.get());
    }

    public static Area protectedArea() {
        return Area.of(CapitalismConfig.TOWN_PROTECT_MIN_X.get(), CapitalismConfig.TOWN_PROTECT_MIN_Z.get(),
            CapitalismConfig.TOWN_PROTECT_MAX_X.get(), CapitalismConfig.TOWN_PROTECT_MAX_Z.get());
    }

    // Is this block in the protected town center? (Overworld, inside the area, at TOWN_PROTECT_MIN_Y or higher)
    public static boolean isProtected(Level level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && pos.getY() >= CapitalismConfig.TOWN_PROTECT_MIN_Y.get()
            && protectedArea().contains(pos);
    }

    // (dev: /tp town, /event bloodmoon tp) drops an overworld player on the ground at x, z (loads the chunk)
    public static boolean teleportOnTop(net.minecraft.server.level.ServerPlayer player, int x, int z) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        var level = player.level();
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        player.teleportTo(x + 0.5, y, z + 0.5);
        player.fallDistance = 0;
        return true;
    }
}
