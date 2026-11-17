package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Which pixels of an item's 16x16 icon are solid, read once from its texture (textures/item/<id>.png, or
// <id>_standby.png for crossbows). Used by the dent game (dents only land on the weapon) and the smith tables'
// faded background picture. Falls back to a sword-like diagonal if the texture can't be read.
final class ItemSilhouette {

    private static final Map<Item, List<int[]>> SOLID_PIXELS = new HashMap<>();

    private ItemSilhouette() {
    }

    // {x, y} on a 16x16 grid
    static List<int[]> pixels(Item item) {
        return SOLID_PIXELS.computeIfAbsent(item, ItemSilhouette::read);
    }

    private static List<int[]> read(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        List<int[]> solid = new ArrayList<>();
        for (String suffix : new String[] {"", "_standby"}) {
            Identifier texture = id.withPath("textures/item/" + id.getPath() + suffix + ".png");
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) continue;
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                int step = Math.max(1, image.getWidth() / 16);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        if (ARGB.alpha(image.getPixel(x * step, y * step)) > 128) solid.add(new int[] {x, y});
                    }
                }
            } catch (Exception e) {
                Capitalism.LOGGER.debug("Couldn't read {} for its silhouette", texture, e);
            }
            if (!solid.isEmpty()) return solid;
        }
        for (int i = 2; i <= 13; i++) solid.add(new int[] {i, 15 - i});
        return solid;
    }
}
