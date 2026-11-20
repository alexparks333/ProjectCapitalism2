package com.alexh.capitalism.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The Blood Moon decorations (Decorations.md > Blood Moon decorations): permanent gothic pieces bought from the Blood
// Merchant for Blood Shards. Unlike the seasonal ones they're always there; anyone can break one (it drops itself) and
// place it anywhere. Each is drawn by client/BloodDecorRenderer (models in client/BloodDecorModels and
// client/BloodDecorModels2, textures painted by tools/BloodDecorTex.cs). Their ids (blocks and items) are the shop's: keep them.
//
// The big ones fill more than one block (their footprint: `wide` blocks to the right as you face their front, `deep`
// toward their back, `tall` up): the block you place is the main one (it has the block entity and draws the whole
// piece), the others are part blocks (decor/BloodPartBlock) that point back at it. Their solid shape (`boxes`) is given
// for the whole footprint, facing west (the front toward -x), in pixels from the main block's corner: x toward the back,
// z toward the right, y up; each block gets its own slice of it (cellShape), turned for the way the piece faces.
public enum BloodDecorKind {

    BLOOD_MOON_LANTERN("blood_moon_lantern", "Blood Moon Lantern", 12, F.GLOWS, 1, 1, 1, 4, 0, 4, 12, 12, 12),
    CRIMSON_CANDELABRA("crimson_candelabra", "Crimson Candelabra", 12, F.GLOWS, 1, 1, 1, 5, 0, 5, 11, 15, 11),
    CRIMSON_THRONE("crimson_throne", "Crimson Throne", 0, F.GLOWS, 1, 2, 1, 1, 0, 1, 15, 11, 15, 12, 0, 1, 15, 31, 15),
    VAMPIRE_COFFIN("vampire_coffin", "Vampire Coffin", 0, F.BED, 1, 1, 2, 1, 0, 1, 31, 9, 15),
    BLOOD_FOUNTAIN("blood_fountain", "Blood Fountain", 8, F.GLOWS, 2, 1, 2, 0, 0, 0, 32, 9, 32, 12, 0, 12, 20, 16, 20),
    BLOOD_MOON_BANNER("blood_moon_banner", "Blood Moon Banner", 5, F.GLOWS, 1, 1, 1, 6, 0, 6, 10, 16, 10),
    CRIMSON_RUG("crimson_rug", "Crimson Rug", 0, F.PASSABLE, 1, 1, 1, 0, 0, 0, 16, 1, 16),
    REVENANT_GREATSWORD_STATUE("revenant_greatsword_statue", "Revenant's Greatsword in Stone", 7, F.GLOWS, 1, 2, 1,
        1, 0, 1, 15, 11, 15, 7, 0, 5, 9, 30, 11),
    GOREFANG_SKULL_TROPHY("gorefang_skull_trophy", "Gorefang Skull Trophy", 3, F.WALL | F.GLOWS, 1, 1, 1, 2, 2, 1, 16, 15, 15),
    SANGUINE_STAFF_PEDESTAL("sanguine_staff_pedestal", "Sanguine Witch's Crystal Staff", 10, F.GLOWS, 1, 2, 1,
        3, 0, 3, 13, 10, 13, 7, 0, 7, 9, 26, 9),
    BAT_GARGOYLE("bat_gargoyle", "Bat Gargoyle", 3, F.GLOWS, 1, 1, 1, 2, 0, 2, 14, 16, 14),
    GRANDFATHER_CLOCK("grandfather_clock", "Gothic Grandfather Clock", 4, F.GLOWS, 1, 2, 1, 3, 0, 2, 13, 32, 14),
    CRIMSON_CHANDELIER("crimson_chandelier", "Crimson Chandelier", 15, F.CEILING | F.GLOWS | F.LIGHTS, 1, 1, 1, 2, 2, 2, 14, 16, 14),
    BLOOD_PIPE_ORGAN("blood_pipe_organ", "Blood Pipe Organ", 4, F.GLOWS, 2, 2, 1, 1, 0, 0, 16, 32, 32),
    SECRET_BOOKSHELF_DOOR("secret_bookshelf_door", "Secret Bookshelf Door", 0, F.DOOR, 1, 2, 1, 0, 0, 0, 5, 32, 16),
    HAUNTED_PORTRAIT("haunted_portrait", "Haunted Portrait", 0, F.WALL | F.GLOWS | F.PASSABLE, 1, 1, 1, 14, 0, 1, 16, 16, 15),
    IRON_MAIDEN("iron_maiden", "Iron Maiden", 0, F.GLOWS, 1, 2, 1, 2, 0, 2, 14, 32, 14),
    BLOOD_MOON_ALTAR("blood_moon_altar", "Blood Moon Altar", 9, F.GLOWS, 2, 1, 1, 2, 0, 1, 14, 14, 31),
    BAT_ROOST("bat_roost", "Bat Roost", 0, F.GLOWS, 1, 1, 1, 4, 0, 4, 12, 16, 12),
    CRIMSON_CANOPY_BED("crimson_canopy_bed", "Four-Poster Crimson Bed", 0, F.BED, 1, 2, 2,
        1, 0, 1, 31, 9, 15, 0.4, 0, 0.2, 2.4, 32, 2.0, 0.4, 0, 14.0, 2.4, 32, 15.8, 29.6, 0, 0.2, 31.6, 32, 2.0,
        29.6, 0, 14.0, 31.6, 32, 15.8, 29.6, 9, 1, 31.4, 26, 15),
    BLOOD_CRYSTAL_FIREPLACE("blood_crystal_fireplace", "Blood Crystal Fireplace", 13, F.GLOWS | F.LIGHTS, 2, 2, 1,
        6, 0, 0, 16, 32, 32, 2, 0, 3, 6, 1, 29),
    // ── the Halloween ones (the Blood Merchant's Spooky tab, only in the Halloween weeks: BloodMerchantShop.SPOOKY) ──
    GRINNING_JACK_O_LANTERN("grinning_jack_o_lantern", "Grinning Jack-o'-Lantern", 15, F.GLOWS, 1, 1, 1, 2, 0, 2, 14, 13, 14),
    HAUNTED_TOMBSTONE("haunted_tombstone", "Haunted Tombstone", 0, F.GLOWS, 1, 1, 1, 6, 0, 1, 11, 15, 15),
    POSSESSED_SCARECROW("possessed_scarecrow", "Possessed Scarecrow", 4, F.GLOWS, 1, 2, 1, 6, 0, 6, 10, 32, 10);

    // flags
    static final class F {
        static final int PASSABLE = 1, WALL = 2, GLOWS = 4, BED = 8, CEILING = 16, DOOR = 32, LIGHTS = 64;
    }

    // the block and item id (capitalism:<id>), its texture (textures/entity/<id>.png) and icon
    public final String id;
    public final String title;
    // its light (for the chandelier and fireplace: only while lit)
    public final int light;
    // its footprint in blocks: to the right, up, toward the back
    public final int wide, tall, deep;
    // its solid shape over the whole footprint (facing west, pixels from the main block's corner; see above)
    public final List<AABB> boxes = new ArrayList<>();
    // nothing to bump into (the rug, the portrait)
    public final boolean passable;
    // hangs on the side of a solid block (the skull trophy, the portrait), facing out from it
    public final boolean wallMounted;
    // hangs from the underside of a solid block (the chandelier)
    public final boolean ceiling;
    // has glowing parts (a second, emissive pass with <id>_glow.png)
    public final boolean glows;
    // a bed you can sleep in, two blocks long (decor/BloodBedBlock: the coffin, the canopy bed)
    public final boolean bed;
    // two blocks tall, placed like a door (decor/BloodDoorBlock: the bookshelf door)
    public final boolean door;
    // lit and snuffed (BloodDecorBlock.LIT): gives its light only while lit
    public final boolean lights;

    BloodDecorKind(String id, String title, int light, int flags, int wide, int tall, int deep, double... px) {
        this.id = id;
        this.title = title;
        this.light = light;
        this.wide = wide;
        this.tall = tall;
        this.deep = deep;
        for (int i = 0; i + 5 < px.length; i += 6) this.boxes.add(new AABB(px[i], px[i + 1], px[i + 2], px[i + 3], px[i + 4], px[i + 5]));
        this.passable = (flags & F.PASSABLE) != 0;
        this.wallMounted = (flags & F.WALL) != 0;
        this.ceiling = (flags & F.CEILING) != 0;
        this.glows = (flags & F.GLOWS) != 0;
        this.bed = (flags & F.BED) != 0;
        this.door = (flags & F.DOOR) != 0;
        this.lights = (flags & F.LIGHTS) != 0;
    }

    public boolean multiBlock() {
        return this.wide * this.tall * this.deep > 1;
    }

    // ── the footprint ──

    // the block `cell` blocks over from the main one (x toward the back, y up, z toward the right), for a piece facing `front`
    public static BlockPos cellPos(BlockPos main, Direction front, int cx, int cy, int cz) {
        Direction back = front.getOpposite(), right = front.getCounterClockWise();
        return main.offset(back.getStepX() * cx + right.getStepX() * cz, cy, back.getStepZ() * cx + right.getStepZ() * cz);
    }

    // every block of the footprint but the main one, as {cx, cy, cz}
    public List<int[]> otherCells() {
        List<int[]> out = new ArrayList<>();
        for (int x = 0; x < this.deep; x++) for (int y = 0; y < this.tall; y++) for (int z = 0; z < this.wide; z++) {
            if (x != 0 || y != 0 || z != 0) out.add(new int[]{x, y, z});
        }
        return out;
    }

    // which block of the footprint `pos` is (cx, cy, cz), seen from the main one facing `front`
    public static int[] cellOf(BlockPos main, Direction front, BlockPos pos) {
        Direction back = front.getOpposite(), right = front.getCounterClockWise();
        int dx = pos.getX() - main.getX(), dz = pos.getZ() - main.getZ();
        return new int[]{dx * back.getStepX() + dz * back.getStepZ(), pos.getY() - main.getY(), dx * right.getStepX() + dz * right.getStepZ()};
    }

    private final Map<Integer, VoxelShape> cellShapes = new HashMap<>();

    // the solid shape of one block of the footprint, facing `front`
    public VoxelShape cellShape(int cx, int cy, int cz, Direction front) {
        return this.cellShape(this.boxes, cx, cy, cz, front, 0);
    }

    // ... of another set of boxes (the iron maiden's hollow); `salt` keeps their cache apart
    public VoxelShape cellShape(List<AABB> boxes, int cx, int cy, int cz, Direction front, int salt) {
        int key = (((salt * 4 + front.get2DDataValue()) * 4 + cx + 1) * 4 + cy + 1) * 4 + cz + 1;
        synchronized (this.cellShapes) {
            VoxelShape cached = this.cellShapes.get(key);
            if (cached != null) return cached;
        }
        VoxelShape west = Shapes.empty();
        AABB cell = new AABB(cx * 16, cy * 16, cz * 16, cx * 16 + 16, cy * 16 + 16, cz * 16 + 16);
        for (AABB b : boxes) {
            if (!b.intersects(cell)) continue;
            AABB in = b.intersect(cell).move(-cx * 16, -cy * 16, -cz * 16);
            if (in.getXsize() <= 0 || in.getYsize() <= 0 || in.getZsize() <= 0) continue;
            west = Shapes.or(west, Shapes.box(in.minX / 16, in.minY / 16, in.minZ / 16, in.maxX / 16, in.maxY / 16, in.maxZ / 16));
        }
        VoxelShape shape = BloodDecorBlock.turn(west.optimize(), front);
        synchronized (this.cellShapes) {
            this.cellShapes.put(key, shape);
        }
        return shape;
    }

    // what holds one of these (a block, a bed, the door)
    public interface Holder {
        BloodDecorKind kind();
    }
}
