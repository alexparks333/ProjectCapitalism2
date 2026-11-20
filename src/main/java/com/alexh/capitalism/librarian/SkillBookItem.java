package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

// A written skill book (Librarian.md), made at the Librarian Table (LibrarianTableMenu). Anyone can read it: hold
// right-click (10 seconds for a lesson, 25 for a trait; the book lies open on screen, client/ReadingHud; let go early
// and nothing happens), and it teaches its Skill and is used up. A lesson gives its effect for
// a while (Delicate: level II, for longer); a trait is learned for good (Traits), and a book of a trait you already
// know stays in your hand. Every book has its own cover (items/skill_book.json picks item/skill_books/<skill> by the
// CUSTOM_MODEL_DATA string, and a lesson's level numeral, I or II, in the corner by the second); traits shimmer. Bookworm makes lessons last 50% longer.
public class SkillBookItem extends Item {

    // How long reading takes: 10 seconds for a lesson (Temporary), 25 for a trait (Persistent)
    public static final int LESSON_READ_TICKS = 200;
    public static final int TRAIT_READ_TICKS = 500;

    public SkillBookItem(Properties properties) {
        super(properties);
    }

    public static int readTicks(Skill skill) {
        return skill.trait ? TRAIT_READ_TICKS : LESSON_READ_TICKS;
    }

    public static @Nullable SkillBook book(ItemStack stack) {
        return stack.get(ModComponents.SKILL_BOOK.get());
    }

    public static @Nullable Skill skill(ItemStack stack) {
        SkillBook book = book(stack);
        return book == null ? null : book.resolve();
    }

    // Write a book: what it teaches, its cover (and a lesson's level numeral), a shimmer if it's a trait
    public static ItemStack write(ItemStack stack, Skill skill, boolean delicate) {
        stack.set(ModComponents.SKILL_BOOK.get(), new SkillBook(skill.id(), delicate));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(List.of(), List.of(), skill.trait || skill.instant || skill.bloodMoon() ? List.of(skill.id()) : List.of(skill.id(), delicate ? "ii" : "i"), List.of(skill.color)));
        if (skill.trait) stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        Skill skill = skill(stack);
        if (skill == null) return Component.literal("Unwritten Book");
        return Component.literal(skill.title).withStyle(skill.trait ? ChatFormatting.GOLD : ChatFormatting.WHITE);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (skill(player.getItemInHand(hand)) == null) return InteractionResult.PASS;
        player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        Skill skill = skill(stack);
        if (skill == null) return 1;
        // Quick Study (Midnight Oil Martini): read in half the time
        return user.hasEffect(com.alexh.capitalism.core.ModEffects.QUICK_STUDY) ? readTicks(skill) / 2 : readTicks(skill);
    }

    // Pages turning while you read
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksLeft) {
        if (ticksLeft % 30 == 0) user.playSound(SoundEvents.BOOK_PAGE_TURN, 0.8F, 0.9F + user.getRandom().nextFloat() * 0.2F);
        if (level instanceof ServerLevel serverLevel && ticksLeft % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, user.getX(), user.getEyeY() - 0.2, user.getZ(), 3, 0.3, 0.2, 0.3, 0.4);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        SkillBook book = book(stack);
        Skill skill = book == null ? null : book.resolve();
        if (skill == null || !(user instanceof ServerPlayer player)) return stack;
        if (skill.trait) {
            if (!Traits.learn(player, skill)) {
                player.sendOverlayMessage(Component.literal("You already know " + skill.title + "."));
                return stack;
            }
            player.sendOverlayMessage(Component.literal("You learned " + skill.title + " for good!").withStyle(ChatFormatting.GOLD));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        } else if (skill.instant) {
            if (skill == Skill.FRESH_START) com.alexh.capitalism.player.WellnessEvents.freshStart(player);
            player.sendOverlayMessage(Component.literal("You read " + skill.title + ".").withStyle(ChatFormatting.GREEN));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
        } else {
            int ticks = book.delicate() ? Skill.DELICATE_TICKS : Skill.ROUGH_TICKS;
            if (Skills.has(player, Skill.BOOKWORM)) ticks = ticks * 3 / 2;
            // Quick Study (Midnight Oil Martini): the lesson lasts 25% longer
            if (player.hasEffect(ModEffects.QUICK_STUDY)) ticks = ticks * 5 / 4;
            // a Blood Moon Book: exactly one night, the same from a Rough or a Delicate Book
            if (skill.bloodMoon()) ticks = Skill.BLOOD_NIGHT_TICKS;
            int amplifier = skill.bloodMoon() ? 0 : book.delicate() ? 1 : 0;
            // no particles, no vanilla icon: it shows in Wellness > Buffs & Debuffs
            player.addEffect(new MobEffectInstance(ModEffects.BOOK_LESSONS.get(skill), ticks, amplifier, false, false, false));
            player.sendOverlayMessage(Component.literal("You read " + skill.title + "."));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
        }
        if (!player.isCreative()) stack.shrink(1);
        return stack;
    }
}
