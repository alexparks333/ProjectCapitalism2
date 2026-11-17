package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

// The Banker, like Project Capitalism 1: any mob named "Banker" (/banker spawns one, or use a name tag) buys items.
//  - Right-click holding an item: sell one (one lot of an "N/M" item).
//  - Shift + right-click: sell the stack.
//  - /price (EconomyCommands): what the held item sells for, per unit.
// Prices are in gold nuggets, "N" a piece or "N/M" for N nuggets per M pieces (cheap things that come by the hundred),
// in the server config (economy.bankPricesV4, Economy.md). Only whole lots sell (the leftover few stay in your hand). A
// sale pays out as gold blocks, then ingots, then nuggets (GoldMoney.give), so a big sale isn't a pile of nuggets.
@EventBusSubscriber(modid = Capitalism.MODID)
public class BankerEvents {

    // what the bank pays: `nuggets` for every `per` of the item
    public record Price(int nuggets, int per) {
        // the most of `count` that sells (whole lots), and what they bring
        long lots(int count) {
            return count / per;
        }

        String text() {
            return per == 1 ? GoldMoney.format(nuggets) + " each" : GoldMoney.format(nuggets) + " for " + per;
        }
    }

    public static boolean isBanker(Entity entity) {
        return entity.hasCustomName() && entity.getCustomName() != null && entity.getCustomName().getString().contains("Banker");
    }

    // A Banker can never be killed: no damage of any kind gets through (not even /kill), and lightning doesn't turn it
    // into a witch. An operator takes one away with /banker remove.
    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (isBanker(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLightning(net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent event) {
        if (isBanker(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isBanker(event.getTarget())) return;

        // Always stop the normal interaction (e.g. a villager's trade screen), on both sides.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(EconomyCommands.tag("Bank").append(EconomyCommands.text("Hold an item to see its sell price.")));
            return;
        }

        Price price = prices().get(held.getItem());
        if (price == null || price.nuggets() <= 0) {
            player.sendSystemMessage(EconomyCommands.tag("Bank").append(EconomyCommands.error("I don't buy that.")));
            return;
        }

        String itemName = held.getHoverName().getString();
        long lots = price.lots(held.getCount());
        int selling = (int) (lots * price.per());
        long total = lots * price.nuggets();
        if (lots == 0) {
            player.sendSystemMessage(EconomyCommands.tag("Bank").append(EconomyCommands.text(itemName + ": " + price.text()
                + ". Bring at least " + price.per() + ".")));
            return;
        }

        // right-click sells one (one lot, for things priced "N for M"); shift + right-click sells the whole stack
        if (!player.isShiftKeyDown()) {
            selling = price.per();
            total = price.nuggets();
        }
        held.shrink(selling);
        GoldMoney.give(player, total);
        player.sendSystemMessage(EconomyCommands.tag("Bank").append(EconomyCommands.text("Sold "))
            .append(EconomyCommands.gold(selling + "x " + itemName)).append(EconomyCommands.text(" for "))
            .append(EconomyCommands.gold(GoldMoney.format(total))).append(EconomyCommands.text("!")));
    }

    // /price: what the Banker pays for the item in your main hand, per unit ("4 Lapis Lazuli for 1 gold nugget")
    public static net.minecraft.network.chat.MutableComponent priceMessage(ItemStack held) {
        if (held.isEmpty()) return EconomyCommands.tag("Price").append(EconomyCommands.text("Hold an item to see what the Banker pays for it."));
        Price price = prices().get(held.getItem());
        String itemName = held.getItem().getName(held).getString();
        if (price == null || price.nuggets() <= 0) {
            return EconomyCommands.tag("Price").append(EconomyCommands.error("The Banker doesn't buy " + itemName + "."));
        }
        return EconomyCommands.tag("Price").append(EconomyCommands.gold(price.per() + " " + itemName))
            .append(EconomyCommands.text(" for ")).append(EconomyCommands.gold(GoldMoney.format(price.nuggets())));
    }

    // Parses "minecraft:diamond=45" / "minecraft:melon_slice=1/3" lines from the config. Unknown items or bad lines are
    // skipped. Then no price is higher than an NPC shop could ever sell the item for (with every discount at once), so
    // buying from an NPC to sell to the bank can't make money.
    public static Map<Item, Price> prices() {
        Map<Item, Price> prices = new HashMap<>();
        for (String line : CapitalismConfig.BANK_PRICES.get()) {
            int split = line.lastIndexOf('=');
            if (split <= 0) continue;
            Identifier id = Identifier.tryParse(line.substring(0, split).trim());
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            String value = line.substring(split + 1).trim();
            try {
                int slash = value.indexOf('/');
                int nuggets = Integer.parseInt((slash < 0 ? value : value.substring(0, slash)).trim());
                int per = slash < 0 ? 1 : Integer.parseInt(value.substring(slash + 1).trim());
                if (per > 0) prices.put(BuiltInRegistries.ITEM.getValue(id), new Price(nuggets, per));
            } catch (NumberFormatException ignored) {
                // skip bad price
            }
        }
        for (String shop : SHOPS) {
            for (com.alexh.capitalism.npc.NpcShops.Offer offer : com.alexh.capitalism.npc.NpcShops.all(shop)) {
                Item item = offer.item().get();
                Price price = prices.get(item);
                if (price == null) continue;
                long lowest = NpcPricing.lowestNpcPrice(offer.fairNuggets());
                int per = Math.max(1, offer.per());
                // the bank's nuggets/per against the NPC's lowest/per (cross-multiplied)
                if ((long) price.nuggets() * per > lowest * price.per()) prices.put(item, new Price((int) lowest, per));
            }
        }
        // (the Traveling Merchant is left out on purpose: his 2 cheap diamonds / emeralds a visit would otherwise drag the
        // bank's price down for everyone; the most anyone could make is a few gold every week or two)
        return prices;
    }

    // every NPC shop: each job's, and the Boat Master's
    private static final java.util.List<String> SHOPS = java.util.stream.Stream.concat(
        java.util.Arrays.stream(com.alexh.capitalism.occupation.Job.values()).map(com.alexh.capitalism.occupation.Job::id),
        java.util.stream.Stream.of(com.alexh.capitalism.npc.NpcShops.BOAT_MASTER)).toList();
}
