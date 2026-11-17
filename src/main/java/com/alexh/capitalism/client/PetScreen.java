package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.network.PetActionPacket;
import com.alexh.capitalism.pets.PetData;
import com.alexh.capitalism.pets.Pets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

// A pet's window (pets/Pets): its owner Ctrl + right-clicks it. Three tabs:
//  - Status: its name, what it is, its hearts, hunger and thirst (live: the pet's data is synced to the client).
//  - Traits: the buff you get from petting it once a day (pets/PetTraits) and what it eats (hover a food for its name).
//  - Actions: Stay Still (on / off, a PetActionPacket), its Hat (designer/Hat: the one it wears, or the ones you carry
//    to put on; a PetHatPacket) and its name (type it and press Rename or Enter, a
//    RenamePetPacket; empty = back to "Pet Pig"). The server checks it's the owner.
// Closes by itself if the pet is gone or far away.
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public class PetScreen extends Screen {

    private static final int W = 220;
    private static final int H = 150;
    private static final int STATUS = 0;
    private static final int TRAITS = 1;
    private static final int ACTIONS = 2;
    private static final String[] TABS = {"Status", "Traits", "Actions"};
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};
    private static final int PANEL = 0xFF3A2E22;
    private static final int PANEL_DARK = 0xFF221A12;
    private static final int TRIM_LIGHT = 0xFF9AD070;
    private static final int TRIM_DARK = 0xFF4A6A2A;
    private static final int GOLD = 0xFFE8D08A;
    private static final int TEXT = 0xFFF2EAD6;
    private static final int TEXT_DIM = 0xFFB8AE98;

    private final int entityId;
    private int tab = STATUS;
    // what each kind of animal eats (every item it takes as food), worked out once
    private static final java.util.Map<net.minecraft.world.entity.EntityType<?>, java.util.List<net.minecraft.world.item.ItemStack>> EATS = new java.util.HashMap<>();
    private net.minecraft.client.gui.components.EditBox name;
    // what's typed in the name box (kept when the tab is rebuilt)
    private @Nullable String typed;

    public PetScreen(int entityId) {
        super(Component.literal("Pet"));
        this.entityId = entityId;
    }

    // Ctrl + right-click your pet: open its window instead of anything else (before Pets' petting gets it)
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND || !mc.hasControlDown()) return;
        Entity target = event.getTarget();
        if (!Pets.isOwner(target, event.getEntity())) return;
        mc.gui.setScreen(new PetScreen(target.getId()));
        event.setCanceled(true);
    }

    private @Nullable Animal pet() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        Entity entity = mc.level.getEntity(this.entityId);
        return entity instanceof Animal animal && Pets.isPet(animal) && animal.distanceTo(mc.player) < 16 ? animal : null;
    }

    @Override
    protected void init() {
        int l = (this.width - W) / 2;
        int t = (this.height - H) / 2;
        for (int i = 0; i < TABS.length; i++) {
            int which = i;
            this.addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> this.tab(which)).bounds(l + 6 + i * 62, t + 6, 60, 16).build()).active = this.tab != i;
        }
        if (this.tab == ACTIONS) {
            Animal pet = this.pet();
            boolean staying = pet != null && Pets.data(pet).stay();
            this.addRenderableWidget(Button.builder(Component.literal(staying ? "Stay Still: ON" : "Stay Still: OFF"), b -> this.stay())
                .bounds(l + 12, t + 40, W - 24, 20).build());
            if (this.name != null) this.typed = this.name.getValue();
            this.name = new net.minecraft.client.gui.components.EditBox(this.font, l + 12, t + 84, W - 82, 18, Component.literal("Name"));
            this.name.setMaxLength(Pets.MAX_NAME);
            this.name.setValue(this.typed != null ? this.typed : pet != null ? Pets.name(pet).getString() : "");
            this.addRenderableWidget(this.name);
            this.addRenderableWidget(Button.builder(Component.literal("Rename"), b -> this.rename()).bounds(l + W - 66, t + 83, 54, 20).build());
        } else {
            this.name = null;
            this.typed = null;
        }
    }

    private void tab(int tab) {
        this.tab = tab;
        this.rebuildWidgets();
    }

    private void rename() {
        if (this.name == null) return;
        ClientPacketDistributor.sendToServer(new com.alexh.capitalism.network.RenamePetPacket(this.entityId, this.name.getValue()));
        this.name.setFocused(false);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.name != null && this.name.isFocused() && event.isConfirmation()) {
            this.rename();
            return true;
        }
        return super.keyPressed(event);
    }

    private void stay() {
        ClientPacketDistributor.sendToServer(new PetActionPacket(this.entityId, Pets.ACTION_STAY));
        // the button flips when the server's answer (the synced pet data) comes back
    }

    private boolean shownStay;

    @Override
    public void tick() {
        Animal pet = this.pet();
        if (pet == null) {
            this.onClose();
            return;
        }
        if (this.tab == ACTIONS && Pets.data(pet).stay() != this.shownStay) this.rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        Animal pet = this.pet();
        this.shownStay = pet != null && Pets.data(pet).stay();
        super.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int l = (this.width - W) / 2;
        int t = (this.height - H) / 2;
        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + W, t + H, PANEL);
        graphics.fill(l + 4, t + 26, l + W - 4, t + H - 4, PANEL_DARK);
        Animal pet = this.pet();
        if (pet != null) {
            PetData data = Pets.data(pet);
            if (this.tab == TRAITS) {
                this.traits(graphics, pet, data, l, t, mouseX, mouseY);
            } else if (this.tab == ACTIONS) {
                graphics.text(this.font, "Actions", l + 12, t + 30, GOLD, false);
                graphics.text(this.font, "Name", l + 12, t + 72, TEXT, false);
                this.hats(graphics, l, t, data, mouseX, mouseY);
            } else {
                int y = t + 30;
                graphics.text(this.font, Pets.name(pet), l + 12, y, GOLD, false);
                y += 10;
                graphics.text(this.font, pet.getType().getDescription().getString() + (data.stay() ? " - staying still" : ""), l + 12, y, TEXT_DIM, false);
                y += 14;
                graphics.text(this.font, "Health", l + 12, y + 1, TEXT, false);
                PetHud.hearts(graphics, l + 60, y, pet);
                graphics.text(this.font, num(pet.getHealth()) + "/" + num(pet.getMaxHealth()), l + 60, y + 10, TEXT_DIM, false);
                y += PetHud.heartCount(pet) > 16 ? 30 : 22;
                graphics.text(this.font, "Hunger", l + 12, y + 1, TEXT, false);
                PetHud.hunger(graphics, l + 60, y, data.hunger(), data.max());
                graphics.text(this.font, num(data.hunger()) + "/" + num(data.max()), l + 160, y + 1, TEXT_DIM, false);
                y += 14;
                graphics.text(this.font, "Thirst", l + 12, y + 1, TEXT, false);
                PetHud.thirst(graphics, l + 60, y, data.thirst(), data.max());
                graphics.text(this.font, num(data.thirst()) + "/" + num(data.max()), l + 160, y + 1, TEXT_DIM, false);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    // Traits: the daily petting buff (its tier, each effect with its icon, level and how long) and what it eats
    private void traits(GuiGraphicsExtractor graphics, Animal pet, PetData data, int l, int t, int mouseX, int mouseY) {
        com.alexh.capitalism.pets.PetTraits.Trait trait = com.alexh.capitalism.pets.PetTraits.of(pet.getType());
        int y = t + 30;
        graphics.text(this.font, "Pet it once a day for:", l + 12, y, GOLD, false);
        graphics.text(this.font, trait.tier().title, l + W - 12 - this.font.width(trait.tier().title), y, trait.tier().color, false);
        y += 12;
        for (com.alexh.capitalism.pets.PetTraits.Buff buff : trait.buffs()) {
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.Hud.getMobEffectSprite(buff.effect()), l + 12, y, 18, 18);
            Component name = buff.effect().value().getDisplayName().copy()
                .append(buff.amplifier() > 0 ? " " + ROMAN[Math.min(buff.amplifier(), ROMAN.length - 1)] : "");
            graphics.text(this.font, name, l + 34, y + 1, TEXT, false);
            graphics.text(this.font, duration(buff.minutes()), l + 34, y + 10, TEXT_DIM, false);
            y += 20;
        }
        boolean today = data.pettedDay() == Pets.day(pet.level());
        graphics.text(this.font, today ? "Already petted today" : "Pet it today to get it!", l + 12, y + 1, today ? TEXT_DIM : 0xFF8AD06A, false);
        y += 14;
        graphics.text(this.font, "Eats:", l + 12, y, GOLD, false);
        y += 10;
        java.util.List<net.minecraft.world.item.ItemStack> foods = EATS.computeIfAbsent(pet.getType(), type -> eats(pet));
        int perRow = (W - 24) / 18;
        for (int i = 0; i < foods.size() && y + (i / perRow) * 18 < t + H - 20; i++) {
            int fx = l + 12 + (i % perRow) * 18, fy = y + (i / perRow) * 18;
            graphics.item(foods.get(i), fx, fy);
            // hover a food for its name
            if (mouseX >= fx && mouseX < fx + 16 && mouseY >= fy && mouseY < fy + 16) graphics.setTooltipForNextFrame(this.font, foods.get(i), mouseX, mouseY);
        }
        if (foods.isEmpty()) graphics.text(this.font, "Nothing you can hand it", l + 12, y + 2, TEXT_DIM, false);
    }

    private static java.util.List<net.minecraft.world.item.ItemStack> eats(Animal pet) {
        java.util.List<net.minecraft.world.item.ItemStack> foods = new java.util.ArrayList<>();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            if (!stack.isEmpty() && com.alexh.capitalism.pets.PetTraits.eats(pet, stack)) foods.add(stack);
        }
        return foods;
    }

    private static String duration(double minutes) {
        if (minutes <= 0) return "Instant";
        if (minutes < 1) return String.format(Locale.ROOT, "%.0f s", minutes * 60);
        return minutes == Math.floor(minutes) ? String.format(Locale.ROOT, "%.0f min", minutes) : String.format(Locale.ROOT, "%.1f min", minutes);
    }

    // ── the Hat row (Actions tab): the hat it wears (click to take it off) and the hats you carry (click one to put it on) ──
    private static final int HAT_Y = 120;
    private static final int HAT_STEP = 20;

    // The Designer's hats in the player's inventory, one of each kind
    private static java.util.List<com.alexh.capitalism.designer.Hat> carried() {
        java.util.List<com.alexh.capitalism.designer.Hat> list = new java.util.ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return list;
        for (com.alexh.capitalism.designer.Hat hat : com.alexh.capitalism.designer.Hat.values()) {
            if (mc.player.getInventory().contains(new net.minecraft.world.item.ItemStack(hat.item()))) list.add(hat);
        }
        return list;
    }

    private void hats(GuiGraphicsExtractor graphics, int l, int t, PetData data, int mouseX, int mouseY) {
        graphics.text(this.font, "Hat", l + 12, t + HAT_Y - 10, TEXT, false);
        int x = l + 12;
        int y = t + HAT_Y;
        com.alexh.capitalism.designer.Hat worn = data.hat();
        boolean hover = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        graphics.fill(x, y, x + 18, y + 18, TRIM_DARK);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, hover && worn != null ? 0xFF4A3A28 : 0xFF14100A);
        if (worn != null) {
            graphics.item(new net.minecraft.world.item.ItemStack(worn.item()), x + 1, y + 1);
            graphics.text(this.font, worn.does, x + 24, y + 5, 0xFF9CF08A, false);
            if (hover) graphics.setTooltipForNextFrame(this.font, Component.literal(worn.title + ": click to take it off"), mouseX, mouseY);
            return;
        }
        java.util.List<com.alexh.capitalism.designer.Hat> carried = carried();
        if (carried.isEmpty()) {
            graphics.text(this.font, "No hats on you (a Designer makes them)", x + 24, y + 5, TEXT_DIM, false);
            return;
        }
        graphics.text(this.font, "Put on:", x + 24, y + 5, TEXT_DIM, false);
        for (int i = 0; i < carried.size(); i++) {
            int hx = x + 64 + i * HAT_STEP;
            boolean over = mouseX >= hx && mouseX < hx + 18 && mouseY >= y && mouseY < y + 18;
            graphics.fill(hx, y, hx + 18, y + 18, over ? GOLD : TRIM_DARK);
            graphics.fill(hx + 1, y + 1, hx + 17, y + 17, 0xFF2A2218);
            graphics.item(new net.minecraft.world.item.ItemStack(carried.get(i).item()), hx + 1, y + 1);
            if (over) graphics.setTooltipForNextFrame(this.font, java.util.List.of(Component.literal(carried.get(i).title),
                Component.literal(carried.get(i).does).withStyle(net.minecraft.ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        Animal pet = this.pet();
        if (this.tab == ACTIONS && pet != null) {
            int l = (this.width - W) / 2;
            int t = (this.height - H) / 2;
            int x = l + 12;
            int y = t + HAT_Y;
            if (event.y() >= y && event.y() < y + 18) {
                if (Pets.data(pet).hat() != null && event.x() >= x && event.x() < x + 18) {
                    ClientPacketDistributor.sendToServer(new com.alexh.capitalism.network.PetHatPacket(this.entityId, -1));
                    return true;
                }
                java.util.List<com.alexh.capitalism.designer.Hat> carried = carried();
                for (int i = 0; i < carried.size() && Pets.data(pet).hat() == null; i++) {
                    int hx = x + 64 + i * HAT_STEP;
                    if (event.x() >= hx && event.x() < hx + 18) {
                        ClientPacketDistributor.sendToServer(new com.alexh.capitalism.network.PetHatPacket(this.entityId, carried.get(i).ordinal()));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static String num(float value) {
        return String.format(Locale.ROOT, "%.0f", Math.ceil(value));
    }
}
