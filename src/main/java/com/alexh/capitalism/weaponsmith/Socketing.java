package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

// Molten Sockets: a rare, secret upgrade for swords, pickaxes and armor. Everything is in Sockets.md.
//  - Found: a 1% drop when a player kills a really hard mob (DROPPED_BY: Wither Skeletons, Endermen, Hoglins...),
//    2% with an Emerald sword; the bosses (the Wither, the Ender Dragon) always drop one.
//    Its tooltip only says to take it to a smith.
//  - Set: at the smith table for the item (sword: Weaponsmith, pickaxe: Toolsmith, armor: Armorsmith), item in the big
//    slot + Molten Socket in the small one = the same item with an empty socket, for SOCKET_XP levels
//    (SmithTableMenu). One socket per item. Stored as ModComponents.SOCKET ("empty" or the gem's id).
//  - Gems: set by the item's smith at their table (SmithTableMenu): the socketed item in the big slot + a gem that fits
//    in the small one, for GEM_XP levels. The item alone in the big slot (a gem in it) = the same item with an empty
//    socket: the gem is destroyed. (Only the Smith's Chisel works in an inventory: the socket pops out, gem and all.)
//    Some gems only fit some items (Gem.fits).
//      Lapis:      sword +50% XP from kills, pickaxe +50% XP from mining, armor +20% XP from kills each. They add up.
//      Redstone:   sword 75% less recharge per swing (4x attack speed), pickaxe +20% mining speed, armor +5% speed each.
//      Emerald:    sword 2% Molten Socket drops, pickaxe 7.5% bonus Compacted Iron / Diamond from iron / diamond ore,
//                  armor 7.5% off NPC shop prices each (economy/NpcPricing).
//      Quartz:     sword only: critical hits do 2x damage instead of 1.5x.
//      Prismarine: armor only: +8% swim speed each.
//      Echo Shard: helmet only: hostile mobs within 24 blocks glow through walls, for you only (mixin/MinecraftMixin).
//      Ice:        armor only (one piece is enough): you can't get hot, but the cold bites harder (BodyTemperature).
//      Blaze:      armor only (one piece is enough): the cold can't reach you, but somewhere hot you burn.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Socketing {

    public static final int SOCKET_XP = 30;
    public static final float DROP_CHANCE = 0.01F;
    public static final float EMERALD_DROP_CHANCE = 0.02F;
    public static final float EMERALD_COMPACTED_CHANCE = 0.075F;
    public static final double EMERALD_DISCOUNT_PER_PIECE = 0.075;
    public static final float QUARTZ_CRIT_MULTIPLIER = 2.0F;
    public static final double ECHO_RANGE = 24.0;
    public static final String EMPTY = "empty";
    private static final double LAPIS_XP_BONUS = 0.5;
    // Lapis in armor: +20% kill XP per piece (a Lapis sword or pickaxe is +50%)
    private static final double LAPIS_ARMOR_XP_BONUS = 0.2;
    // Redstone in a sword: recharge time x0.25 (75% less) = attack speed x4
    private static final double SWORD_ATTACK_SPEED_BONUS = 3.0;
    private static final double PRISMARINE_SWIM_BONUS = 0.08;
    private static final double AMETHYST_KNOCKBACK = 0.05;
    public static final float PRISMARINE_UNDERWATER_HIT = 1.5F, PRISMARINE_UNDERWATER_MINING = 3.0F, QUARTZ_STONE_MINING = 1.25F;
    public static final float GHAST_TEAR_NETHER_DAMAGE = 1.2F, ICE_SLOW_CHANCE = 0.2F, FLINT_BLEED_CHANCE = 0.25F;
    public static final float BONE_PROJECTILE_PER_PIECE = 0.05F, AMETHYST_KILL_HEAL = 1.0F;
    public static final int GLOWSTONE_GLOW_TICKS = 10 * 20, ICE_SLOW_TICKS = 3 * 20, FLINT_BLEED_TICKS = 4 * 20, DIAMOND_PLANKS = 8;

    // The really hard mobs that can drop one
    private static final Set<EntityType<?>> DROPPED_BY = Set.of(EntityTypes.WITHER_SKELETON, EntityTypes.ENDERMAN,
        EntityTypes.HOGLIN, EntityTypes.ZOGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.RAVAGER, EntityTypes.EVOKER,
        EntityTypes.ELDER_GUARDIAN, EntityTypes.WARDEN, EntityTypes.WITHER);

    // Bosses always drop one, whoever (or whatever) killed them
    private static final Set<EntityType<?>> BOSSES = Set.of(EntityTypes.WITHER, EntityTypes.ENDER_DRAGON);

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    // Where a gem can go (a bitmask of these)
    public static final int SWORD = 1, PICKAXE = 2, AXE = 4, HELMET = 8, CHEST = 16, LEGS = 32, BOOTS = 64;
    public static final int ARMOR = HELMET | CHEST | LEGS | BOOTS;

    public enum Gem {
        LAPIS("lapis", Items.LAPIS_LAZULI, "Lapis", SWORD | PICKAXE | ARMOR, ChatFormatting.BLUE, 0xFF3A6CF0),
        REDSTONE("redstone", Items.REDSTONE, "Redstone", SWORD | PICKAXE | ARMOR, ChatFormatting.RED, 0xFFE02020),
        EMERALD("emerald", Items.EMERALD, "Emerald", SWORD | PICKAXE | ARMOR, ChatFormatting.GREEN, 0xFF2BD160),
        QUARTZ("quartz", Items.QUARTZ, "Quartz", SWORD | PICKAXE, ChatFormatting.WHITE, 0xFFEDE6DC),
        PRISMARINE("prismarine", Items.PRISMARINE_SHARD, "Prismarine", SWORD | PICKAXE | ARMOR, ChatFormatting.AQUA, 0xFF5FC9B6),
        ECHO("echo", Items.ECHO_SHARD, "Echo Shard", HELMET, ChatFormatting.DARK_AQUA, 0xFF1B7E8E),
        // a Farmer's butchering sword: more Meat Slabs (farmer/MeatCut.BONE_CHANCE); armor: projectile protection
        BONE("bone", Items.BONE, "Bone", SWORD | ARMOR, ChatFormatting.GRAY, 0xFFE8E0C8),
        // armor: body heat (player/BodyTemperature): Ice = never hot but colder in the cold, Blaze Powder = never cold but
        // burning somewhere hot. Sword: Ice slows, Blaze Powder lights. Pickaxe: Blaze Powder lights. Axe: Blaze Powder
        // turns logs to charcoal.
        ICE("ice", Items.ICE, "Ice", SWORD | ARMOR, ChatFormatting.AQUA, 0xFFA8DCFF),
        BLAZE("blaze", Items.BLAZE_POWDER, "Blaze Powder", SWORD | PICKAXE | AXE | ARMOR, ChatFormatting.GOLD, 0xFFFFA020),
        AMETHYST("amethyst", Items.AMETHYST_SHARD, "Amethyst", SWORD | ARMOR, ChatFormatting.LIGHT_PURPLE, 0xFFB57BE8),
        GLOWSTONE("glowstone", Items.GLOWSTONE_DUST, "Glowstone", SWORD, ChatFormatting.YELLOW, 0xFFFFE070),
        GHAST_TEAR("ghast_tear", Items.GHAST_TEAR, "Ghast Tear", SWORD, ChatFormatting.WHITE, 0xFFE8F4F4),
        SLIME("slime", Items.SLIME_BALL, "Slime Ball", BOOTS, ChatFormatting.GREEN, 0xFF7ED46A),
        RABBIT_FOOT("rabbit_foot", Items.RABBIT_FOOT, "Rabbit's Foot", ARMOR, ChatFormatting.GOLD, 0xFFD8B080),
        DIAMOND("diamond", Items.DIAMOND, "Diamond", AXE, ChatFormatting.AQUA, 0xFF5EE8E0),
        FLINT("flint", Items.FLINT, "Flint", AXE, ChatFormatting.DARK_GRAY, 0xFF4A4A50);

        public final String id;
        public final Item item;
        public final String title;
        public final int fits;
        public final ChatFormatting color;
        // its color in the little socket icon on the item
        public final int iconColor;

        Gem(String id, Item item, String title, int fits, ChatFormatting color, int iconColor) {
            this.id = id;
            this.item = item;
            this.title = title;
            this.fits = fits;
            this.color = color;
            this.iconColor = iconColor;
        }

        public boolean fits(ItemStack target) {
            return (this.fits & kind(target)) != 0;
        }

        // "a sword, pickaxe or armor", "boots", ...
        String fitsWhere() {
            List<String> where = new java.util.ArrayList<>();
            if ((this.fits & SWORD) != 0) where.add("a sword");
            if ((this.fits & PICKAXE) != 0) where.add("a pickaxe");
            if ((this.fits & AXE) != 0) where.add("an axe");
            if ((this.fits & ARMOR) == ARMOR) where.add("armor");
            else if ((this.fits & HELMET) != 0) where.add("a helmet");
            else if ((this.fits & BOOTS) != 0) where.add("boots");
            if (where.size() == 1) return where.getFirst();
            return String.join(", ", where.subList(0, where.size() - 1)) + " or " + where.getLast();
        }

        static @Nullable Gem byId(String id) {
            for (Gem gem : values()) {
                if (gem.id.equals(id)) return gem;
            }
            return null;
        }

        public static @Nullable Gem byItem(ItemStack stack) {
            // any ice fits the Ice socket (it holds as Ice: taking it out gives a plain Ice block back)
            if (stack.is(Items.PACKED_ICE) || stack.is(Items.BLUE_ICE)) return ICE;
            for (Gem gem : values()) {
                if (stack.is(gem.item)) return gem;
            }
            return null;
        }
    }

    // What kind of socketable item this is (SWORD, PICKAXE, AXE or the armor slot), 0 = none
    public static int kind(ItemStack item) {
        if (isMeleeWeapon(item)) return SWORD;
        if (Wear.isPickaxe(item)) return PICKAXE;
        if (isAxe(item)) return AXE;
        if (!Wear.isArmor(item)) return 0;
        if (item.is(ItemTags.HEAD_ARMOR)) return HELMET;
        if (item.is(ItemTags.CHEST_ARMOR)) return CHEST;
        if (item.is(ItemTags.LEG_ARMOR)) return LEGS;
        if (item.is(ItemTags.FOOT_ARMOR)) return BOOTS;
        return 0;
    }

    // The gems that fit this item, for the empty socket's tooltip: "Lapis, Redstone, Emerald, ..."
    public static String gemsFor(ItemStack item) {
        List<String> names = new java.util.ArrayList<>();
        for (Gem gem : Gem.values()) {
            if (gem.fits(item)) names.add(gem.title);
        }
        return String.join(", ", names);
    }

    // Swords, Maces and Tridents: socketed at the Weaponsmith Table, and they take the sword gems
    public static boolean isMeleeWeapon(ItemStack item) {
        return Sharpness.isSword(item) || item.is(Items.MACE) || item.is(Items.TRIDENT);
    }

    public static boolean isAxe(ItemStack item) {
        return item.is(ItemTags.AXES);
    }
    private Socketing() {
    }

    // ── What can have a socket, and what's in it ──

    // Which table sets a socket in what: swords at the Weaponsmith Table, pickaxes and axes at the Toolsmith Table, armor at the
    // Armorsmith Table
    public static boolean socketable(SmithTable kind, ItemStack item) {
        return switch (kind) {
            case WEAPONSMITH -> isMeleeWeapon(item);
            case TOOLSMITH -> Wear.isPickaxe(item) || isAxe(item);
            case ARMORSMITH -> Wear.isArmor(item);
        };
    }

    public static boolean hasSocket(ItemStack item) {
        return item.has(ModComponents.SOCKET.get());
    }

    public static @Nullable Gem gem(ItemStack item) {
        String inside = item.get(ModComponents.SOCKET.get());
        return inside == null ? null : Gem.byId(inside);
    }

    // How many of the armor pieces this entity wears hold this gem
    public static int armorWith(LivingEntity entity, Gem gem) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (gem(entity.getItemBySlot(slot)) == gem) count++;
        }
        return count;
    }

    // At a smith table: the item + a Molten Socket
    public static boolean canApply(SmithTable kind, ItemStack item, ItemStack socket) {
        return socket.is(ModItems.MOLTEN_SOCKET.get()) && socketable(kind, item) && !hasSocket(item);
    }

    public static ItemStack withSocket(ItemStack item) {
        ItemStack copy = item.copyWithCount(1);
        copy.set(ModComponents.SOCKET.get(), EMPTY);
        return copy;
    }

    // ── Gems are set by the item's smith, at their table (SmithTableMenu) ──
    // Setting a gem costs this many XP levels; breaking one out is free (the gem is destroyed)
    public static final int GEM_XP = 1;

    // At this smith's table: a socketed item with an empty socket + a gem that fits it
    public static boolean canSetGem(SmithTable kind, ItemStack item, ItemStack gemStack) {
        Gem gem = Gem.byItem(gemStack);
        return gem != null && socketable(kind, item) && hasSocket(item) && gem(item) == null && gem.fits(item);
    }

    // At this smith's table: a socketed item holding a gem, alone (nothing in the small slot): break the gem out
    public static boolean canBreakGem(SmithTable kind, ItemStack item, ItemStack small) {
        return small.isEmpty() && socketable(kind, item) && gem(item) != null;
    }

    public static ItemStack withGem(ItemStack item, ItemStack gemStack) {
        ItemStack copy = item.copyWithCount(1);
        Gem gem = Gem.byItem(gemStack);
        if (gem != null) copy.set(ModComponents.SOCKET.get(), gem.id);
        return copy;
    }

    public static ItemStack withEmptySocket(ItemStack item) {
        ItemStack copy = item.copyWithCount(1);
        copy.set(ModComponents.SOCKET.get(), EMPTY);
        return copy;
    }

    // What the gem does in this item, for the tooltip
    public static String effect(Gem gem, ItemStack item) {
        int kind = kind(item);
        boolean armor = (kind & ARMOR) != 0;
        boolean pickaxe = kind == PICKAXE;
        boolean axe = kind == AXE;
        return switch (gem) {
            case LAPIS -> pickaxe ? "+50% XP from mining" : armor ? "+20% XP from kills" : "+50% XP from kills";
            case REDSTONE -> armor ? "+5% movement speed" : pickaxe ? "+20% mining speed" : "75% less swing recharge time";
            case EMERALD -> armor ? "7.5% off NPC shop prices" : pickaxe ? "7.5% chance of a bonus Compacted Iron / Diamond from ore"
                : "Molten Sockets drop 2% of the time (not 1%)";
            case QUARTZ -> pickaxe ? "Stone and deepslate break 25% faster" : "Critical hits do 2x damage (not 1.5x)";
            case PRISMARINE -> pickaxe ? "Mine 3x as fast underwater" : armor ? "+8% swim speed" : "Hits 50% harder underwater";
            case ECHO -> "Hostile mobs within 24 blocks glow through walls";
            case BONE -> armor ? "5% less damage from arrows and other projectiles" : "+15% chance of a Meat Slab (a Farmer's kills)";
            case ICE -> armor ? "You can't get hot, but the cold bites harder" : "20% chance to slow what you hit";
            case BLAZE -> armor ? "The cold can't reach you, but heat burns hotter" : axe ? "Logs you chop come out as Charcoal"
                : "Lights up the area around you like a torch while held";
            case AMETHYST -> armor ? "+5% knockback resistance" : "Each kill heals half a heart";
            case GLOWSTONE -> "What you hit glows for 10 seconds";
            case GHAST_TEAR -> "+20% damage to Nether mobs";
            case SLIME -> "No fall damage";
            case RABBIT_FOOT -> "+1 Luck: better fishing (rare catches too) and loot";
            case DIAMOND -> "Each log you chop splits into 8 planks";
            case FLINT -> "25% chance to make what you hit bleed";
        };
    }

    // ── In an inventory: only the Smith's Chisel. (Gems are set and broken out by the item's smith at their table,
    // SmithTableMenu; right-clicking an item with a gem only says so.) ──

    @SubscribeEvent
    public static void onStackedOn(ItemStackedOnOtherEvent event) {
        ItemStack target = event.getStackedOnItem();
        if (event.getClickAction() != ClickAction.SECONDARY || !hasSocket(target)) return;
        ItemStack carried = event.getCarriedItem();
        Gem inside = gem(target);
        Player player = event.getPlayer();
        // the Smith's Chisel (only from the Traveling Merchant): the socket comes back out as a Molten Socket, any gem
        // with it; the chisel is used up
        if (carried.is(ModItems.SMITHS_CHISEL.get())) {
            event.cancelWithResult(true);
            target.remove(ModComponents.SOCKET.get());
            event.getCarriedSlotAccess().set(new ItemStack(ModItems.MOLTEN_SOCKET.get()));
            if (inside != null && !player.getInventory().add(new ItemStack(inside.item))) player.drop(new ItemStack(inside.item), false, net.minecraft.util.Prediction.SERVER_ONLY);
            event.getSlot().setChanged();
            player.playSound(SoundEvents.ANVIL_USE, 0.6F, 1.4F);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(Component.literal("The Molten Socket pops free."));
            return;
        }
        // a gem on the cursor: point them to the smith (and don't let the gem drop into the item's slot)
        if (!carried.isEmpty() && Gem.byItem(carried) != null) {
            event.cancelWithResult(true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.literal("Gems are set by " + smithFor(target) + " at their table."));
            }
        }
    }

    // who sets gems in this item: "a Weaponsmith", "a Toolsmith" or "an Armorsmith"
    public static String smithFor(ItemStack item) {
        return isMeleeWeapon(item) ? "a Weaponsmith" : Wear.isArmor(item) ? "an Armorsmith" : "a Toolsmith";
    }

    // ── Redstone and Prismarine: speed (shows in the item's attribute lines too) ──

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack item = event.getItemStack();
        Gem gem = gem(item);
        if (gem == Gem.REDSTONE) {
            if (isMeleeWeapon(item)) {
                // 75% less time to recharge each swing = 4x attack speed (attack speed is hits per second)
                event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(id("socket_redstone"), SWORD_ATTACK_SPEED_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
            } else if (Wear.isPickaxe(item)) {
                event.addModifier(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(id("socket_redstone"), 0.20,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
            } else {
                addArmorModifier(event, item, Attributes.MOVEMENT_SPEED, "socket_redstone_", 0.05);
            }
        } else if (gem == Gem.PRISMARINE) {
            // +8% per piece; each slot has its own modifier, so they add up (4 pieces = +32%)
            addArmorModifier(event, item, NeoForgeMod.SWIM_SPEED, "socket_prismarine_", PRISMARINE_SWIM_BONUS);
        } else if (gem == Gem.AMETHYST) {
            // +5% knockback resistance per piece (base is 0, so it's added straight on)
            addArmorModifier(event, item, Attributes.KNOCKBACK_RESISTANCE, "socket_amethyst_", AMETHYST_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE);
        } else if (gem == Gem.RABBIT_FOOT) {
            // +1 Luck per piece: loot and fishing (the rare catches read the cast's Luck, fishing/RareCatchModifier)
            addArmorModifier(event, item, Attributes.LUCK, "socket_rabbit_foot_", 1.0, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    // A "+x% of base" modifier for whichever armor slot this piece goes in (one id per slot, so pieces add up)
    private static void addArmorModifier(ItemAttributeModifierEvent event, ItemStack piece,
                                         net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                         String idPrefix, double amount) {
        addArmorModifier(event, piece, attribute, idPrefix, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void addArmorModifier(ItemAttributeModifierEvent event, ItemStack piece,
                                         net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                         String idPrefix, double amount, AttributeModifier.Operation operation) {
        if (!Wear.isArmor(piece)) return;
        Equippable equippable = piece.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;
        EquipmentSlot slot = equippable.slot();
        event.addModifier(attribute, new AttributeModifier(id(idPrefix + slot.getName()), amount, operation), EquipmentSlotGroup.bySlot(slot));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Capitalism.MODID, path);
    }

    // ── Quartz: harder critical hits ──

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        if (!event.isCriticalHit()) return;
        ItemStack sword = event.getEntity().getMainHandItem();
        if (!isMeleeWeapon(sword) || gem(sword) != Gem.QUARTZ) return;
        // vanilla crits are 1.5x: make it 2x (on top of anything else that changed it)
        event.setDamageMultiplier(event.getDamageMultiplier() * QUARTZ_CRIT_MULTIPLIER / 1.5F);
    }

    // ── Lapis: more XP ──

    // Kills: a Lapis sword in hand +50%, every Lapis armor piece worn +20%, all added together
    @SubscribeEvent
    public static void onKillXp(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null || event.getDroppedExperience() <= 0) return;
        double bonus = LAPIS_ARMOR_XP_BONUS * armorWith(player, Gem.LAPIS);
        ItemStack hand = player.getMainHandItem();
        if (isMeleeWeapon(hand) && gem(hand) == Gem.LAPIS) bonus += LAPIS_XP_BONUS;
        if (bonus > 0) event.setDroppedExperience(scale(event.getDroppedExperience(), 1 + bonus, player.getRandom()));
    }

    // Mining with a socketed pickaxe: Lapis +50% XP; Emerald a 7.5% chance of a bonus Compacted Iron / Diamond from
    // iron / diamond ore (not when Silk Touch drops the ore itself, so it can't be farmed by placing it again)
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!Wear.isPickaxe(tool)) return;
        Gem gem = gem(tool);
        if (gem == Gem.LAPIS && event.getDroppedExperience() > 0) {
            event.setDroppedExperience(scale(event.getDroppedExperience(), 1 + LAPIS_XP_BONUS, event.getLevel().getRandom()));
        } else if (gem == Gem.EMERALD) {
            BlockState state = event.getState();
            Item compacted = state.is(BlockTags.IRON_ORES) ? ModItems.COMPACTED_IRON.get()
                : state.is(BlockItemTags.DIAMOND_ORES.block()) ? ModItems.COMPACTED_DIAMOND.get() : null;
            if (compacted == null || event.getLevel().getRandom().nextFloat() >= EMERALD_COMPACTED_CHANCE) return;
            Item oreItem = state.getBlock().asItem();
            for (ItemEntity drop : event.getDrops()) {
                if (drop.getItem().is(oreItem)) return; // Silk Touch
            }
            BlockPos pos = event.getPos();
            event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(compacted)));
        }
    }

    // 3 XP x 1.5 = 4.5: 4, plus 1 more half the time
    private static int scale(int xp, double multiplier, RandomSource random) {
        double scaled = xp * multiplier;
        int whole = (int) scaled;
        return whole + (random.nextDouble() < scaled - whole ? 1 : 0);
    }

    // ── The drop ──

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!BOSSES.contains(mob.getType())) {
            if (!DROPPED_BY.contains(mob.getType()) || !(event.getSource().getEntity() instanceof Player killer)) return;
            ItemStack weapon = killer.getMainHandItem();
            float chance = isMeleeWeapon(weapon) && gem(weapon) == Gem.EMERALD ? EMERALD_DROP_CHANCE : DROP_CHANCE;
            if (mob.getRandom().nextFloat() >= chance) return;
        }
        event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(ModItems.MOLTEN_SOCKET.get())));
    }
}
