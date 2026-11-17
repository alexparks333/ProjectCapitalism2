package com.alexh.capitalism.farmer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

// A steak seared on the Grill (GrillMenu, client/SearGame): how done it came out (Doneness, from its final inside
// temperature: when it was pulled off) and how aged the cut was (Freshness: Dry-Aged / Prime). On the Seared item
// (ModComponents.SEAR). The aim is Medium Rare ("★ Perfectly Cooked": the most filling, the most gold); chicken's best
// is Well Done (anything less makes you ill). Its food value follows (food).
public record Sear(int doneness, int aged) {

    public static final Codec<Sear> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.fieldOf("doneness").forGetter(Sear::doneness),
        Codec.INT.fieldOf("aged").forGetter(Sear::aged)
    ).apply(inst, Sear::new));

    public static final StreamCodec<ByteBuf, Sear> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, Sear::doneness,
        ByteBufCodecs.VAR_INT, Sear::aged,
        Sear::new);

    // How done, by the inside temperature (°F) once it's rested. RAW is under Rare (cold in the middle); past Well
    // Done it's burnt (Burnt Food, not a steak). `bonus`: how much more filling it is cooked this way.
    public enum Doneness {
        RAW("Raw Inside", 0, 119, 0),
        RARE("Rare", 120, 129, 2),
        MEDIUM_RARE("Medium Rare", 130, 139, 4),
        MEDIUM("Medium", 140, 149, 2),
        MEDIUM_WELL("Medium Well", 150, 159, 1),
        WELL_DONE("Well Done", 160, 174, 0);

        public static final int BURNT = 175;

        public final String title;
        public final int min;
        public final int max;
        public final int bonus;

        Doneness(String title, int min, int max, int bonus) {
            this.title = title;
            this.min = min;
            this.max = max;
            this.bonus = bonus;
        }

        public static Doneness of(int temperature) {
            for (Doneness d : values()) if (temperature <= d.max) return d;
            return WELL_DONE;
        }
    }

    public Doneness done() {
        return Doneness.values()[Math.max(0, Math.min(Doneness.values().length - 1, this.doneness))];
    }

    // Cooked just right: Medium Rare (chicken: Well Done)
    public boolean perfect(MeatCut cut) {
        return this.done() == (cut.animal == MeatCut.Animal.CHICKEN ? Doneness.WELL_DONE : Doneness.MEDIUM_RARE);
    }

    // Chicken has to be cooked through: anything less than Well Done makes you ill
    public boolean unsafe(MeatCut cut) {
        return cut.animal == MeatCut.Animal.CHICKEN ? this.done() != Doneness.WELL_DONE : this.done() == Doneness.RAW;
    }

    // How filling it is: the animal and the cut's rarity, then how it's cooked (Medium Rare best) and its aging
    public FoodProperties food(MeatCut cut) {
        int nutrition = switch (cut.animal) {
            case COW, PIG -> 8;
            case SHEEP -> 7;
            case CHICKEN, RABBIT -> 6;
        } + cut.rarity;
        float saturation = 0.8F;
        nutrition += cut.animal == MeatCut.Animal.CHICKEN ? (this.perfect(cut) ? 4 : 0) : this.done().bonus;
        if (this.perfect(cut)) saturation += 0.2F;
        nutrition += this.aged * 2;
        saturation += this.aged * 0.1F;
        if (this.done() == Doneness.RAW) nutrition /= 2;
        nutrition = Math.max(1, nutrition);
        return new FoodProperties(nutrition, nutrition * saturation * 2.0F, true);
    }

    // A Prime Dry-Aged cut seared just right (Medium Rare; chicken: Well Done) makes you strong: Strength I for 2 minutes
    public boolean strengthens(MeatCut cut) {
        return this.aged >= 2 && this.perfect(cut);
    }

    public static final int STRENGTH_TICKS = 2 * 60 * 20;

    // Eating it: a perfect Prime steak gives Strength; undercooked (raw in the middle, or chicken that isn't cooked
    // through) gives you food poisoning
    public Consumable consumable(MeatCut cut) {
        if (this.strengthens(cut)) {
            return Consumables.defaultFood()
                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.STRENGTH, STRENGTH_TICKS, 0)))
                .build();
        }
        if (!this.unsafe(cut)) return Consumables.DEFAULT_FOOD;
        return Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(java.util.List.of(
                new MobEffectInstance(MobEffects.HUNGER, 600, 1),
                new MobEffectInstance(MobEffects.NAUSEA, 200, 0)), 0.8F))
            .build();
    }
}
