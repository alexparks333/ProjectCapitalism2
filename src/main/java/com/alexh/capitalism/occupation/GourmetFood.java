package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.network.ThirstSyncPacket;
import com.alexh.capitalism.player.ThirstData;
import com.alexh.capitalism.player.WellnessEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.neoforge.network.PacketDistributor;

// Gourmet food (made by a Cook) and the golden food bar.
// Eating Gourmet food fills the normal food bar first; the drumsticks that don't fit become GOLD drumsticks
// (drained first, like the Bartender's gold drops), multiplied by how it was served:
//   by hand: x0.5,  off a Silver Plate: x1,  off a Golden Plate: x1.5   (config gourmetGold*)
// e.g. a Steak Roll (7 drumsticks) with a full bar: 3.5 gold by hand, 7 off a Silver Plate, 10 off a Golden Plate.
// Gourmet food can be eaten even with a full bar, so there's always a reason to go eat at a restaurant.
public final class GourmetFood {

    public enum Serving { HAND, SILVER_PLATE, GOLDEN_PLATE }

    private GourmetFood() {
    }

    public static double multiplier(Serving serving) {
        return switch (serving) {
            case HAND -> CapitalismConfig.GOURMET_GOLD_HAND.get();
            case SILVER_PLATE -> CapitalismConfig.GOURMET_GOLD_SILVER_PLATE.get();
            case GOLDEN_PLATE -> CapitalismConfig.GOURMET_GOLD_GOLDEN_PLATE.get();
        };
    }

    // Call after the food has been added to the bar. `wanted` = the food bar before + the food's hunger points;
    // whatever didn't fit (under the current top of the bar) becomes gold, times the serving's multiplier.
    public static void addGold(ServerPlayer player, int wanted, Serving serving) {
        ThirstData data = player.getData(ModAttachments.THIRST.get());
        int filled = Math.min(player.getFoodData().getFoodLevel(), data.foodCap);
        int overflow = wanted - filled;
        if (overflow <= 0) return;
        data.addGoldFood((int) Math.round(overflow * multiplier(serving)));
        PacketDistributor.sendToPlayer(player, ThirstSyncPacket.of(data, player));
    }

    // 5 points -> "2.5"
    private static String drumsticks(int points) {
        return points % 2 == 0 ? String.valueOf(points / 2) : (points / 2) + ".5";
    }

    // Eat one serving off a plate: fills the bar, gold for the rest, counts toward the diet.
    public static boolean eatFromPlate(ServerPlayer player, ItemStack serving, Serving plate) {
        FoodProperties food = serving.get(DataComponents.FOOD);
        if (food == null) return false;
        ThirstData data = player.getData(ModAttachments.THIRST.get());
        int before = player.getFoodData().getFoodLevel();
        int goldBefore = data.getGoldFood();
        float saturationBefore = player.getFoodData().getSaturationLevel();
        int wanted = before + food.nutrition();
        player.getFoodData().eat(food);
        // The meal's effects (Fire Resistance, Night Vision...), and its bowl back, same as eating it by hand
        Consumable consumable = serving.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            for (ConsumeEffect effect : consumable.onConsumeEffects()) effect.apply(player.level(), serving, player);
        }
        UseRemainder remainder = serving.get(DataComponents.USE_REMAINDER);
        if (remainder != null) {
            ItemStack bowl = remainder.convertInto().create();
            if (!player.getInventory().add(bowl)) player.drop(bowl, false, Prediction.SERVER_ONLY);
        }
        data.fillBonusFood(player.getFoodData(), wanted);
        addGold(player, wanted, plate);
        // the bar was already full: gold only, so a gold rush instead of saturation
        if (before >= data.foodCap) data.goldOnlyMeal(player.getFoodData(), saturationBefore);
        WellnessEvents.recordMeal(player, serving);

        // Make it feel like a meal: crumbs at the mouth, crunching, a happy burp, and what you got
        if (player.level() instanceof ServerLevel level) {
            var look = player.getLookAngle();
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, serving.getItem()),
                player.getX() + look.x * 0.4, player.getEyeY() - 0.15, player.getZ() + look.z * 0.4, 12, 0.12, 0.08, 0.12, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT.value(),
                SoundSource.PLAYERS, 0.9F, 0.9F + player.getRandom().nextFloat() * 0.2F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS, 0.6F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
        int filled = player.getFoodData().getFoodLevel() - before;
        int gold = data.getGoldFood() - goldBefore;
        String plateName = plate == Serving.GOLDEN_PLATE ? "Golden Plate" : "Silver Plate";
        StringBuilder got = new StringBuilder(serving.getHoverName().getString()).append(":");
        if (filled > 0) got.append(" +").append(drumsticks(filled)).append(" drumsticks");
        if (gold > 0) got.append(filled > 0 ? "," : "").append(" +").append(drumsticks(gold)).append(" gold");
        if (filled <= 0 && gold <= 0) got.append(" you're completely stuffed (gold bar full)");
        got.append(" (").append(plateName).append(")");
        player.sendOverlayMessage(Component.literal(got.toString()).withStyle(ChatFormatting.GOLD));
        return true;
    }
}
