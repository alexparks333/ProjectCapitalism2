package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.menu.AssemblyTableMenu;
import com.alexh.capitalism.menu.BartendingTableMenu;
import com.alexh.capitalism.menu.OvenMenu;
import com.alexh.capitalism.menu.FermenterMenu;
import com.alexh.capitalism.economy.ShopfrontMenu;
import com.alexh.capitalism.menu.JuicerMenu;
import com.alexh.capitalism.menu.PurificationTableMenu;
import com.alexh.capitalism.npc.NpcShopMenu;
import com.alexh.capitalism.enchanter.EnchantersTableMenu;
import com.alexh.capitalism.enchanter.LapisCompressorMenu;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.weaponsmith.LavaCreatorMenu;
import com.alexh.capitalism.weaponsmith.SmithTableMenu;
import com.alexh.capitalism.weaponsmith.SmithTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Capitalism.MODID);

    public static final Supplier<MenuType<PurificationTableMenu>> PURIFICATION_TABLE =
        MENUS.register("purification_table", () -> new MenuType<>(PurificationTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<JuicerMenu>> JUICER =
        MENUS.register("juicer", () -> new MenuType<>(JuicerMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<BartendingTableMenu>> BARTENDING_TABLE =
        MENUS.register("bartending_table", () -> new MenuType<>(BartendingTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<AssemblyTableMenu>> SUSHI_TABLE =
        MENUS.register("sushi_table", () -> new MenuType<>(AssemblyTableMenu::sushi, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<AssemblyTableMenu>> COOKING_TABLE =
        MENUS.register("cooking_table", () -> new MenuType<>(AssemblyTableMenu::cooking, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<OvenMenu>> OVEN =
        MENUS.register("oven", () -> new MenuType<>(OvenMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<FermenterMenu>> FERMENTER =
        MENUS.register("fermenter", () -> new MenuType<>(FermenterMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<SmithTableMenu>> WEAPONSMITH_TABLE =
        MENUS.register("weaponsmith_table", () -> new MenuType<>(
            (id, inventory) -> new SmithTableMenu(SmithTable.WEAPONSMITH, id, inventory), FeatureFlags.VANILLA_SET));

    // An NPC's shop: the client is told which shop (a job's id, or the Boat Master's) so it can list the offers
    public static final Supplier<MenuType<NpcShopMenu>> NPC_SHOP =
        MENUS.register("npc_shop", () -> IMenuTypeExtension.create(
            (id, inventory, buf) -> new NpcShopMenu(id, inventory, buf.readUtf(), null)));

    public static final Supplier<MenuType<EnchantersTableMenu>> ENCHANTERS_TABLE =
        MENUS.register("enchanters_table", () -> new MenuType<>(EnchantersTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<LapisCompressorMenu>> LAPIS_COMPRESSOR =
        MENUS.register("lapis_compressor", () -> new MenuType<>(LapisCompressorMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.librarian.StitchingTableMenu>> STITCHING_TABLE =
        MENUS.register("stitching_table", () -> new MenuType<>(com.alexh.capitalism.librarian.StitchingTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.librarian.LibrarianTableMenu>> LIBRARIAN_TABLE =
        MENUS.register("librarian_table", () -> new MenuType<>(com.alexh.capitalism.librarian.LibrarianTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.librarian.KnowledgeCreatorMenu>> KNOWLEDGE_CREATOR =
        MENUS.register("knowledge_creator", () -> new MenuType<>(com.alexh.capitalism.librarian.KnowledgeCreatorMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.town.WhispererMenu>> WHISPERER =
        MENUS.register("whisperer", () -> new MenuType<>(com.alexh.capitalism.town.WhispererMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<MenuType<com.alexh.capitalism.town.TravelingMerchantMenu>> TRAVELING_MERCHANT =
        MENUS.register("traveling_merchant", () -> IMenuTypeExtension.create(com.alexh.capitalism.town.TravelingMerchantMenu::new));
    public static final Supplier<MenuType<com.alexh.capitalism.town.BloodMerchantMenu>> BLOOD_MERCHANT =
        MENUS.register("blood_merchant", () -> new MenuType<>(com.alexh.capitalism.town.BloodMerchantMenu::new, FeatureFlags.VANILLA_SET));
    // the Fisherman (town/FishermanMenu): selling him rare catches, mounting one on a plaque
    public static final Supplier<MenuType<com.alexh.capitalism.town.FishermanMenu>> FISHERMAN =
        MENUS.register("fisherman", () -> new MenuType<>(com.alexh.capitalism.town.FishermanMenu::new, FeatureFlags.VANILLA_SET));
    // the Mayoral Chest (town/MayoralChestMenu): its six rows, in the town's colours
    public static final Supplier<MenuType<com.alexh.capitalism.town.MayoralChestMenu>> MAYORAL_CHEST =
        MENUS.register("mayoral_chest", () -> new MenuType<>(com.alexh.capitalism.town.MayoralChestMenu::new, FeatureFlags.VANILLA_SET));
    // the Bounty Clerk (town/BountyMenu): today's bounty task, your open tasks, the ledger
    public static final Supplier<MenuType<com.alexh.capitalism.town.BountyMenu>> BOUNTY_CLERK =
        MENUS.register("bounty_clerk", () -> new MenuType<>(com.alexh.capitalism.town.BountyMenu::new, FeatureFlags.VANILLA_SET));

    // Pet Trainer: a Food Trough (its number of slots comes with it) and a Water Bowl (how much water is in it)
    public static final Supplier<MenuType<com.alexh.capitalism.pets.FoodTroughMenu>> FOOD_TROUGH =
        MENUS.register("food_trough", () -> IMenuTypeExtension.create(
            (id, inventory, buf) -> new com.alexh.capitalism.pets.FoodTroughMenu(id, inventory, buf.readVarInt())));

    public static final Supplier<MenuType<com.alexh.capitalism.pets.WaterBowlMenu>> WATER_BOWL =
        MENUS.register("water_bowl", () -> new MenuType<>(com.alexh.capitalism.pets.WaterBowlMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<LavaCreatorMenu>> LAVA_CREATOR =
        MENUS.register("lava_creator", () -> new MenuType<>(LavaCreatorMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<SmithTableMenu>> TOOLSMITH_TABLE =
        MENUS.register("toolsmith_table", () -> new MenuType<>(
            (id, inventory) -> new SmithTableMenu(SmithTable.TOOLSMITH, id, inventory), FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<SmithTableMenu>> ARMORSMITH_TABLE =
        MENUS.register("armorsmith_table", () -> new MenuType<>(
            (id, inventory) -> new SmithTableMenu(SmithTable.ARMORSMITH, id, inventory), FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<ShopfrontMenu>> SHOPFRONT_OWNER =
        MENUS.register("shopfront_owner", () -> new MenuType<>(ShopfrontMenu::ownerClient, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<ShopfrontMenu>> SHOPFRONT_CUSTOMER =
        MENUS.register("shopfront_customer", () -> new MenuType<>(ShopfrontMenu::customerClient, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.designer.DesignerTableMenu>> DESIGNER_TABLE =
        MENUS.register("designer_table", () -> new MenuType<>(com.alexh.capitalism.designer.DesignerTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.designer.SewingMachineMenu>> SEWING_MACHINE =
        MENUS.register("sewing_machine", () -> new MenuType<>(com.alexh.capitalism.designer.SewingMachineMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.farmer.DecomposterMenu>> DECOMPOSTER =
        MENUS.register("decomposter", () -> new MenuType<>(com.alexh.capitalism.farmer.DecomposterMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.realestate.CityChestMenu>> CITY_CHEST =
        MENUS.register("city_chest", () -> new MenuType<>(com.alexh.capitalism.realestate.CityChestMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.mail.MailboxMenu>> MAILBOX_OWNER =
        MENUS.register("mailbox_owner", () -> new MenuType<>(com.alexh.capitalism.mail.MailboxMenu::ownerClient, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.mail.MailboxMenu>> MAILBOX_SENDER =
        MENUS.register("mailbox_sender", () -> new MenuType<>(com.alexh.capitalism.mail.MailboxMenu::senderClient, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.farmer.GrillMenu>> GRILL =
        MENUS.register("grill", () -> new MenuType<>(com.alexh.capitalism.farmer.GrillMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.farmer.ColdStorageMenu>> COLD_STORAGE =
        MENUS.register("cold_storage", () -> new MenuType<>(com.alexh.capitalism.farmer.ColdStorageMenu::new, FeatureFlags.VANILLA_SET));

    public static final Supplier<MenuType<com.alexh.capitalism.farmer.ButchersTableMenu>> BUTCHERS_TABLE =
        MENUS.register("butchers_table", () -> new MenuType<>(com.alexh.capitalism.farmer.ButchersTableMenu::new, FeatureFlags.VANILLA_SET));
}
