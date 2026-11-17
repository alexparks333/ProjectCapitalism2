package com.alexh.capitalism.realestate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

// One lot of town land (RealEstate.md): a named rectangle (corners inclusive; the whole column, sky to bedrock) in the
// Overworld. The city owns it until a Real Estate Agent buys it at its City Chest (CityChestBlockEntity); its owner
// can build on it, and an agent who owns it can sell it or lease it out (salePrice and rent are what its For Sale / For
// Lease signs ask; a tenant can build on it too and pays the rent every Minecraft day, paidThrough). Prices in gold
// nuggets (economy/GoldMoney), -1 when there's none.
public final class Property {

    public static final Codec<Property> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.STRING.fieldOf("name").forGetter(p -> p.name),
        Codec.INT.fieldOf("x1").forGetter(p -> p.minX),
        Codec.INT.fieldOf("z1").forGetter(p -> p.minZ),
        Codec.INT.fieldOf("x2").forGetter(p -> p.maxX),
        Codec.INT.fieldOf("z2").forGetter(p -> p.maxZ),
        UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(p -> Optional.ofNullable(p.owner)),
        Codec.STRING.optionalFieldOf("owner_name", "").forGetter(p -> p.ownerName),
        UUIDUtil.CODEC.optionalFieldOf("tenant").forGetter(p -> Optional.ofNullable(p.tenant)),
        Codec.STRING.optionalFieldOf("tenant_name", "").forGetter(p -> p.tenantName),
        Codec.LONG.optionalFieldOf("sale_price", -1L).forGetter(p -> p.salePrice),
        Codec.LONG.optionalFieldOf("rent", -1L).forGetter(p -> p.rent),
        Codec.LONG.optionalFieldOf("paid_through", -1L).forGetter(p -> p.paidThrough)
    ).apply(inst, (name, x1, z1, x2, z2, owner, ownerName, tenant, tenantName, salePrice, rent, paidThrough) -> {
        Property p = new Property(name, x1, z1, x2, z2);
        p.owner = owner.orElse(null);
        p.ownerName = ownerName;
        p.tenant = tenant.orElse(null);
        p.tenantName = tenantName;
        p.salePrice = salePrice;
        p.rent = rent;
        p.paidThrough = paidThrough;
        return p;
    }));

    public final String name;
    public final int minX;
    public final int minZ;
    public final int maxX;
    public final int maxZ;
    @Nullable UUID owner;
    String ownerName = "";
    @Nullable UUID tenant;
    String tenantName = "";
    long salePrice = -1;
    long rent = -1;
    // the last Minecraft day the rent's paid for
    long paidThrough = -1;

    public Property(String name, int x1, int z1, int x2, int z2) {
        this.name = name;
        this.minX = Math.min(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= this.minX && pos.getX() <= this.maxX && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
    }

    public boolean overlaps(Property other) {
        return this.minX <= other.maxX && other.minX <= this.maxX && this.minZ <= other.maxZ && other.minZ <= this.maxZ;
    }

    public int width() {
        return this.maxX - this.minX + 1;
    }

    public int length() {
        return this.maxZ - this.minZ + 1;
    }

    public boolean cityOwned() {
        return this.owner == null;
    }

    public boolean ownedBy(Player player) {
        return player.getUUID().equals(this.owner);
    }

    public boolean leasedTo(Player player) {
        return player.getUUID().equals(this.tenant);
    }

    public boolean leased() {
        return this.tenant != null;
    }

    // Who may build here: its owner and its tenant
    public boolean canBuild(Player player) {
        return this.ownedBy(player) || this.leasedTo(player);
    }

    public @Nullable UUID owner() {
        return this.owner;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public @Nullable UUID tenant() {
        return this.tenant;
    }

    public String tenantName() {
        return this.tenantName;
    }

    public long salePrice() {
        return this.salePrice;
    }

    public long rent() {
        return this.rent;
    }
}
