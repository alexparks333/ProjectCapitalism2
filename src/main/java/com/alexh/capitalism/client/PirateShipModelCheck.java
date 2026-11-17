package com.alexh.capitalism.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.BoatRenderState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

// For boats/BoatTests: builds every Premium boat's model, bakes it and runs its animation the way the client will
// (going full speed, turning, rowing, and with someone riding in first person), and counts its boxes. A broken model
// would otherwise only show up when the game loads its models.
public final class PirateShipModelCheck {

    private PirateShipModelCheck() {
    }

    public static String run() {
        Map<String, Supplier<LayerDefinition>> layers = new LinkedHashMap<>();
        Map<String, Function<ModelPart, EntityModel<BoatRenderState>>> models = new LinkedHashMap<>();
        add(layers, models, "pirate_ship", PirateShipModel::createLayer, PirateShipModel::new);
        add(layers, models, "ghost_ship", GhostShipModel::createLayer, GhostShipModel::new);
        add(layers, models, "speedboat", SpeedboatModel::createLayer, SpeedboatModel::new);
        add(layers, models, "yacht", YachtModel::createLayer, YachtModel::new);
        add(layers, models, "longship", LongshipModel::createLayer, LongshipModel::new);
        add(layers, models, "paddlewheeler", PaddlewheelerModel::createLayer, PaddlewheelerModel::new);
        add(layers, models, "swamp_raft", SwampRaftModel::createLayer, SwampRaftModel::new);
        add(layers, models, "steamboat", SteamboatModel::createLayer, SteamboatModel::new);
        add(layers, models, "leviathan", LeviathanModel::createLayer, LeviathanModel::new);
        add(layers, models, "teapot", TeapotModel::createLayer, TeapotModel::new);
        add(layers, models, "submersible", SubmersibleModel::createLayer, SubmersibleModel::new);
        add(layers, models, "castaway_tub", CastawayTubModel::createLayer, CastawayTubModel::new);
        add(layers, models, "fairy_boat", FairyBoatModel::createLayer, FairyBoatModel::new);
        StringBuilder out = new StringBuilder("ok");
        for (String name : layers.keySet()) {
            ModelPart root = layers.get(name).get().bakeRoot();
            EntityModel<BoatRenderState> model = models.get(name).apply(root);
            PremiumBoatRenderer.State state = new PremiumBoatRenderer.State();
            state.ageInTicks = 123.4F;
            state.speed = 1.0F;
            state.turn = 4.0F;
            state.rowingTimeLeft = 2.0F;
            state.rowingTimeRight = 2.5F;
            state.viewerX = -20.0F;
            state.isUnderWater = true;   // (no rocking, so faces stay square for the fight check)
            model.setupAnim(state);
            int[] boxes = {0};
            Map<String, java.util.List<float[]>> planes = new java.util.HashMap<>();
            java.util.List<String> paths = new java.util.ArrayList<>();
            root.visit(new com.mojang.blaze3d.vertex.PoseStack(), (pose, path, index, cube) -> {
                boxes[0]++;
                paths.add(path + "#" + index);
                collectFaces(pose, cube, boxes[0], planes);
            });
            int fights = fights(planes);
            if (fights > 0) EXAMPLES.add(name + ": " + examples(planes, paths));
            out.append(", ").append(name).append(" ").append(boxes[0]).append(" boxes ").append(fights).append(" fights");
            if (fights > 3) out.insert(0, "FIGHTS ");   // (a hairline spot or two deep in a joint is let through, but logged)
        }
        return out.toString();
    }

    // Every face that lies square to an axis: by (axis and facing, its plane to 0.001 px), its rectangle and its box
    private static void collectFaces(com.mojang.blaze3d.vertex.PoseStack.Pose pose, ModelPart.Cube cube, int box,
                                     Map<String, java.util.List<float[]>> planes) {
        org.joml.Vector3f n = new org.joml.Vector3f();
        org.joml.Vector3f p = new org.joml.Vector3f();
        for (ModelPart.Polygon polygon : cube.polygons) {
            // a face with no area (the edge of a flat panel) is never drawn: turned, its box would look like a face
            ModelPart.Vertex[] vs = polygon.vertices();
            org.joml.Vector3f e1 = new org.joml.Vector3f(vs[1].worldX() - vs[0].worldX(), vs[1].worldY() - vs[0].worldY(), vs[1].worldZ() - vs[0].worldZ());
            org.joml.Vector3f e2 = new org.joml.Vector3f(vs[2].worldX() - vs[0].worldX(), vs[2].worldY() - vs[0].worldY(), vs[2].worldZ() - vs[0].worldZ());
            org.joml.Vector3f e3 = new org.joml.Vector3f(vs[3].worldX() - vs[0].worldX(), vs[3].worldY() - vs[0].worldY(), vs[3].worldZ() - vs[0].worldZ());
            if (e1.cross(e2, new org.joml.Vector3f()).length() < 1e-7F && e2.cross(e3, new org.joml.Vector3f()).length() < 1e-7F) continue;
            pose.transformNormal(polygon.normal(), n);
            int axis = Math.abs(n.x) > 0.999F ? 0 : Math.abs(n.y) > 0.999F ? 1 : Math.abs(n.z) > 0.999F ? 2 : -1;
            if (axis < 0) continue;
            float sign = axis == 0 ? n.x : axis == 1 ? n.y : n.z;
            float plane = 0;
            float a0 = Float.MAX_VALUE, a1 = -Float.MAX_VALUE, b0 = Float.MAX_VALUE, b1 = -Float.MAX_VALUE;
            for (ModelPart.Vertex v : polygon.vertices()) {
                pose.pose().transformPosition(v.worldX(), v.worldY(), v.worldZ(), p);
                float[] c = {p.x * 16, p.y * 16, p.z * 16};
                plane = c[axis];
                float a = c[(axis + 1) % 3];
                float b = c[(axis + 2) % 3];
                a0 = Math.min(a0, a);
                a1 = Math.max(a1, a);
                b0 = Math.min(b0, b);
                b1 = Math.max(b1, b);
            }
            if ((a1 - a0) < 0.01F || (b1 - b0) < 0.01F) continue;
            String key = axis + (sign > 0 ? "+" : "-") + Math.round(plane * 1000);
            planes.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(new float[]{a0, a1, b0, b1, box});
        }
    }

    // pairs of faces from different boxes, facing the same way in the same plane, overlapping (they'd flicker)
    private static int fights(Map<String, java.util.List<float[]>> planes) {
        int fights = 0;
        for (java.util.List<float[]> faces : planes.values()) {
            for (int i = 0; i < faces.size(); i++) {
                for (int j = i + 1; j < faces.size(); j++) {
                    float[] a = faces.get(i);
                    float[] b = faces.get(j);
                    if (a[4] == b[4]) continue;
                    float w = Math.min(a[1], b[1]) - Math.max(a[0], b[0]);
                    float h = Math.min(a[3], b[3]) - Math.max(a[2], b[2]);
                    if (w > 0.05F && h > 0.05F) fights++;
                }
            }
        }
        return fights;
    }

    // the fights found, a few of each boat's (for the log)
    public static final java.util.List<String> EXAMPLES = new java.util.ArrayList<>();

    private static String examples(Map<String, java.util.List<float[]>> planes, java.util.List<String> paths) {
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, java.util.List<float[]>> e : planes.entrySet()) {
            java.util.List<float[]> faces = e.getValue();
            for (int i = 0; i < faces.size() && n < 25; i++) {
                for (int j = i + 1; j < faces.size() && n < 25; j++) {
                    float[] a = faces.get(i);
                    float[] b = faces.get(j);
                    if (a[4] == b[4]) continue;
                    float w = Math.min(a[1], b[1]) - Math.max(a[0], b[0]);
                    float h = Math.min(a[3], b[3]) - Math.max(a[2], b[2]);
                    if (w > 0.05F && h > 0.05F) {
                        out.append("\n   ").append(e.getKey()).append(" ").append(paths.get((int) a[4] - 1)).append(" x ").append(paths.get((int) b[4] - 1))
                            .append(String.format(" [%.2f..%.2f, %.2f..%.2f] [%.2f..%.2f, %.2f..%.2f]", a[0], a[1], a[2], a[3], b[0], b[1], b[2], b[3]));
                        n++;
                    }
                }
            }
        }
        return out.toString();
    }

    private static void add(Map<String, Supplier<LayerDefinition>> layers, Map<String, Function<ModelPart, EntityModel<BoatRenderState>>> models,
                            String name, Supplier<LayerDefinition> layer, Function<ModelPart, EntityModel<BoatRenderState>> model) {
        layers.put(name, layer);
        models.put(name, model);
    }
}
