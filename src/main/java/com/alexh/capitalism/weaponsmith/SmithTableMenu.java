package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.guide.Discoveries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// A smith table (SmithTable: Weaponsmith Table, Toolsmith Table): a big slot (the weapon, or a Grindstone...) and a
// small slot (the material or ingredient), a result slot, a fuel slot and a lava tank.
//  - Recipes (SmithTable.recipes()): big + small -> result, burning some lava.
//  - Repairs (Weaponsmith Table): a damaged weapon + its repair material lights up the Forge button. The client plays
//    the dent game (client/SmithTableScreen) and sends its score as a menu button (BUTTON_FORGE + hits); the server
//    checks everything again, takes the material, lava and XP, and repairs the weapon in place (DentForging).
// A Lava Bucket in the fuel slot is poured into the tank (you get the empty Bucket back). The lava stays in the block
// (SmithTableBlockEntity); whatever is left in the slots goes back to you when you close it.
public class SmithTableMenu extends AbstractContainerMenu {

    public static final int BIG = 0;
    public static final int SMALL = 1;
    private static final int INPUTS = 2;
    public static final int RESULT = INPUTS;
    public static final int FUEL = INPUTS + 1;
    private static final int INV_START = FUEL + 1;
    private static final int INV_END = INV_START + 36;
    // Menu button ids: BUTTON_FORGE + how many dents were hit (0..DentForging.dents(weapon))
    public static final int BUTTON_FORGE = 100;
    // Menu button id: the Salvage button
    public static final int BUTTON_SALVAGE = 50;
    // Synced to the client: the lava, whether a recipe is short of lava, and whether the Forge button can be used
    private static final int DATA_LAVA = 0;
    private static final int DATA_NEEDS_LAVA = 1;
    private static final int DATA_FORGE = 2;
    private static final int DATA_SALVAGE = 3;
    private static final int DATA_SOCKET = 4;
    private static final int DATA_COUNT = 5;
    // DATA_SOCKET values
    public static final int SOCKET_NONE = 0;
    public static final int SOCKET_READY = 1;
    public static final int SOCKET_NEEDS_XP = 2;
    // setting a gem in a socket (GEM_XP levels), or breaking one out (free; the gem is destroyed)
    public static final int GEM_READY = 3;
    public static final int GEM_NEEDS_XP = 4;
    public static final int GEM_BREAK = 5;

    private final SmithTable kind;
    private final ContainerLevelAccess access;
    private final @Nullable SmithTableBlockEntity table;
    private final Player player;
    private final ContainerData data;
    private boolean needsLava;
    private boolean refreshingSocket;
    private final SimpleContainer inputs = new SimpleContainer(INPUTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmithTableMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer fuel = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmithTableMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    // Client side: the lava comes from the server through the data slots
    public SmithTableMenu(SmithTable kind, int containerId, Inventory inventory) {
        this(kind, containerId, inventory, ContainerLevelAccess.NULL, null);
    }

    public SmithTableMenu(SmithTable kind, int containerId, Inventory inventory, ContainerLevelAccess access, @Nullable SmithTableBlockEntity table) {
        super(kind.menuType(), containerId);
        this.kind = kind;
        this.access = access;
        this.table = table;
        this.player = inventory.player;
        this.data = table == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_LAVA -> table.lava();
                    case DATA_NEEDS_LAVA -> SmithTableMenu.this.needsLava ? 1 : 0;
                    case DATA_SALVAGE -> SmithTableMenu.this.salvageState().ordinal();
                    case DATA_SOCKET -> SmithTableMenu.this.socketState();
                    default -> SmithTableMenu.this.forgeState().ordinal();
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        // the big slot (a 26x26 frame in the texture) and the small one
        this.addSlot(new Slot(this.inputs, BIG, 19, 35) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.inputs, SMALL, 61, 35));
        this.addSlot(new Slot(this.result, 0, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            // setting a Molten Socket costs XP levels: no taking it without them
            @Override
            public boolean mayPickup(Player taker) {
                return !SmithTableMenu.this.isSocketing() || canAffordSocket(taker);
            }

            @Override
            public void onTake(Player taker, ItemStack carried) {
                SmithTableMenu.this.craft(taker, carried);
                super.onTake(taker, carried);
            }
        });
        this.addSlot(new Slot(this.fuel, 0, 80, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SmithTableBlockEntity.fuelValue(stack) > 0;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(this.data);
    }

    public SmithTable kind() {
        return this.kind;
    }

    public int lava() {
        return this.data.get(DATA_LAVA);
    }

    // The items make something, but the tank doesn't have enough lava for it
    public boolean needsLava() {
        return this.data.get(DATA_NEEDS_LAVA) != 0;
    }

    // Client: whether the Forge button can be pressed (or why not)
    public DentForging.State forge() {
        return DentForging.State.values()[Math.clamp(this.data.get(DATA_FORGE), 0, DentForging.State.values().length - 1)];
    }

    // Client: SOCKET_NONE, SOCKET_READY or SOCKET_NEEDS_XP (setting a Molten Socket)
    public int socket() {
        return this.data.get(DATA_SOCKET);
    }

    private int socketState() {
        if (this.table == null) return SOCKET_NONE;
        if (this.isSettingGem()) return canAffordGem(this.player) ? GEM_READY : GEM_NEEDS_XP;
        if (this.isBreakingGem()) return GEM_BREAK;
        if (!this.isSocketing()) return SOCKET_NONE;
        return canAffordSocket(this.player) ? SOCKET_READY : SOCKET_NEEDS_XP;
    }

    private static boolean canAffordGem(Player player) {
        return player.isCreative() || player.experienceLevel >= Socketing.GEM_XP;
    }

    // the socketed item + a gem that fits it (this table's kind of item). A repair comes first: a damaged diamond axe +
    // a Diamond is a repair (Forge), not a Diamond gem; repair it, then set the gem.
    private boolean isSettingGem() {
        ItemStack big = this.inputs.getItem(BIG), small = this.inputs.getItem(SMALL);
        return Socketing.canSetGem(this.kind, big, small) && !DentForging.isRepair(this.kind, big, small);
    }

    // a socketed item holding a gem, alone in the big slot
    private boolean isBreakingGem() {
        return Socketing.canBreakGem(this.kind, this.inputs.getItem(BIG), this.inputs.getItem(SMALL));
    }

    private static boolean canAffordSocket(Player player) {
        return player.isCreative() || player.experienceLevel >= Socketing.SOCKET_XP;
    }

    // Client: whether the Salvage button can be pressed (or why not)
    public Salvaging.State salvage() {
        return Salvaging.State.values()[Math.clamp(this.data.get(DATA_SALVAGE), 0, Salvaging.State.values().length - 1)];
    }

    // Server: can the gear in the big slot be salvaged with the hammer in the small one?
    private Salvaging.State salvageState() {
        if (this.table == null) return Salvaging.State.NONE;
        return Salvaging.state(this.inputs.getItem(BIG), this.inputs.getItem(SMALL), this.table.lava());
    }

    // Server: can the items in the slots be forged right now?
    private DentForging.State forgeState() {
        if (this.table == null) return DentForging.State.NONE;
        return DentForging.state(this.kind, this.inputs.getItem(BIG), this.inputs.getItem(SMALL), this.table.lava(), this.player);
    }

    // Server only: pour in a Lava Bucket (if it fits), then work out the result
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.table == null) return;
        if (this.table.pour(SmithTableBlockEntity.fuelValue(this.fuel.getItem(0)))) {
            this.fuel.setItem(0, new ItemStack(Items.BUCKET));
            this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F));
            return; // setItem calls slotsChanged again
        }
        // a sword / tool + the Reroll Token of its material: the result is the item with its stats wiped (rolled on take)
        if (this.isReroll()) {
            boolean enoughLava = this.table.lava() >= Rerolling.REROLL_LAVA;
            this.needsLava = !enoughLava;
            this.result.setItem(0, enoughLava ? Rerolling.preview(this.inputs.getItem(BIG)) : ItemStack.EMPTY);
            return;
        }
        // a socketed item + a gem that fits: the same item with the gem set, for GEM_XP levels (no lava)
        if (this.isSettingGem()) {
            this.needsLava = false;
            this.result.setItem(0, canAffordGem(this.player) ? Socketing.withGem(this.inputs.getItem(BIG), this.inputs.getItem(SMALL)) : ItemStack.EMPTY);
            return;
        }
        // a socketed item holding a gem, alone: the same item with an empty socket (the gem is broken out and lost)
        if (this.isBreakingGem()) {
            this.needsLava = false;
            this.result.setItem(0, Socketing.withEmptySocket(this.inputs.getItem(BIG)));
            return;
        }
        // a sword / pickaxe / armor piece + a Molten Socket: the same item with an empty socket, for XP levels (no lava)
        if (this.isSocketing()) {
            this.needsLava = false;
            this.result.setItem(0, canAffordSocket(this.player) ? Socketing.withSocket(this.inputs.getItem(BIG)) : ItemStack.EMPTY);
            return;
        }
        WeaponsmithTableRecipes.Recipe recipe = this.recipe();
        boolean affordable = recipe != null && this.table.lava() >= recipe.lavaCost();
        this.needsLava = recipe != null && !affordable;
        this.result.setItem(0, affordable ? new ItemStack(recipe.result()) : ItemStack.EMPTY);
    }

    private boolean isSocketing() {
        return Socketing.canApply(this.kind, this.inputs.getItem(BIG), this.inputs.getItem(SMALL));
    }

    private boolean isReroll() {
        return Rerolling.canReroll(this.kind, this.inputs.getItem(BIG), this.inputs.getItem(SMALL));
    }

    private WeaponsmithTableRecipes.@Nullable Recipe recipe() {
        return WeaponsmithTableRecipes.find(this.kind.recipes(), this.inputs.getItem(BIG), this.inputs.getItem(SMALL));
    }

    private void craft(Player taker, ItemStack carried) {
        WeaponsmithTableRecipes.Recipe recipe = this.recipe();
        boolean settingGem = this.isSettingGem(), breakingGem = this.isBreakingGem();
        boolean socketing = !settingGem && !breakingGem && this.isSocketing();
        int lava = socketing || settingGem || breakingGem ? 0 : this.isReroll() ? Rerolling.REROLL_LAVA : recipe != null ? recipe.lavaCost() : 0;
        if (socketing && !taker.isCreative()) taker.giveExperienceLevels(-Socketing.SOCKET_XP);
        if (settingGem && !taker.isCreative()) taker.giveExperienceLevels(-Socketing.GEM_XP);
        if (this.table != null) this.table.burn(lava);
        Rerolling.roll(carried, taker);
        this.inputs.removeItem(BIG, 1);
        this.inputs.removeItem(SMALL, 1);
        Discoveries.discover(taker, carried);
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F));
        // a bucket waiting in the fuel slot may fit now
        this.slotsChanged(this.fuel);
    }

    // The dent game is over: the client tells us how many dents were hit. Check it all again, then repair.
    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == BUTTON_SALVAGE) return this.salvage(clicker);
        if (id < BUTTON_FORGE || id > BUTTON_FORGE + DentForging.MAX_DENTS) return false;
        if (this.table == null || !(clicker instanceof ServerPlayer serverPlayer)) return true;
        if (this.forgeState() != DentForging.State.READY) return false;
        int hits = id - BUTTON_FORGE;
        ItemStack weapon = this.inputs.getItem(BIG);
        int dents = DentForging.dents(weapon);
        int xp = DentForging.xpCost(this.inputs.getItem(SMALL));
        if (hits > dents) return false;
        int fixed = DentForging.repair(weapon, hits, this.inputs.getItem(SMALL));
        this.inputs.removeItem(SMALL, 1);
        this.table.burn(DentForging.REPAIR_LAVA);
        if (!serverPlayer.isCreative()) serverPlayer.giveExperienceLevels(-xp);
        this.inputs.setChanged();
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.1F));
        serverPlayer.sendOverlayMessage(Component.literal("Forged! " + hits + "/" + dents + " dents: +" + fixed + " durability"));
        return true;
    }

    // The Salvage button: the gear in the big slot becomes 1 of its material (to the player), the hammer loses a use
    private boolean salvage(Player clicker) {
        if (this.table == null || !(clicker instanceof ServerPlayer serverPlayer)) return true;
        if (this.salvageState() != Salvaging.State.READY) return false;
        ItemStack back = Salvaging.result(this.inputs.getItem(BIG));
        Socketing.Gem gemInside = Socketing.gem(this.inputs.getItem(BIG));
        this.inputs.setItem(BIG, ItemStack.EMPTY);
        boolean hammerLeft = Salvaging.useHammer(this.inputs.getItem(SMALL));
        this.inputs.setChanged();
        this.table.burn(Salvaging.SALVAGE_LAVA);
        serverPlayer.getInventory().placeItemBackInInventory(back.copy(), Prediction.SERVER_ONLY);
        // a gem in its socket comes back too
        if (gemInside != null) serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(gemInside.item), Prediction.SERVER_ONLY);
        this.access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.9F, 0.8F);
            if (!hammerLeft) level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        });
        serverPlayer.sendOverlayMessage(Component.literal("Salvaged: 1 " + back.getHoverName().getString()
            + (hammerLeft ? "" : ". Your Salvage Hammer broke!")));
        return true;
    }

    // Levels can change while the menu is open (killing a mob...): show or hide the socket result to match.
    // Vanilla's slotsChanged calls broadcastChanges itself, so this must not run again from inside its own refresh.
    @Override
    public void broadcastChanges() {
        if (!this.refreshingSocket && this.table != null && this.isSocketing()
            && canAffordSocket(this.player) == this.result.getItem(0).isEmpty()) {
            this.refreshingSocket = true;
            try {
                this.slotsChanged(this.inputs);
            } finally {
                this.refreshingSocket = false;
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.inputs);
            this.clearContainer(player, this.fuel);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, this.kind.block());
    }

    // Shift-click: the result, the inputs and the fuel slot go to your inventory; from the inventory, a Lava Bucket
    // goes in the fuel slot, anything else into the big slot if it's empty, else the small one.
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            // a rerolled item gets its new stats before it moves (onTake below only sees what's left behind)
            if (slotIndex == RESULT) Rerolling.roll(stack, player);
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            if (slotIndex == RESULT) slot.onQuickCraft(stack, clicked);
        } else if (SmithTableBlockEntity.fuelValue(stack) > 0) {
            if (!this.moveItemStackTo(stack, FUEL, FUEL + 1, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, BIG, RESULT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }
}
