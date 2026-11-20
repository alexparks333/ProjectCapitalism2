package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.BankerEvents;
import com.alexh.capitalism.network.AdminPanelPacket;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// /admin panel (operators): the live admin panel (client/AdminPanelScreen). Three tabs:
//  - Settings: every server setting in capitalism-server.toml (CapitalismConfig), changed in game. A change is saved to
//    the file straight away and most take effect at once.
//  - Banker: everything the Banker buys and for how much (the bankPrices list), changed in game; "0" stops buying it.
//    "Add held item" puts what's in your hand on the list (at 0, not bought until you price it).
//  - Commands: every operator command with what it does; clicking one types it into chat.
// The client sends AdminSetPacket(key, value); only operators get anything done. Each change is logged and told to the
// admin who made it, then the panel's figures are sent again.
public final class AdminPanel {

    // fields in one row of the packet's lists
    public static final String SEP = "\u001F";
    // keys of the Banker's rows ("bank:minecraft:diamond") and of "add the item in my hand"
    public static final String BANK = "bank:";
    public static final String BANK_ADD = "bankadd";

    private AdminPanel() {
    }

    public static boolean allowed(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static void open(ServerPlayer player, boolean open) {
        PacketDistributor.sendToPlayer(player, new AdminPanelPacket(settings(), banker(), open));
    }

    // ── what the panel shows ──

    // every setting but the Banker's list (its own tab) and the list's version (not for changing):
    // "section<SEP>path<SEP>type (B I L D)<SEP>value<SEP>min<SEP>max<SEP>comment"
    public static List<String> settings() {
        List<String> rows = new ArrayList<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            Object now = value.get();
            String type = now instanceof Boolean ? "B" : now instanceof Integer ? "I" : now instanceof Long ? "L" : now instanceof Double ? "D" : null;
            if (type == null) continue;
            ModConfigSpec.Range<?> range = value.getSpec().getRange();
            List<String> path = value.getPath();
            rows.add(String.join(SEP, path.getFirst(), String.join(".", path), type, String.valueOf(now),
                range == null ? "" : String.valueOf(range.getMin()), range == null ? "" : String.valueOf(range.getMax()),
                comment(value.getSpec().getComment()), String.valueOf(value.getDefault())));
        }
        return rows;
    }

    // the Banker's list: "item id<SEP>its price line's value (N or N/M)<SEP>what it really pays (after the caps), or empty
    // if nothing"
    public static List<String> banker() {
        Map<Item, BankerEvents.Price> paid = BankerEvents.prices();
        List<String> rows = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String line : CapitalismConfig.BANK_PRICES.get()) {
            int split = line.lastIndexOf('=');
            if (split <= 0) continue;
            String id = line.substring(0, split).trim();
            Identifier key = Identifier.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key) || !seen.add(key.toString())) continue;
            BankerEvents.Price price = paid.get(BuiltInRegistries.ITEM.getValue(key));
            rows.add(String.join(SEP, key.toString(), line.substring(split + 1).trim(),
                price == null || price.nuggets() <= 0 ? "" : price.text(), CapitalismConfig.defaultBankPrice(key.toString())));
        }
        return rows;
    }

    private static String comment(@Nullable String comment) {
        if (comment == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : comment.split("\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("Range:") || l.startsWith("Default:") || l.startsWith("Allowed Values:")) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(l);
        }
        return out.toString();
    }

    static List<ModConfigSpec.ConfigValue<?>> values() {
        List<ModConfigSpec.ConfigValue<?>> out = new ArrayList<>();
        walk(CapitalismConfig.spec().getValues(), out);
        return out;
    }

    private static void walk(UnmodifiableConfig config, List<ModConfigSpec.ConfigValue<?>> out) {
        for (Object entry : config.valueMap().values()) {
            if (entry instanceof UnmodifiableConfig inner) walk(inner, out);
            else if (entry instanceof ModConfigSpec.ConfigValue<?> value && value != CapitalismConfig.BANK_PRICES_VERSION) out.add(value);
        }
    }

    // ── changing things ──

    // From the panel (or a test, player null): set a setting ("economy.investmentDailyPercent"), a Banker price
    // ("bank:minecraft:diamond" to "54" or "1/2"; "0" = not bought) or add the held item to the Banker's list.
    // Null if it worked, else why not. Saved to the file at once.
    public static @Nullable String apply(@Nullable ServerPlayer player, String key, String value) {
        value = value.trim();
        if (key.equals(BANK_ADD)) {
            if (player == null) return "Nobody's holding anything.";
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) return "Hold the item you want the Banker to buy.";
            String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            for (String line : CapitalismConfig.BANK_PRICES.get()) {
                if (line.startsWith(id + "=")) return held.getHoverName().getString() + " is already on the list.";
            }
            List<String> list = new ArrayList<>(CapitalismConfig.BANK_PRICES.get());
            list.add(id + "=0");
            CapitalismConfig.BANK_PRICES.set(list);
            CapitalismConfig.spec().save();
            return log(player, "Banker: added " + id + " (not bought until you give it a price)");
        }
        if (key.startsWith(BANK)) return setPrice(player, key.substring(BANK.length()), value);
        for (ModConfigSpec.ConfigValue<?> setting : values()) {
            if (!String.join(".", setting.getPath()).equals(key)) continue;
            Object old = setting.get();
            Object parsed;
            try {
                if (old instanceof Boolean) {
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) return "That's true or false.";
                    parsed = Boolean.parseBoolean(value);
                } else if (old instanceof Integer) parsed = Integer.parseInt(value);
                else if (old instanceof Long) parsed = Long.parseLong(value);
                else if (old instanceof Double) parsed = Double.parseDouble(value);
                else return "That one can't be changed here.";
            } catch (NumberFormatException e) {
                return "\"" + value + "\" isn't a " + (old instanceof Double ? "number" : "whole number") + ".";
            }
            ModConfigSpec.Range<?> range = setting.getSpec().getRange();
            if (!setting.getSpec().test(parsed)) {
                return range == null ? "That value isn't allowed." : "It has to be from " + range.getMin() + " to " + range.getMax() + ".";
            }
            if (parsed.equals(old)) return null;
            @SuppressWarnings("unchecked")
            ModConfigSpec.ConfigValue<Object> writable = (ModConfigSpec.ConfigValue<Object>) setting;
            writable.set(parsed);
            CapitalismConfig.spec().save();
            return log(player, key + ": " + old + " -> " + parsed);
        }
        return "No setting called " + key + ".";
    }

    private static @Nullable String setPrice(@Nullable ServerPlayer player, String id, String value) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return "No item called " + id + ".";
        if (!value.matches("\\d{1,7}(/\\d{1,5})?")) return "A price is nuggets each (\"54\"), or nuggets for so many (\"1/2\"). 0 = not bought.";
        if (value.contains("/") && Integer.parseInt(value.substring(value.indexOf('/') + 1)) <= 0) return "It can't be for 0 of them.";
        String name = key.toString();
        List<String> list = new ArrayList<>(CapitalismConfig.BANK_PRICES.get());
        String old = null;
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            int split = line.lastIndexOf('=');
            if (split > 0 && name.equals(normal(line.substring(0, split)))) {
                old = line.substring(split + 1).trim();
                list.set(i, name + "=" + value);
                break;
            }
        }
        if (old == null) list.add(name + "=" + value);
        if (value.equals(old)) return null;
        CapitalismConfig.BANK_PRICES.set(list);
        CapitalismConfig.spec().save();
        return log(player, "Banker: " + name + " " + (old == null ? "(new)" : old) + " -> " + value);
    }

    private static String normal(String id) {
        Identifier key = Identifier.tryParse(id.trim());
        return key == null ? id.trim() : key.toString();
    }

    // a change went through: the console's log, and the admin who made it
    private static @Nullable String log(@Nullable ServerPlayer player, String what) {
        String who = player == null ? "(test)" : player.getGameProfile().name();
        Capitalism.LOGGER.info("[Admin panel] {} changed {}", who, what);
        if (player != null) player.sendSystemMessage(Component.literal("[Admin] ").withStyle(ChatFormatting.RED)
            .append(Component.literal(what).withStyle(ChatFormatting.GRAY)));
        return null;
    }

    // the panel's Save (AdminSetPacket): every waiting change at once; operators only
    public static void handle(ServerPlayer player, List<String> keys, List<String> values) {
        if (!allowed(player)) return;
        for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
            String error = apply(player, keys.get(i), values.get(i));
            if (error != null) player.sendSystemMessage(Component.literal("[Admin] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(keys.get(i) + ": " + error).withStyle(ChatFormatting.YELLOW)));
        }
        open(player, false);
    }
}
