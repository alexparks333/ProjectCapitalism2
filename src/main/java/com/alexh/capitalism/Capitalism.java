package com.alexh.capitalism;

import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.core.ModEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.core.ModLootModifiers;
import com.alexh.capitalism.core.ModMenus;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(Capitalism.MODID)
public class Capitalism {

    public static final String MODID = "capitalism";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Capitalism(IEventBus modEventBus, ModContainer modContainer) {
        CapitalismConfig.register(modContainer);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        com.alexh.capitalism.core.ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModLootModifiers.SERIALIZERS.register(modEventBus);
        com.alexh.capitalism.gametest.GameTests.init();
        com.alexh.capitalism.gametest.GameTests.TEST_FUNCTIONS.register(modEventBus);
        LOGGER.info("Project Capitalism 2 loaded.");
    }
}
