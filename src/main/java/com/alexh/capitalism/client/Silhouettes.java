package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Draws an item as a solid black silhouette (the guides' "not discovered yet" look).
//  - Flat items: reads the item's model for its texture layers and draws each one tinted black.
//  - Blocks (3D models): works out the outline the inventory icon has. Every cube of the model (following its parents,
//    with each cube's own rotation) is turned and sized like the inventory does (the model's "gui" display, e.g.
//    tilted 30 degrees and turned 225), projected flat, and filled into a MASK x MASK mask, kept as a texture.
//  - Anything else: a dark square.
public final class Silhouettes {

    private static final int BLACK = 0xFF000000;
    private static final int MASK = 64;
    private static final Map<Item, Shape> SHAPES = new HashMap<>();

    // layers: flat item textures; mask: a block's outline texture (or null)
    private record Shape(List<Identifier> layers, @Nullable Identifier mask) {
        static final Shape NONE = new Shape(List.of(), null);
    }

    private Silhouettes() {
    }

    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        Shape shape = SHAPES.computeIfAbsent(stack.getItem(), Silhouettes::load);
        if (shape.mask() != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, shape.mask(), x, y, 0, 0, 16, 16, 16, 16, BLACK);
            return;
        }
        if (shape.layers().isEmpty()) {
            graphics.fill(x + 2, y + 2, x + 14, y + 14, BLACK);
            return;
        }
        for (Identifier texture : shape.layers()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 16, 16, 16, 16, BLACK);
        }
    }

    // items/<id>.json -> its model -> flat layers (layer0, layer1...) or, failing that, its cubes
    private static Shape load(Item item) {
        try {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            JsonObject definition = read(id.withPath("items/" + id.getPath() + ".json"));
            String modelName = definition == null ? null : findModel(definition.get("model"));
            if (modelName == null) return Shape.NONE;
            List<JsonObject> chain = modelChain(Identifier.parse(modelName));
            if (chain.isEmpty()) return Shape.NONE;
            List<Identifier> layers = new ArrayList<>();
            JsonObject textures = chain.getFirst().has("textures") ? chain.getFirst().getAsJsonObject("textures") : new JsonObject();
            for (int i = 0; textures.has("layer" + i); i++) {
                Identifier texture = Identifier.parse(textures.get("layer" + i).getAsString());
                layers.add(texture.withPath("textures/" + texture.getPath() + ".png"));
            }
            if (!layers.isEmpty()) return new Shape(layers, null);
            Identifier mask = blockMask(id, chain);
            return mask == null ? Shape.NONE : new Shape(List.of(), mask);
        } catch (RuntimeException broken) {
            Capitalism.LOGGER.debug("No silhouette for {}", item, broken);
            return Shape.NONE;
        }
    }

    // The model and its parents, child first
    private static List<JsonObject> modelChain(Identifier modelId) {
        List<JsonObject> chain = new ArrayList<>();
        Identifier next = modelId;
        for (int depth = 0; next != null && depth < 10; depth++) {
            JsonObject model = read(next.withPath("models/" + next.getPath() + ".json"));
            if (model == null) break;
            chain.add(model);
            next = model.has("parent") ? Identifier.parse(model.get("parent").getAsString()) : null;
        }
        return chain;
    }

    // ── Block outlines ──

    private static @Nullable Identifier blockMask(Identifier id, List<JsonObject> chain) {
        JsonArray elements = null;
        JsonObject gui = null;
        for (JsonObject model : chain) {
            if (elements == null && model.has("elements")) elements = model.getAsJsonArray("elements");
            if (gui == null && model.has("display") && model.getAsJsonObject("display").has("gui")) {
                gui = model.getAsJsonObject("display").getAsJsonObject("gui");
            }
        }
        if (elements == null || elements.isEmpty()) return null;

        // the inventory's turn and size: translate(t) * rotateXYZ(r) * scale(s), around the block's middle
        Vector3f rotation = vector(gui, "rotation", 0);
        Vector3f translation = vector(gui, "translation", 0).div(16.0F);
        Vector3f scale = vector(gui, "scale", 1);
        Matrix3f turn = new Matrix3f().rotationXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y),
            (float) Math.toRadians(rotation.z));

        NativeImage image = new NativeImage(MASK, MASK, true);
        for (int py = 0; py < MASK; py++) {
            for (int px = 0; px < MASK; px++) image.setPixel(px, py, 0);
        }
        for (JsonElement element : elements) {
            JsonObject cube = element.getAsJsonObject();
            Vector3f from = vector(cube, "from", 0);
            Vector3f to = vector(cube, "to", 16);
            List<float[]> corners = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                Vector3f v = new Vector3f((i & 1) == 0 ? from.x : to.x, (i & 2) == 0 ? from.y : to.y, (i & 4) == 0 ? from.z : to.z);
                rotateInModel(cube, v);
                // model units (0..16) -> block units around the middle -> the inventory's turn -> screen pixels
                v.div(16.0F).sub(0.5F, 0.5F, 0.5F).mul(scale);
                turn.transform(v);
                v.add(translation);
                corners.add(new float[] {(8 + 16 * v.x) * MASK / 16.0F, (8 - 16 * v.y) * MASK / 16.0F});
            }
            fill(image, hull(corners));
        }
        DynamicTexture texture = new DynamicTexture(() -> "capitalism silhouette " + id, image);
        Identifier key = Identifier.fromNamespaceAndPath(Capitalism.MODID, "silhouette/" + id.getNamespace() + "/" + id.getPath());
        Minecraft.getInstance().getTextureManager().register(key, texture);
        return key;
    }

    // A cube's own "rotation": {angle, axis, origin}
    private static void rotateInModel(JsonObject cube, Vector3f v) {
        if (!cube.has("rotation")) return;
        JsonObject r = cube.getAsJsonObject("rotation");
        float angle = (float) Math.toRadians(r.has("angle") ? r.get("angle").getAsFloat() : 0);
        if (angle == 0) return;
        Vector3f origin = vector(r, "origin", 8);
        String axis = r.has("axis") ? r.get("axis").getAsString().toLowerCase(Locale.ROOT) : "y";
        Matrix3f m = switch (axis) {
            case "x" -> new Matrix3f().rotationX(angle);
            case "z" -> new Matrix3f().rotationZ(angle);
            default -> new Matrix3f().rotationY(angle);
        };
        v.sub(origin);
        m.transform(v);
        v.add(origin);
    }

    private static Vector3f vector(@Nullable JsonObject object, String key, float fallback) {
        if (object == null || !object.has(key)) return new Vector3f(fallback);
        JsonArray a = object.getAsJsonArray(key);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    // Convex hull of the projected corners (Andrew's monotone chain), counter-clockwise
    private static List<float[]> hull(List<float[]> points) {
        List<float[]> p = new ArrayList<>(points);
        p.sort((a, b) -> a[0] != b[0] ? Float.compare(a[0], b[0]) : Float.compare(a[1], b[1]));
        List<float[]> out = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            int start = out.size();
            for (float[] q : p) {
                while (out.size() >= start + 2 && cross(out.get(out.size() - 2), out.get(out.size() - 1), q) <= 0) out.removeLast();
                out.add(q);
            }
            out.removeLast();
            p = p.reversed();
        }
        return out;
    }

    private static float cross(float[] o, float[] a, float[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    // Every mask pixel whose middle is inside the (convex) polygon goes solid
    private static void fill(NativeImage image, List<float[]> polygon) {
        if (polygon.size() < 3) return;
        for (int py = 0; py < MASK; py++) {
            for (int px = 0; px < MASK; px++) {
                float[] c = {px + 0.5F, py + 0.5F};
                boolean inside = true;
                boolean positive = false;
                boolean negative = false;
                for (int i = 0; i < polygon.size() && inside; i++) {
                    float side = cross(polygon.get(i), polygon.get((i + 1) % polygon.size()), c);
                    if (side > 0) positive = true;
                    if (side < 0) negative = true;
                    inside = !(positive && negative);
                }
                if (inside) image.setPixel(px, py, 0xFFFFFFFF);
            }
        }
    }

    // ── Reading model files ──

    // The first plain model name in an item definition (handles {"type": "minecraft:model", "model": "..."}).
    private static String findModel(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("model")) return findModel(object.get("model"));
        }
        return null;
    }

    private static JsonObject read(Identifier file) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception broken) {
            return null;
        }
    }
}
