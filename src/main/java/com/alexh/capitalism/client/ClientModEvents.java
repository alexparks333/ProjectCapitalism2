package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.FermenterBlock;
import com.alexh.capitalism.block.KitchenRecipes;
import com.alexh.capitalism.block.JuicerBlock;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.player.DietData;
import com.alexh.capitalism.player.DietValues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import com.alexh.capitalism.core.GoldenBonus;
import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.core.ModItems;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.alexh.capitalism.network.DrinkWaterPacket;
import com.alexh.capitalism.player.ClientThirstData;
import com.alexh.capitalism.player.WaterDrinking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.commands.Commands;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.IItemDecorator;
import net.minecraft.core.registries.BuiltInRegistries;
import com.alexh.capitalism.weaponsmith.Sharpness;
import com.alexh.capitalism.weaponsmith.SwordForging;
import com.alexh.capitalism.weaponsmith.ToolForging;
import com.alexh.capitalism.weaponsmith.Wear;
import com.alexh.capitalism.weaponsmith.Rerolling;
import com.alexh.capitalism.weaponsmith.Socketing;
import com.alexh.capitalism.occupation.ClientJobData;
import com.alexh.capitalism.occupation.CookMadeFood;
import com.alexh.capitalism.occupation.FoodNerf;
import com.alexh.capitalism.occupation.Job;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    // Skill book lessons (librarian/Skill) don't show as effect icons next to the inventory or on the HUD: they're in
    // Wellness > Buffs & Debuffs instead
    @SubscribeEvent
    public static void onRegisterClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        var hidden = new net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(net.minecraft.world.effect.MobEffectInstance instance) {
                return false;
            }

            @Override
            public boolean isVisibleInGui(net.minecraft.world.effect.MobEffectInstance instance) {
                return false;
            }
        };
        event.registerMobEffect(hidden, com.alexh.capitalism.core.ModEffects.BOOK_LESSONS.values().stream()
            .map(holder -> holder.get()).toArray(net.minecraft.world.effect.MobEffect[]::new));
    }

    // The petting hand (the first pet of the day) floats up just like vanilla's heart
    @SubscribeEvent
    public static void onRegisterParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.alexh.capitalism.core.ModParticles.PETTING.get(), net.minecraft.client.particle.HeartParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // the clock the meat's freshness bars read (farmer/MeatItems)
        com.alexh.capitalism.farmer.Freshness.clientClock = () -> {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? -1L : level.getGameTime();
        };
        event.registerAbove(
            VanillaGuiLayers.FOOD_LEVEL,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "thirst_bar"),
            ThirstHud::render
        );
        event.registerAbove(
            VanillaGuiLayers.FOOD_LEVEL,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "gold_food_bar"),
            ThirstHud::renderGoldFood
        );
        // the calendar clock, top right (calendar/Calendar), below the status effect icons
        event.registerAbove(
            VanillaGuiLayers.EFFECTS,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "calendar_clock"),
            ClockHud::render
        );
        // the new day's card, bottom right (calendar/CalendarEvents.morning)
        event.registerAbove(
            VanillaGuiLayers.CHAT,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "day_notice"),
            DayNoticeHud::render
        );
        // owing death tax: "In Purgatory", top left (deathtax/DeathTax)
        event.registerAbove(
            VanillaGuiLayers.CHAT,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "purgatory"),
            PurgatoryHud::render
        );
        // /devpanel: debugging info in the bottom left
        event.registerAbove(
            VanillaGuiLayers.CHAT,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "dev_panel"),
            DevPanel::render
        );
        // Looking at a claimed animal: whose it is (and a pet's name, hearts, hunger and thirst)
        event.registerAbove(
            VanillaGuiLayers.CROSSHAIR,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "pet_info"),
            PetHud::render
        );
        // Looking at a placed Meat Slab: its lean and how long it keeps
        event.registerAbove(
            VanillaGuiLayers.CROSSHAIR,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "meat_slab_info"),
            MeatSlabHud::render
        );
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "temperature"),
            TemperatureHud::render
        );
        event.registerAbove(
            VanillaGuiLayers.CROSSHAIR,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "gold_owner"),
            GoldOwnerHud::render
        );
        // Reading a skill book: the open book in the middle of the screen
        event.registerAbove(
            VanillaGuiLayers.CROSSHAIR,
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "reading"),
            ReadingHud::render
        );
    }

    // Vanilla draws the air bubbles exactly where our thirst drops are, so lift them one row (10px) above.
    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        // /toggle leaderboard hides the Gold Leaderboard on this screen only (LeaderboardToggle)
        if (event.getName().equals(VanillaGuiLayers.SCOREBOARD_SIDEBAR) && LeaderboardToggle.shouldHideSidebar()) {
            event.setCanceled(true);
            return;
        }
        // a drumstick lost to a Poor diet isn't drawn at all: vanilla always draws 10, so draw the shorter bar ourselves
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) && com.alexh.capitalism.player.ClientThirstData.foodCap < 20) {
            event.setCanceled(true);
            ThirstHud.renderShortFood(event.getGuiGraphics());
            return;
        }
        if (event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            event.getGuiGraphics().pose().pushMatrix();
            event.getGuiGraphics().pose().translate(0, -10);
        }
    }

    @SubscribeEvent
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) event.getGuiGraphics().pose().popMatrix();
    }

    // /guides opens the guides screen (client-side command, no server needed)
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guides").executes(context -> {
            GuidesScreen.open();
            return 1;
        }));
        // /pick opens the dev job picker (the server only lets ops change jobs with it)
        event.getDispatcher().register(Commands.literal("pick").executes(context -> {
            JobPickScreen.open();
            return 1;
        }));
        // /toggle leaderboard hides or shows the Gold Leaderboard sidebar on your screen
        event.getDispatcher().register(Commands.literal("toggle").then(Commands.literal("leaderboard").executes(context -> {
            LeaderboardToggle.toggle();
            return 1;
        })));
    }

    // Right-clicking water with an empty hand takes a sip (the server checks salt water, thirst, cooldown).
    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || player == null) return;
        if (!player.getMainHandItem().isEmpty() || player.isSpectator()) return;
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.ENTITY) return;
        BlockPos water = WaterDrinking.lookedAtWater(player);
        if (water == null) return;
        boolean salt = WaterDrinking.isSaltWater(player.level(), water);
        if (!salt && ClientThirstData.thirst >= ClientThirstData.thirstCap && !player.isCreative()) return;
        ClientPacketDistributor.sendToServer(DrinkWaterPacket.INSTANCE);
        event.setCanceled(true);
        event.setSwingHand(!salt);
    }

    // Foods show their diet points, e.g. "Diet: +2 Fruit (~1% of your diet)".
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        // the Blood Moon Crest and its pieces (Events.md > Trophies)
        var held = event.getItemStack();
        if (held.is(ModItems.REVENANT_CREST.get()) || held.is(ModItems.SANGUINE_CREST.get()) || held.is(ModItems.GOREFANG_CREST.get())) {
            event.getToolTip().add(Component.literal("A broken third of the Blood Moon Crest.").withStyle(s -> s.withColor(0xE04050).withItalic(true)));
            event.getToolTip().add(Component.literal("The Blood Merchant mends all three into one.").withStyle(ChatFormatting.GRAY));
        } else if (held.is(ModItems.BLOOD_MOON_CREST.get())) {
            event.getToolTip().add(Component.literal("Whole again, still warm.").withStyle(s -> s.withColor(0xE04050).withItalic(true)));
            event.getToolTip().add(Component.literal("The Blood Merchant trades a wish for it.").withStyle(ChatFormatting.GRAY));
        }
        // the Wishing Stars and the Changing Star (Jobs.md)
        if (event.getItemStack().getItem() instanceof com.alexh.capitalism.occupation.WishingStarItem star) {
            event.getToolTip().add(Component.literal("Wish on the Wishing Well for one more").withStyle(ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.literal(star.category.title + " job slot.").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (event.getItemStack().is(ModItems.CHANGING_STAR.get())) {
            event.getToolTip().add(Component.literal("Right-click: change your jobs in /pick.").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (event.getItemStack().is(ModItems.WISHING_WELL.get())) {
            event.getToolTip().add(Component.literal("Make a wish with a Wishing Star.").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (event.getItemStack().is(ModItems.MAILBOX.get())) {
            event.getToolTip().add(Component.literal("Anyone can drop mail in; only you can take it out.").withStyle(ChatFormatting.GRAY));
        } else if (event.getItemStack().is(ModItems.TOWN_MAILBOX.get())) {
            event.getToolTip().add(Component.literal("Anyone can post mail; only the Mayor and Vice Mayor read it.").withStyle(ChatFormatting.GRAY));
        }
        // An Apprentice item: its armor set and the XP it's holding
        if (event.getItemStack().is(ModItems.APPRENTICE.get())) {
            var kind = com.alexh.capitalism.enchanter.ApprenticeItem.kind(event.getItemStack());
            if (kind == null) {
                event.getToolTip().add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
            } else {
                int xp = com.alexh.capitalism.enchanter.ApprenticeItem.storedXp(event.getItemStack());
                event.getToolTip().add(Component.literal(kind.title + ": " + xp + " / " + kind.capacity() + " XP").withStyle(ChatFormatting.GREEN));
            }
        }
        // A skill book (librarian/SkillBookItem): Rough / Delicate, Persistent (a trait), Instant or Temporary (a lesson, how long),
        // what it does, and how to read it
        if (event.getItemStack().is(ModItems.SKILL_BOOK.get())) {
            var book = com.alexh.capitalism.librarian.SkillBookItem.book(event.getItemStack());
            var skill = book == null ? null : book.resolve();
            if (skill != null) {
                var tip = event.getToolTip();
                tip.add(Component.literal(book.delicate() ? "Delicate Book" : "Rough Book").withStyle(ChatFormatting.GRAY));
                if (skill.trait) {
                    tip.add(Component.literal(skill.rare ? "Persistent (rare)" : "Persistent").withStyle(ChatFormatting.GOLD));
                } else if (skill.instant) {
                    tip.add(Component.literal(skill.rare ? "Instant (rare): once, when you read it" : "Instant: once, when you read it").withStyle(ChatFormatting.AQUA));
                } else {
                    tip.add(Component.literal(book.delicate() ? "Temporary: level II for 40 minutes" : "Temporary: level I for 20 minutes")
                        .withStyle(ChatFormatting.GREEN));
                }
                tip.add(Component.literal(skill.description(book.delicate() ? 2 : 1)).withStyle(ChatFormatting.WHITE));
                tip.add(Component.literal("Hold right-click to read it.").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        // The Pet Trainer's items (pets/Pets)
        if (event.getItemStack().is(ModItems.OWNERSHIP_TAG.get())) {
            event.getToolTip().add(Component.literal("Right-click a trained pet to make it yours.").withStyle(ChatFormatting.GRAY));
        } else if (event.getItemStack().is(ModItems.TRAINING_STICK.get())) {
            event.getToolTip().add(Component.literal("Pet Trainers: right-click an animal").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("to train it into a pet, ready to be claimed.").withStyle(ChatFormatting.GRAY));
        }
        // Meat that rots (farmer/Freshness): a slab's lean, and how long it stays fresh
        if (com.alexh.capitalism.farmer.Freshness.rots(event.getItemStack())) {
            ItemStack meat = event.getItemStack();
            if (com.alexh.capitalism.farmer.MeatCut.ofSlab(meat) != null && meat.has(com.alexh.capitalism.core.ModComponents.LEAN.get())) {
                int lean = com.alexh.capitalism.farmer.Freshness.lean(meat);
                int lost = com.alexh.capitalism.farmer.Freshness.leanPenalty(lean);
                event.getToolTip().add(Component.literal("Lean: " + lean + "%" + (lost > 0 ? " (-" + lost + " meat)" : ""))
                    .withStyle(lost == 0 ? ChatFormatting.GREEN : lost == 1 ? ChatFormatting.GOLD : ChatFormatting.RED));
            }
            var level = net.minecraft.client.Minecraft.getInstance().level;
            long now = level == null ? 0 : level.getGameTime();
            long left = level == null ? -1 : com.alexh.capitalism.farmer.Freshness.ticksLeft(meat, now);
            com.alexh.capitalism.farmer.Chill chill = meat.get(com.alexh.capitalism.core.ModComponents.CHILL.get());
            if (chill != null && chill.current(now)) {
                // in a running Fridge / Freezer: a smooth countdown of how long it keeps in there, and how long it'd
                // have if it came out
                long normal = chill.leftAtNormalRate(now);
                event.getToolTip().add(Component.literal((chill.slower() >= 6 ? "❄ Frozen: " : "❄ Chilled: ") + "rotting " + chill.slower() + "x slower")
                    .withStyle(ChatFormatting.AQUA));
                event.getToolTip().add(Component.literal("Keeps " + com.alexh.capitalism.farmer.Freshness.clock(normal * chill.slower()) + " in here")
                    .withStyle(ChatFormatting.GREEN));
                event.getToolTip().add(Component.literal("(" + com.alexh.capitalism.farmer.Freshness.clock(normal) + " if taken out)")
                    .withStyle(ChatFormatting.GRAY));
            } else if (left < 0) {
                event.getToolTip().add(Component.literal("Fresh").withStyle(ChatFormatting.GREEN));
            } else if (left == 0) {
                event.getToolTip().add(Component.literal("Rotting!").withStyle(ChatFormatting.DARK_GREEN));
            } else {
                ChatFormatting color = left < 2 * com.alexh.capitalism.farmer.Freshness.MINUTE ? ChatFormatting.RED
                    : left < 5 * com.alexh.capitalism.farmer.Freshness.MINUTE ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
                event.getToolTip().add(Component.literal("Fresh for " + com.alexh.capitalism.farmer.Freshness.clock(left)).withStyle(color));
            }
            // dry-aging (fine cuts in a running Fridge)
            int aging = com.alexh.capitalism.farmer.Freshness.aging(meat);
            if (aging >= com.alexh.capitalism.farmer.Freshness.PRIME_AGED) {
                event.getToolTip().add(Component.literal("★★ Prime Dry-Aged").withStyle(ChatFormatting.GOLD));
            } else if (aging >= com.alexh.capitalism.farmer.Freshness.DRY_AGED) {
                int share = Math.round(100F * (aging - com.alexh.capitalism.farmer.Freshness.DRY_AGED)
                    / (com.alexh.capitalism.farmer.Freshness.PRIME_AGED - com.alexh.capitalism.farmer.Freshness.DRY_AGED));
                event.getToolTip().add(Component.literal("★ Dry-Aged (Prime: " + share + "%)").withStyle(ChatFormatting.GOLD));
            } else if (aging > 0) {
                event.getToolTip().add(Component.literal("Dry-aging: " + Math.round(100F * aging / com.alexh.capitalism.farmer.Freshness.DRY_AGED) + "%")
                    .withStyle(ChatFormatting.YELLOW));
            }
            if (chill == null || !chill.current(now)) {
                event.getToolTip().add(Component.literal(com.alexh.capitalism.farmer.Freshness.canAge(meat)
                    ? "Keep it in a Fridge (it dry-ages) or Freezer." : "Keep it in a Fridge or Freezer.").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        // A steak off the Grill (farmer/Sear): how done, against what was ordered, its crust, aged
        com.alexh.capitalism.farmer.Sear sear = event.getItemStack().get(com.alexh.capitalism.core.ModComponents.SEAR.get());
        if (sear != null) {
            var tip = event.getToolTip();
            var searedCut = com.alexh.capitalism.farmer.GrillMenu.searedCutOf(event.getItemStack());
            boolean perfect = searedCut != null && sear.perfect(searedCut);
            tip.add(Component.literal(sear.done().title + (perfect ? "  ★ Perfectly Cooked" : "")).withStyle(perfect ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            if (sear.aged() > 0) tip.add(Component.literal(sear.aged() == 2 ? "★★ Prime Dry-Aged" : "★ Dry-Aged").withStyle(ChatFormatting.GOLD));
            if (searedCut != null && sear.strengthens(searedCut)) tip.add(Component.literal("Strength I (2:00)").withStyle(ChatFormatting.BLUE));
            if (searedCut != null && sear.unsafe(searedCut)) tip.add(Component.literal("Undercooked!").withStyle(ChatFormatting.RED));
        }
        if (event.getItemStack().is(ModItems.ROTTEN_MEAT.get())) {
            event.getToolTip().add(Component.literal("Pet food or fertilizer, not for people.").withStyle(ChatFormatting.GRAY));
        }
        // Butcher Knives (farmer/ButcherKnifeItem): how many slabs it can still cut
        if (com.alexh.capitalism.farmer.ButcherKnifeItem.isKnife(event.getItemStack())) {
            ItemStack knife = event.getItemStack();
            int left = com.alexh.capitalism.farmer.ButcherKnifeItem.cutsLeft(knife);
            event.getToolTip().add(Component.literal("Sharpness: " + left + " / " + com.alexh.capitalism.farmer.ButcherKnifeItem.maxCuts(knife) + " slabs")
                .withStyle(left <= 2 ? ChatFormatting.RED : ChatFormatting.AQUA));
            event.getToolTip().add(Component.literal("Spent (it breaks) on its last cut.").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Sharpen it at a Pickaxe Whetstone.").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Swords: their forged sharpness (weaponsmith/SwordForging) and how much edge is left (weaponsmith/Sharpness)
        if (Sharpness.isSword(event.getItemStack()) && !SwordForging.isForged(event.getItemStack())) {
            // Not rolled yet (e.g. sitting in the crafting result slot): "?" out of the best YOU could roll (no Grail)
            var player = Minecraft.getInstance().player;
            boolean smith = player != null && ClientJobData.has(Job.WEAPONSMITH);
            int topSharpness = smith ? SwordForging.SMITH_SHARPNESS_MAX : SwordForging.OTHERS_SHARPNESS_MAX;
            int topEdge = smith ? SwordForging.SMITH_EDGE_MAX : SwordForging.OTHERS_EDGE_MAX;
            event.getToolTip().add(Component.literal("Sharpness: ?/" + topSharpness).withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Edge: ?/" + topEdge + " hits").withStyle(ChatFormatting.GRAY));
        } else if (Sharpness.isSword(event.getItemStack())) {
            ItemStack sword = event.getItemStack();
            int left = Sharpness.hitsLeft(sword);
            int max = Sharpness.maxHits(sword);
            int sharpness = SwordForging.sharpness(sword);
            if (SwordForging.isGrailSharpness(sword)) {
                event.getToolTip().add(Component.literal("Sharpness: " + sharpness + " ★ Grail").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                ChatFormatting tier = sharpness >= 100 ? ChatFormatting.GREEN : sharpness >= 80 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
                event.getToolTip().add(Component.literal("Sharpness: " + sharpness + " (" + sharpness + "% damage)").withStyle(tier));
            }
            if (left <= 0) {
                // the name itself says it: "Dull Iron Sword"
                if (!event.getToolTip().isEmpty()) {
                    event.getToolTip().set(0, Component.literal("Dull ").withStyle(ChatFormatting.GRAY).append(event.getToolTip().get(0)));
                }
                event.getToolTip().add(Component.literal("Dull: half damage").withStyle(ChatFormatting.RED));
                event.getToolTip().add(Component.literal("Sharpen it at a Sword Whetstone").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                ChatFormatting color = left > max / 2 ? ChatFormatting.GREEN : left > max / 5 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
                String grail = SwordForging.isGrailEdge(sword) ? " ★ Grail" : "";
                event.getToolTip().add(Component.literal("Edge: " + left + "/" + max + " hits" + grail)
                    .withStyle(grail.isEmpty() ? color : ChatFormatting.LIGHT_PURPLE));
            }
        }
        // Tools (pickaxes, shovels, axes): their forged mining speed (weaponsmith/ToolForging)
        if (ToolForging.isTool(event.getItemStack())) {
            ItemStack tool = event.getItemStack();
            if (!ToolForging.isForged(tool)) {
                // Not rolled yet: "?" out of the best YOU could roll (no Grail)
                var player = Minecraft.getInstance().player;
                boolean smith = player != null && ClientJobData.has(Job.TOOLSMITH);
                int top = smith ? ToolForging.SMITH_MAX : ToolForging.OTHERS_MAX;
                event.getToolTip().add(Component.literal("Efficiency: ?/" + top).withStyle(ChatFormatting.GRAY));
            } else if (ToolForging.isGrail(tool)) {
                event.getToolTip().add(Component.literal("Efficiency: " + ToolForging.efficiency(tool) + " ★ Grail").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                int efficiency = ToolForging.efficiency(tool);
                ChatFormatting tier = efficiency >= 100 ? ChatFormatting.GREEN : efficiency >= 85 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
                event.getToolTip().add(Component.literal("Efficiency: " + efficiency + " (" + efficiency + "% mining speed)").withStyle(tier));
            }
        }
        // Pickaxes: blocks until dull; armor: hits until dented (weaponsmith/Wear)
        ItemStack worn = event.getItemStack();
        if (Wear.isPickaxe(worn) || Wear.isArmor(worn)) {
            boolean pickaxe = Wear.isPickaxe(worn);
            int max = pickaxe ? Wear.pickaxeMax(worn) : Wear.armorMax(worn);
            int left = pickaxe ? Wear.pickaxeLeft(worn) : Wear.armorLeft(worn);
            if (left <= 0) {
                if (!event.getToolTip().isEmpty()) {
                    event.getToolTip().set(0, Component.literal(pickaxe ? "Dull " : "Dented ").withStyle(ChatFormatting.GRAY)
                        .append(event.getToolTip().get(0)));
                }
                event.getToolTip().add(Component.literal(pickaxe ? "Dull: half mining speed" : "Dented: half protection").withStyle(ChatFormatting.RED));
                event.getToolTip().add(Component.literal(pickaxe ? "Sharpen it at a Pickaxe Whetstone" : "Fix it at an Armor Whetstone")
                    .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                ChatFormatting color = left > max / 2 ? ChatFormatting.GREEN : left > max / 5 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
                event.getToolTip().add(Component.literal((pickaxe ? "Edge: " : "Condition: ") + left + "/" + max
                    + (pickaxe ? " blocks" : " hits")).withStyle(color));
            }
        }
        // Molten Socket: a mystery until a smith works with it (weaponsmith/Socketing)
        if (event.getItemStack().is(ModItems.MOLTEN_SOCKET.get())) {
            event.getToolTip().add(Component.literal("Mysterious Item").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            event.getToolTip().add(Component.literal("Take it to a Smith of Your Choice to figure out what it does.").withStyle(ChatFormatting.GRAY));
        }
        // The Traveling Merchant's own goods (town/TravelingMerchantShop): what they do
        String travelerTip = null;
        if (event.getItemStack().is(ModItems.BOTTOMLESS_WATERSKIN.get())) travelerTip = "16 sips. Right-click any water to fill it: it comes out purified.";
        else if (event.getItemStack().is(ModItems.HAND_WARMER.get())) travelerTip = "In your offhand, the cold can't reach you. Burns down while it works.";
        else if (event.getItemStack().is(ModItems.SMITHS_CHISEL.get())) travelerTip = "Right-click a socketed item with it: the Molten Socket pops back out. Used up.";
        else if (event.getItemStack().is(ModItems.PHOENIX_FEATHER.get())) travelerTip = "In your offhand when you die: no death tax. It burns away.";
        else if (event.getItemStack().is(ModItems.TRAVELERS_SNOW_GLOBE.get())) travelerTip = "Right-click it and the snow swirls.";
        else if (event.getItemStack().is(ModItems.TRAVELERS_LANTERN.get())) travelerTip = "Lights up when the Traveling Merchant is about to come to town.";
        else if (event.getItemStack().is(ModItems.CHEFS_HAT.get()) || event.getItemStack().is(ModItems.WELDERS_MASK.get()) || event.getItemStack().is(ModItems.WIZARDS_HAT.get())
            || event.getItemStack().is(ModItems.FARMERS_HAT.get())) travelerTip = "A cosmetic hat: wear it to look the part.";
        else if (event.getItemStack().is(ModItems.TRAVELERS_PIPE.get())) travelerTip = "Wear it in your head slot: a pipe in the corner of your mouth.";
        else if (event.getItemStack().is(ModItems.FEEDING_HAT.get())) travelerTip = "Put it on a pet (its window): that pet is always full and never thirsty.";
        else if (event.getItemStack().is(ModItems.TRAVELERS_BAG.get())) travelerTip = "Wear it on your back: 6 slots of anything, opening out to 12 when you're 1000 blocks from town. Shrinking back near town, what doesn't fit bursts out.";
        if (travelerTip != null) {
            event.getToolTip().add(Component.literal(travelerTip).withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Only from the Traveling Merchant.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        // A socketed sword / pickaxe / armor piece: what's in the socket and how to change it
        if (Socketing.hasSocket(event.getItemStack())) {
            ItemStack socketed = event.getItemStack();
            Socketing.Gem gem = Socketing.gem(socketed);
            if (gem == null) {
                event.getToolTip().add(Component.literal("◇ Empty Socket").withStyle(ChatFormatting.GOLD));
                event.getToolTip().add(Component.literal("  Right-click it with a gem (in your inventory): " + Socketing.gemsFor(socketed)).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                event.getToolTip().add(Component.literal("◆ " + gem.title + " Socket: " + Socketing.effect(gem, socketed)).withStyle(gem.color));
                event.getToolTip().add(Component.literal("  Right-click it with an empty hand (in your inventory) to take it out").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        // Reroll Tokens: what they're for (weaponsmith/Rerolling)
        Rerolling.Tier tokenTier = Rerolling.tokenTier(event.getItemStack());
        if (tokenTier != null) {
            String metal = switch (tokenTier) {
                case IRON -> "an iron";
                case DIAMOND -> "a diamond";
                case NETHERITE -> "a netherite";
            };
            event.getToolTip().add(Component.literal("Gives " + metal + " sword or tool brand new stats").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Weaponsmith Table (swords) or Toolsmith Table (tools)").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Cook-made food is Gourmet: full strength, keeps you full longer
        if (KitchenRecipes.isGourmet(event.getItemStack())) {
            event.getToolTip().add(Component.literal("★ Gourmet").withStyle(ChatFormatting.GOLD));
        }
        // How much it fills: drumsticks for food, water drops for drinks (2 points = 1 icon)
        FoodProperties hunger = event.getItemStack().get(DataComponents.FOOD);
        if (hunger != null && hunger.nutrition() > 0) {
            event.getToolTip().add(Component.literal("Hunger: +" + icons(hunger.nutrition()) + " drumsticks").withStyle(ChatFormatting.GOLD));
        }
        float water = thirstPoints(event.getItemStack());
        if (water > 0) {
            event.getToolTip().add(Component.literal("Thirst: +" + icons(Math.round(water)) + " water drops").withStyle(style -> style.withColor(0x7CC8FF)));
        }
        FoodProperties food = event.getItemStack().get(DataComponents.FOOD);
        float[] shares = DietValues.sharesFor(event.getItemStack());
        if (food == null || shares == null) return;
        MutableComponent line = Component.literal("Diet: ").withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (int i = 0; i < DietData.GROUPS; i++) {
            if (shares[i] <= 0) continue;
            float points = food.nutrition() * shares[i];
            String amount = points == Math.floor(points) ? String.valueOf((int) points) : String.format("%.1f", points);
            if (!first) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            int color = WellnessScreen.groupColor(i);
            line.append(Component.literal("+" + amount + " " + DietData.NAMES[i]).withStyle(style -> style.withColor(color)));
            first = false;
        }
        double window = 130;
        try {
            window = CapitalismConfig.DIET_WINDOW_POINTS.get();
        } catch (IllegalStateException notLoaded) {
            // default above
        }
        long percent = Math.max(1, Math.round(food.nutrition() / window * 100));
        line.append(Component.literal(" (~" + percent + "% of your diet)").withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(line);
    }

    // 5 points -> "2.5"
    private static String icons(int points) {
        return points % 2 == 0 ? String.valueOf(points / 2) : (points / 2) + ".5";
    }

    // Thirst points a drink gives: Bartender drinks (GoldenBonus), Purified Water and plain water bottles.
    private static float thirstPoints(ItemStack stack) {
        GoldenBonus bonus = GoldenBonus.of(stack);
        if (bonus != null) return bonus.water();
        try {
            if (stack.is(ModItems.PURIFIED_WATER.get()) || stack.is(ModItems.BOTTOMLESS_WATERSKIN.get())) return (float) CapitalismConfig.THIRST_PURIFIED_WATER_RESTORE.get().doubleValue();
            PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
            if (stack.is(Items.POTION) && potion != null && potion.is(Potions.WATER)) {
                return (float) CapitalismConfig.THIRST_RAW_WATER_RESTORE.get().doubleValue();
            }
        } catch (IllegalStateException notLoaded) {
            return 0;
        }
        return 0;
    }

    // Adds the Wellness tab to the left of the survival inventory.
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inventory) {
            event.addListener(new WellnessTabButton(inventory));
        }
    }

    // Food on plates
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PLATE.get(), PlateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STITCHING_TABLE.get(), StitchingTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SEASONAL_DECOR.get(), DecorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BLOOD_DECOR.get(), BloodDecorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FISH_PLAQUE.get(), FishPlaqueRenderer::new);
        event.registerEntityRenderer(ModEntities.DECOR_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WISHING_WELL.get(), WishingWellRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MAILBOX.get(), MailboxRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MAYORAL_CHEST.get(), MayoralChestRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KNOWLEDGE_CREATOR.get(), KnowledgeCreatorRenderer::new);
        event.registerEntityRenderer(ModEntities.JOB_NPC.get(), JobNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CRIMSON_REVENANT.get(), CrimsonRevenantRenderer::new);
        event.registerEntityRenderer(ModEntities.GOREFANG.get(), c -> new BloodBossRenderer<>(c, new GorefangModel(c.bakeLayer(GorefangModel.LAYER)), "gorefang", 1.3F));
        event.registerEntityRenderer(ModEntities.SANGUINE_WITCH.get(), c -> new BloodBossRenderer<>(c, new SanguineWitchModel(c.bakeLayer(SanguineWitchModel.LAYER)), "sanguine_witch", 0.9F));
        // Apprentices: the little iron figure in a wizard hat
        event.registerEntityRenderer(ModEntities.APPRENTICE.get(), ApprenticeRenderer::new);
        event.registerEntityRenderer(ModEntities.WHISPERER.get(), WhispererRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_MERCHANT.get(), BloodMerchantRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAVELING_MERCHANT.get(), TravelingMerchantRenderer::new);
        event.registerEntityRenderer(ModEntities.FISHERMAN.get(), FishermanRenderer::new);
        event.registerEntityRenderer(ModEntities.BOUNTY_CLERK.get(), BountyClerkRenderer::new);
        // the Boat Master looks like the job NPCs (his own skin); the Pirate Ship
        event.registerEntityRenderer(ModEntities.BOAT_MASTER.get(), JobNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.PIRATE_SHIP.get(), c -> new PremiumBoatRenderer(c, "pirate_ship", PirateShipModel.LAYER, PirateShipModel::new, 1.4F, 3.6, 5.3));
        event.registerEntityRenderer(ModEntities.GHOST_SHIP.get(), c -> new PremiumBoatRenderer(c, "ghost_ship", GhostShipModel.LAYER, GhostShipModel::new, 1.4F, 3.6, 5.3));
        event.registerEntityRenderer(ModEntities.SPEEDBOAT.get(), c -> new PremiumBoatRenderer(c, "speedboat", SpeedboatModel.LAYER, SpeedboatModel::new, 1.0F, 1.6, 1.4));
        event.registerEntityRenderer(ModEntities.YACHT.get(), c -> new PremiumBoatRenderer(c, "yacht", YachtModel.LAYER, YachtModel::new, 1.6F, 2.6, 2.6));
        event.registerEntityRenderer(ModEntities.LONGSHIP.get(), c -> new PremiumBoatRenderer(c, "longship", LongshipModel.LAYER, LongshipModel::new, 1.2F, 3.4, 3.0));
        event.registerEntityRenderer(ModEntities.PADDLEWHEELER.get(), c -> new PremiumBoatRenderer(c, "paddlewheeler", PaddlewheelerModel.LAYER, PaddlewheelerModel::new, 1.4F, 2.4, 3.0));
        event.registerEntityRenderer(ModEntities.SWAMP_RAFT.get(), c -> new PremiumBoatRenderer(c, "swamp_raft", SwampRaftModel.LAYER, SwampRaftModel::new, 1.2F, 1.6, 1.6));
        event.registerEntityRenderer(ModEntities.STEAMBOAT.get(), c -> new PremiumBoatRenderer(c, "steamboat", SteamboatModel.LAYER, SteamboatModel::new, 1.6F, 3.4, 4.6));
        event.registerEntityRenderer(ModEntities.LEVIATHAN.get(), c -> new PremiumBoatRenderer(c, "leviathan", LeviathanModel.LAYER, LeviathanModel::new, 1.3F, 3.0, 2.8));
        event.registerEntityRenderer(ModEntities.TEAPOT.get(), c -> new PremiumBoatRenderer(c, "teapot", TeapotModel.LAYER, TeapotModel::new, 1.3F, 1.6, 3.2));
        event.registerEntityRenderer(ModEntities.SUBMERSIBLE.get(), c -> new PremiumBoatRenderer(c, "submersible", SubmersibleModel.LAYER, SubmersibleModel::new, 1.2F, 7.5, 2.6));
        event.registerEntityRenderer(ModEntities.FAIRY_BOAT.get(), c -> new PremiumBoatRenderer(c, "fairy_boat", FairyBoatModel.LAYER, FairyBoatModel::new, 1.1F, 2.2, 3.0));
        event.registerEntityRenderer(ModEntities.CASTAWAY_TUB.get(), c -> new PremiumBoatRenderer(c, "castaway_tub", CastawayTubModel.LAYER, CastawayTubModel::new, 0.8F, 0.6, 1.8));
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ApprenticeRenderer.LAYER, ApprenticeModel::createBodyLayer);
        event.registerLayerDefinition(WhispererRenderer.LAYER, WhispererModel::createBodyLayer);
        event.registerLayerDefinition(BloodMerchantRenderer.LAYER, BloodMerchantModel::createBodyLayer);
        event.registerLayerDefinition(TravelingMerchantRenderer.LAYER, TravelingMerchantModel::createBodyLayer);
        event.registerLayerDefinition(FishermanRenderer.LAYER, FishermanModel::createBodyLayer);
        event.registerLayerDefinition(BountyClerkRenderer.LAYER, BountyClerkModel::createBodyLayer);
        event.registerLayerDefinition(PirateShipModel.LAYER, PirateShipModel::createLayer);
        event.registerLayerDefinition(GhostShipModel.LAYER, GhostShipModel::createLayer);
        event.registerLayerDefinition(SpeedboatModel.LAYER, SpeedboatModel::createLayer);
        event.registerLayerDefinition(YachtModel.LAYER, YachtModel::createLayer);
        event.registerLayerDefinition(LongshipModel.LAYER, LongshipModel::createLayer);
        event.registerLayerDefinition(PaddlewheelerModel.LAYER, PaddlewheelerModel::createLayer);
        event.registerLayerDefinition(SwampRaftModel.LAYER, SwampRaftModel::createLayer);
        event.registerLayerDefinition(SteamboatModel.LAYER, SteamboatModel::createLayer);
        event.registerLayerDefinition(LeviathanModel.LAYER, LeviathanModel::createLayer);
        event.registerLayerDefinition(TeapotModel.LAYER, TeapotModel::createLayer);
        event.registerLayerDefinition(FairyBoatModel.LAYER, FairyBoatModel::createLayer);
        event.registerLayerDefinition(DecorModels.ChristmasTree.LAYER, DecorModels.ChristmasTree::createLayer);
        // the Blood Moon decorations (client/BloodDecorModels)
        event.registerLayerDefinition(BloodDecorModels.Lantern.LAYER, BloodDecorModels.Lantern::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Candelabra.LAYER, BloodDecorModels.Candelabra::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Throne.LAYER, BloodDecorModels.Throne::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Coffin.LAYER, BloodDecorModels.Coffin::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Fountain.LAYER, BloodDecorModels.Fountain::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Banner.LAYER, BloodDecorModels.Banner::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Rug.LAYER, BloodDecorModels.Rug::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Greatsword.LAYER, BloodDecorModels.Greatsword::createLayer);
        event.registerLayerDefinition(BloodDecorModels.SkullTrophy.LAYER, BloodDecorModels.SkullTrophy::createLayer);
        event.registerLayerDefinition(BloodDecorModels.StaffPedestal.LAYER, BloodDecorModels.StaffPedestal::createLayer);
        event.registerLayerDefinition(BloodDecorModels.Gargoyle.LAYER, BloodDecorModels.Gargoyle::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Clock.LAYER, BloodDecorModels2.Clock::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Chandelier.LAYER, BloodDecorModels2.Chandelier::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Organ.LAYER, BloodDecorModels2.Organ::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.BookshelfDoor.LAYER, BloodDecorModels2.BookshelfDoor::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Portrait.LAYER, BloodDecorModels2.Portrait::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.IronMaiden.LAYER, BloodDecorModels2.IronMaiden::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Altar.LAYER, BloodDecorModels2.Altar::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.BatRoost.LAYER, BloodDecorModels2.BatRoost::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.CanopyBed.LAYER, BloodDecorModels2.CanopyBed::createLayer);
        event.registerLayerDefinition(BloodDecorModels2.Fireplace.LAYER, BloodDecorModels2.Fireplace::createLayer);
        event.registerLayerDefinition(DecorModels.Scarecrow.LAYER, DecorModels.Scarecrow::createLayer);
        event.registerLayerDefinition(DecorModels.WitchsCauldron.LAYER, DecorModels.WitchsCauldron::createLayer);
        event.registerLayerDefinition(DecorModels.PumpkinPile.LAYER, DecorModels.PumpkinPile::createLayer);
        event.registerLayerDefinition(DecorModels.LeafPile.LAYER, DecorModels.LeafPile::createLayer);
        event.registerLayerDefinition(WishingWellModel.LAYER, WishingWellModel::createLayer);
        event.registerLayerDefinition(WishingWellModel.WATER_LAYER, WishingWellModel::createWaterLayer);
        event.registerLayerDefinition(MailboxModel.LAYER, MailboxModel::createLayer);
        event.registerLayerDefinition(MailboxModel.TOWN_LAYER, MailboxModel::createTownLayer);
        event.registerLayerDefinition(MayoralChestModel.LAYER, MayoralChestModel::createLayer);
        event.registerLayerDefinition(CrimsonRevenantModel.LAYER, CrimsonRevenantModel::createLayer);
        event.registerLayerDefinition(GorefangModel.LAYER, GorefangModel::createLayer);
        event.registerLayerDefinition(SanguineWitchModel.LAYER, SanguineWitchModel::createLayer);
        event.registerLayerDefinition(DecorModels.PresentPile.LAYER, DecorModels.PresentPile::createLayer);
        event.registerLayerDefinition(DecorModels.CandyCanePole.LAYER, DecorModels.CandyCanePole::createLayer);
        event.registerLayerDefinition(DecorModels.CandyCanes.LAYER, DecorModels.CandyCanes::createLayer);
        event.registerLayerDefinition(DecorModels.GraveRound.LAYER, DecorModels.GraveRound::createLayer);
        event.registerLayerDefinition(DecorModels.GraveCross.LAYER, DecorModels.GraveCross::createLayer);
        event.registerLayerDefinition(DecorModels.GraveTomb.LAYER, DecorModels.GraveTomb::createLayer);
        event.registerLayerDefinition(DecorModels.BonePile.LAYER, DecorModels.BonePile::createLayer);
        event.registerLayerDefinition(DecorModels.AutumnMaple.LAYER, DecorModels.AutumnMaple::createLayer);
        event.registerLayerDefinition(DecorModels.AutumnBirch.LAYER, DecorModels.AutumnBirch::createLayer);
        event.registerLayerDefinition(DecorModels.BeachTowel.LAYER, DecorModels.BeachTowel::createLayer);
        event.registerLayerDefinition(DecorModels.BeachBall.LAYER, DecorModels.BeachBall::createLayer);
        event.registerLayerDefinition(DecorModels.BeachUmbrella.LAYER, DecorModels.BeachUmbrella::createLayer);
        event.registerLayerDefinition(DecorModels.BeachChair.LAYER, DecorModels.BeachChair::createLayer);
        event.registerLayerDefinition(DecorModels.EggBasket.LAYER, DecorModels.EggBasket::createLayer);
        event.registerLayerDefinition(DecorModels.GiantEgg.LAYER, DecorModels.GiantEgg::createLayer);
        event.registerLayerDefinition(DecorModels.SpringBunny.LAYER, DecorModels.SpringBunny::createLayer);
        event.registerLayerDefinition(DecorModels.TulipBed.LAYER, DecorModels.TulipBed::createLayer);

        event.registerLayerDefinition(SubmersibleModel.LAYER, SubmersibleModel::createLayer);
        event.registerLayerDefinition(CastawayTubModel.LAYER, CastawayTubModel::createLayer);
        event.registerLayerDefinition(BjornLayer.LAYER, BjornLayer::createLayer);
        event.registerLayerDefinition(BagLayer.LAYER, BagLayer::createLayer);
        // the Pirate Hat on players' heads, the fish plaques' boards (1, 2 and 3 blocks wide)
        event.registerLayerDefinition(PirateHatLayer.LAYER, PirateHatLayer::createLayer);
        for (CosmeticHatLayer.Kind kind : CosmeticHatLayer.Kind.values()) event.registerLayerDefinition(kind.layer, () -> CosmeticHatLayer.createLayer(kind));
        for (int w = 1; w <= 3; w++) {
            int width = w;
            for (int s = 0; s < 4; s++) {
                int style = s;
                event.registerLayerDefinition(FishPlaqueRenderer.LAYERS[w - 1][s], () -> FishPlaqueRenderer.createLayer(width, style));
            }
        }
        // the catches mounted on them, sculpted in 3D (FishTrophies)
        for (var trophy : FishTrophies.LAYERS.entrySet()) {
            var kind = trophy.getKey();
            event.registerLayerDefinition(trophy.getValue(), () -> FishTrophies.create(kind));
        }
        for (com.alexh.capitalism.designer.Hat hat : com.alexh.capitalism.designer.Hat.values()) {
            event.registerLayerDefinition(PetHatLayer.LAYERS.get(hat), () -> PetHatLayer.createLayer(hat));
        }
    }

    // The Animal Bjorn on players' chests (BjornLayer) and the Designer's bags on their backs (BagLayer): the layers on every player skin, and what's in it worked out with
    // the player's render state
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer == null) continue;
            renderer.addLayer(new BjornLayer(renderer, event.getEntityModels().bakeLayer(BjornLayer.LAYER)));
            renderer.addLayer(new BagLayer(renderer, event.getEntityModels().bakeLayer(BagLayer.LAYER)));
            renderer.addLayer(new PirateHatLayer(renderer, event.getEntityModels().bakeLayer(PirateHatLayer.LAYER)));
            java.util.Map<CosmeticHatLayer.Kind, net.minecraft.client.model.geom.ModelPart> cosmetics = new java.util.EnumMap<>(CosmeticHatLayer.Kind.class);
            for (CosmeticHatLayer.Kind kind : CosmeticHatLayer.Kind.values()) cosmetics.put(kind, event.getEntityModels().bakeLayer(kind.layer));
            renderer.addLayer(new CosmeticHatLayer(renderer, cosmetics));
        }
        // pets' hats (PetHatLayer): on every living thing's renderer (only pets with a hat get one drawn)
        java.util.Map<com.alexh.capitalism.designer.Hat, net.minecraft.client.model.geom.ModelPart> hats = new java.util.EnumMap<>(com.alexh.capitalism.designer.Hat.class);
        for (com.alexh.capitalism.designer.Hat hat : com.alexh.capitalism.designer.Hat.values()) hats.put(hat, event.getEntityModels().bakeLayer(PetHatLayer.LAYERS.get(hat)));
        for (net.minecraft.world.entity.EntityType<?> type : event.getEntityTypes()) {
            if (event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> living) addHatLayer(living, hats);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addHatLayer(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer,
                                    java.util.Map<com.alexh.capitalism.designer.Hat, net.minecraft.client.model.geom.ModelPart> hats) {
        net.minecraft.client.renderer.entity.LivingEntityRenderer raw = renderer;
        raw.addLayer(new PetHatLayer(raw, hats));
    }

    @SubscribeEvent
    public static void onRenderStateModifiers(net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new com.google.common.reflect.TypeToken<net.minecraft.client.renderer.entity.LivingEntityRenderer<
                net.minecraft.world.entity.LivingEntity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState, ?>>() { },
            (net.minecraft.world.entity.LivingEntity entity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) -> {
                PetHatLayer.extract(entity, state);
                MobLevels.extract(entity, state);
                MobHealthBars.extract(entity, state, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
            });
        event.registerAvatarEntityModifier(new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
            @Override
            public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar,
                    net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                if (avatar instanceof net.minecraft.world.entity.player.Player player) {
                    BjornLayer.extract(player, state, state.partialTick);
                    BagLayer.extract(player, state);
                    MobHealthBars.extract(player, state, state.partialTick);
                }
            }
        });
    }

    // Also pickaxes (blocks until dull) and armor (hits until dented).
    // A thin bar along the bottom of a sword's icon, under the durability bar: how sharp it still is (only once it's been used). Grey = dull.
    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        IItemDecorator edge = (graphics, font, stack, x, y) -> {
            // swords: edge; pickaxes: blocks until dull; armor: hits until dented (weaponsmith/Sharpness, Wear)
            int max;
            int left;
            if (Sharpness.isSword(stack)) {
                if (Sharpness.hits(stack) == 0) return false;
                max = Sharpness.maxHits(stack);
                left = Sharpness.hitsLeft(stack);
            } else if (Wear.isPickaxe(stack)) {
                if (Wear.pickaxeUsed(stack) == 0) return false;
                max = Wear.pickaxeMax(stack);
                left = Wear.pickaxeLeft(stack);
            } else if (Wear.isArmor(stack)) {
                if (Wear.armorDents(stack) == 0) return false;
                max = Wear.armorMax(stack);
                left = Wear.armorLeft(stack);
            } else {
                return false;
            }
            int width = Math.round(13.0F * left / max);
            // a hairline (half a GUI pixel tall) along the very bottom of the icon, under the durability bar (which uses
            // the two rows above)
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + 15.5F);
            graphics.pose().scale(1.0F, 0.5F);
            if (left <= 0) {
                graphics.fill(2, 0, 15, 1, 0xFF6A6A6A);
            } else {
                int color = 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(0.5F * left / max, 0.55F, 1.0F);
                graphics.fill(2, 0, 15, 1, 0xFF000000);
                graphics.fill(2, 0, 2 + width, 1, color);
            }
            graphics.pose().popMatrix();
            return false;
        };
        // A Molten Socket (weaponsmith/Socketing): a tiny 4x4 socket in the top-left corner, molten rim, the gem's color
        // inside (dark when empty, else the gem's color).
        IItemDecorator socket = (graphics, font, stack, x, y) -> {
            if (!Socketing.hasSocket(stack)) return false;
            Socketing.Gem gem = Socketing.gem(stack);
            int core = gem == null ? 0xFF1A1014 : gem.iconColor;
            graphics.fill(x + 1, y, x + 3, y + 4, 0xFFF06A10);
            graphics.fill(x, y + 1, x + 4, y + 3, 0xFFF06A10);
            graphics.fill(x + 1, y + 1, x + 3, y + 3, core);
            if (gem != null) graphics.fill(x + 1, y + 1, x + 2, y + 2, 0xB0FFFFFF);
            return false;
        };
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            String path = entry.getKey().identifier().getPath();
            if (path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots")) {
                event.register(entry.getValue(), edge);
                event.register(entry.getValue(), socket);
            }
        }
        // Cook-made vanilla food (occupation/CookMadeFood): a tiny gold "G" (3x5 pixels, with a dark shadow) in the
        // bottom-left corner. Only vanilla food: the Cook's own meals don't get one.
        IItemDecorator gourmet = (graphics, font, stack, x, y) -> {
            if (!CookMadeFood.isCookMade(stack)) return false;
            int left = x + 1;
            int top = y + 10;
            for (int row = 0; row < GOURMET_G.length; row++) {
                for (int col = 0; col < 3; col++) {
                    if (GOURMET_G[row].charAt(col) != '#') continue;
                    graphics.fill(left + col + 1, top + row + 1, left + col + 2, top + row + 2, 0xC0201008);
                    graphics.fill(left + col, top + row, left + col + 1, top + row + 1, 0xFFFFD34A);
                }
            }
            return false;
        };
        for (Item food : FoodNerf.foods()) event.register(food, gourmet);
        // A seared steak (farmer/Sear): how done it is, small in the top-left corner (R, MR, M, MW, WD; RAW when it's
        // raw in the middle), gold when it's cooked just right
        IItemDecorator doneness = (graphics, font, stack, x, y) -> {
            com.alexh.capitalism.farmer.Sear sear = stack.get(com.alexh.capitalism.core.ModComponents.SEAR.get());
            if (sear == null) return false;
            String letters = switch (sear.done()) {
                case RAW -> "RAW";
                case RARE -> "R";
                case MEDIUM_RARE -> "MR";
                case MEDIUM -> "M";
                case MEDIUM_WELL -> "MW";
                case WELL_DONE -> "WD";
            };
            var cut = com.alexh.capitalism.farmer.GrillMenu.searedCutOf(stack);
            int color = cut != null && sear.perfect(cut) ? 0xFFFFD34A : sear.done() == com.alexh.capitalism.farmer.Sear.Doneness.RAW ? 0xFFFF5A4A : 0xFFFFFFFF;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(0.6F, 0.6F);
            graphics.text(font, letters, 0, 0, color, true);
            graphics.pose().popMatrix();
            return false;
        };
        for (var seared : ModItems.SEARED_CUTS.values()) event.register(seared.get(), doneness);
    }

    private static final String[] GOURMET_G = {
        ".##",
        "#..",
        "#.#",
        "#.#",
        ".##"};

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PURIFICATION_TABLE.get(), PurificationTableScreen::new);
        event.register(ModMenus.JUICER.get(), JuicerScreen::new);
        event.register(ModMenus.BARTENDING_TABLE.get(), BartendingTableScreen::new);
        event.register(ModMenus.FERMENTER.get(), FermenterScreen::new);
        event.register(ModMenus.SUSHI_TABLE.get(), AssemblyTableScreen::new);
        event.register(ModMenus.COOKING_TABLE.get(), AssemblyTableScreen::new);
        event.register(ModMenus.OVEN.get(), OvenScreen::new);
        event.register(ModMenus.WEAPONSMITH_TABLE.get(), SmithTableScreen::new);
        event.register(ModMenus.TOOLSMITH_TABLE.get(), SmithTableScreen::new);
        event.register(ModMenus.ARMORSMITH_TABLE.get(), SmithTableScreen::new);
        event.register(ModMenus.LAVA_CREATOR.get(), LavaCreatorScreen::new);
        event.register(ModMenus.NPC_SHOP.get(), NpcShopScreen::new);
        event.register(ModMenus.ENCHANTERS_TABLE.get(), EnchantersTableScreen::new);
        event.register(ModMenus.LAPIS_COMPRESSOR.get(), LapisCompressorScreen::new);
        event.register(ModMenus.STITCHING_TABLE.get(), StitchingTableScreen::new);
        event.register(ModMenus.SEWING_MACHINE.get(), SewingMachineScreen::new);
        event.register(ModMenus.DESIGNER_TABLE.get(), DesignerTableScreen::new);
        event.register(ModMenus.DECOMPOSTER.get(), DecomposterScreen::new);
        event.register(ModMenus.BUTCHERS_TABLE.get(), ButchersTableScreen::new);
        event.register(ModMenus.COLD_STORAGE.get(), ColdStorageScreen::new);
        event.register(ModMenus.GRILL.get(), GrillScreen::new);
        event.register(ModMenus.CITY_CHEST.get(), CityChestScreen::new);
        event.register(ModMenus.MAILBOX_OWNER.get(), MailboxScreen::new);
        event.register(ModMenus.MAILBOX_SENDER.get(), MailboxScreen::new);
        event.register(ModMenus.LIBRARIAN_TABLE.get(), LibrarianTableScreen::new);
        event.register(ModMenus.KNOWLEDGE_CREATOR.get(), KnowledgeCreatorScreen::new);
        event.register(ModMenus.FOOD_TROUGH.get(), FoodTroughScreen::new);
        event.register(ModMenus.WATER_BOWL.get(), WaterBowlScreen::new);
        event.register(ModMenus.WHISPERER.get(), WhispererScreen::new);
        event.register(ModMenus.BLOOD_MERCHANT.get(), BloodMerchantScreen::new);
        event.register(ModMenus.TRAVELING_MERCHANT.get(), TravelingMerchantScreen::new);
        event.register(ModMenus.FISHERMAN.get(), FishermanScreen::new);
        event.register(ModMenus.BOUNTY_CLERK.get(), BountyClerkScreen::new);
        event.register(ModMenus.MAYORAL_CHEST.get(), MayoralChestScreen::new);
        event.register(ModMenus.SHOPFRONT_OWNER.get(), ShopfrontScreen::new);
        event.register(ModMenus.SHOPFRONT_CUSTOMER.get(), ShopfrontScreen::new);
    }

    // Juicer's juice window: one grayscale texture, tinted to whichever juice is inside.
    @SubscribeEvent
    public static void onRegisterBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return 0xFF000000 | state.getValue(JuicerBlock.JUICE).color();
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(JuicerBlock.JUICE);
            }
        }), ModBlocks.JUICER.get());
    }

    // Fermenter: the liquid in the glass jug takes the color of whichever liquor is fermenting.
    @SubscribeEvent
    public static void onRegisterFermenterTint(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return 0xFF000000 | state.getValue(FermenterBlock.LIQUOR).color();
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(FermenterBlock.LIQUOR);
            }
        }), ModBlocks.FERMENTER.get());
    }
}
