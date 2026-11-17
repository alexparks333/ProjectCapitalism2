package com.alexh.capitalism.librarian;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// Traits from skill books (Skill.trait): learned for good, until you die: dying forgets them all (TraitData isn't
// copied on death), as dying loses the lessons too (they're effects). The attribute ones are put back on as permanent
// modifiers at login and respawn, and any for a trait you no longer know taken off; the rest are hooks (SkillEffects).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Traits {

    private Traits() {
    }

    public static TraitData of(Player player) {
        return player.getData(ModAttachments.TRAITS.get());
    }

    // Learn a trait (from reading its book): false if they already had it
    public static boolean learn(Player player, Skill skill) {
        TraitData data = of(player);
        if (!data.learn(skill)) return false;
        player.setData(ModAttachments.TRAITS.get(), data);
        apply(player);
        return true;
    }

    public static void apply(Player player) {
        // Iron Constitution (+1 heart) was a test trait, now gone: take its heart back off anyone who learned it
        AttributeInstance health = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (health != null) health.removeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath(Capitalism.MODID, "trait_iron_constitution"));
        java.util.Set<Skill> known = java.util.EnumSet.noneOf(Skill.class);
        known.addAll(of(player).traits());
        for (Skill skill : Skill.values()) {
            if (!skill.trait || skill.attribute == null) continue;
            AttributeInstance attribute = player.getAttribute(skill.attribute);
            if (attribute == null) continue;
            if (known.contains(skill)) attribute.addOrReplacePermanentModifier(new AttributeModifier(skill.modifierId(), skill.amount, skill.operation));
            else attribute.removeModifier(skill.modifierId());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }
}
