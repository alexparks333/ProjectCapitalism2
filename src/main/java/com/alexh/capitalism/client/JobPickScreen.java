package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.network.PickJobPacket;
import com.alexh.capitalism.occupation.ClientJobData;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

// /pick (Jobs.md > Picking jobs): Essential jobs on the left, Fun jobs on the right, each column showing how many of its
// slots are filled (1 of each to start; a Wishing Star at the Wishing Well adds one). Click a job to pick it: a bar asks
// you to confirm, since it's for good (the server checks it all, occupation/JobCommands.togglePicked). While changing
// jobs (a Changing Star), click a job you hold to drop it. Hover a tile for all its pros and cons.
public class JobPickScreen extends Screen {

    private static final int TILE_W = 184;
    private static final int TILE_H = 22;
    private static final int GAP = 3;
    private static final int HEADER_Y = 22;

    private static final int PANEL = 0xFF3A2A1C;
    private static final int TRIM_LIGHT = 0xFFD4A040;
    private static final int TRIM_DARK = 0xFF7A5418;
    private static final int TILE = 0xFF5A3420;
    private static final int TILE_HOVER = 0xFF7A4A2C;
    private static final int PICKED = 0xFF2E5A28;
    private static final int PICKED_TRIM = 0xFF8AF09A;
    private static final int GOLD = 0xFFFFD24A;
    private static final int TEXT = 0xFFF2E2C0;
    private static final int TEXT_DIM = 0xFFBFA888;

    // the job waiting for "Yes" on the confirm bar, or null
    private Job confirming;

    public JobPickScreen() {
        super(Component.literal("Pick Your Jobs"));
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new JobPickScreen());
    }

    private static ItemStack icon(Job job) {
        return switch (job) {
            case COOK -> new ItemStack(Items.SMOKER);
            case BARTENDER -> new ItemStack(ModItems.BARTENDING_TABLE.get());
            case WEAPONSMITH -> new ItemStack(ModItems.WEAPONSMITH_TABLE.get());
            case ARMORSMITH -> new ItemStack(ModItems.ARMORSMITH_TABLE.get());
            case TOOLSMITH -> new ItemStack(ModItems.TOOLSMITH_TABLE.get());
            case ENCHANTER -> new ItemStack(ModItems.ENCHANTERS_TABLE.get());
            case LIBRARIAN -> new ItemStack(Items.LECTERN);
            case PET_TRAINER -> new ItemStack(ModItems.TRAINING_STICK.get());
            case DESIGNER -> new ItemStack(ModItems.SEWING_MACHINE.get());
            case FARMER -> new ItemStack(ModItems.SCYTHE.get());
            case REAL_ESTATE_AGENT -> new ItemStack(ModItems.FOR_SALE_SIGN.get());
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // The jobs in this column, in their usual order
    private static List<Job> column(Job.Category category) {
        List<Job> jobs = new ArrayList<>();
        for (Job job : Job.values()) {
            if (job.category == category) jobs.add(job);
        }
        return jobs;
    }

    private static int rows() {
        int rows = 0;
        for (Job.Category category : Job.Category.values()) rows = Math.max(rows, column(category).size());
        return rows;
    }

    private int panelW() {
        int cols = Job.Category.values().length;
        return cols * TILE_W + (cols + 1) * 12;
    }

    private int panelH() {
        return HEADER_Y + 14 + rows() * (TILE_H + GAP) - GAP + 30;
    }

    private int tileX(Job.Category category) {
        return (this.width - panelW()) / 2 + 12 + category.ordinal() * (TILE_W + 12);
    }

    private int tileY(int row) {
        return (this.height - panelH()) / 2 + HEADER_Y + 14 + row * (TILE_H + GAP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int l = (this.width - panelW()) / 2;
        int t = (this.height - panelH()) / 2;
        graphics.fill(l - 2, t - 2, l + panelW() + 2, t + panelH() + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + panelW() + 1, t + panelH() + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + panelW(), t + panelH(), PANEL);
        graphics.centeredText(this.font, this.title, l + panelW() / 2, t + 7, GOLD);

        Job hovered = null;
        for (Job.Category category : Job.Category.values()) {
            int x = tileX(category);
            // column header: "Essential Jobs - pick 1"
            String header = category.title + " Jobs";
            graphics.text(this.font, header, x + 2, t + HEADER_Y, GOLD, false);
            int have = ClientJobData.count(category);
            int slots = ClientJobData.slots(category);
            String rule = have + " of " + slots + " picked";
            graphics.text(this.font, rule, x + TILE_W - 2 - this.font.width(rule), t + HEADER_Y, TEXT_DIM, false);
            graphics.fill(x, t + HEADER_Y + 11, x + TILE_W, t + HEADER_Y + 12, TRIM_DARK);
            List<Job> jobs = column(category);
            for (int row = 0; row < jobs.size(); row++) {
                Job job = jobs.get(row);
                int y = tileY(row);
                boolean hover = inside(mouseX, mouseY, x, y);
                boolean has = ClientJobData.has(job);
                // a full column (and not changing): its other jobs are out of reach
                boolean locked = !has && ClientJobData.count(category) >= ClientJobData.slots(category);
                if (hover) hovered = job;
                graphics.fill(x, y, x + TILE_W, y + TILE_H, has ? PICKED_TRIM : hover ? TRIM_LIGHT : TRIM_DARK);
                graphics.fill(x + 1, y + 1, x + TILE_W - 1, y + TILE_H - 1, has ? PICKED : hover && !locked ? TILE_HOVER : TILE);
                if (locked) graphics.fill(x + 1, y + 1, x + TILE_W - 1, y + TILE_H - 1, 0x80000000);
                graphics.item(icon(job), x + 4, y + 3);
                graphics.text(this.font, job.title, x + 24, y + 3, TEXT, false);
                if (has) graphics.text(this.font, "✔", x + TILE_W - 12, y + 3, PICKED_TRIM, false);
                String summary = job.pros.isEmpty() ? "" : "+ " + job.pros.get(0);
                graphics.text(this.font, this.font.plainSubstrByWidth(summary, TILE_W - 30), x + 24, y + 12, TEXT_DIM, false);
            }
        }
        if (this.confirming != null) {
            // the confirm bar
            int by = t + panelH() - 22;
            graphics.fill(l + 8, by - 3, l + panelW() - 8, by + 15, 0xFF5A2A18);
            String q = "Become a " + this.confirming.title + "? This is for good.";
            graphics.text(this.font, q, l + 16, by + 2, GOLD, false);
            button(graphics, mouseX, mouseY, yesX(), by, "Yes", 0xFF2E7A28);
            button(graphics, mouseX, mouseY, noX(), by, "No", 0xFF7A2828);
        } else if (ClientJobData.changing) {
            graphics.centeredText(this.font, "Changing jobs: click a job you hold to drop it, then pick another.", l + panelW() / 2, t + panelH() - 16, GOLD);
        }

        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(hovered.title + " (" + hovered.category.title + ")").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (String pro : hovered.pros) lines.add(Component.literal("+ " + pro).withStyle(ChatFormatting.GREEN));
            for (String con : hovered.cons) lines.add(Component.literal("- " + con).withStyle(ChatFormatting.RED));
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    private int yesX() {
        return (this.width + panelW()) / 2 - 8 - 2 * 40 - 4;
    }

    private int noX() {
        return (this.width + panelW()) / 2 - 8 - 40;
    }

    private void button(GuiGraphicsExtractor graphics, int mx, int my, int x, int y, String label, int color) {
        boolean hover = mx >= x && mx < x + 36 && my >= y - 1 && my < y + 13;
        graphics.fill(x, y - 1, x + 36, y + 13, hover ? TRIM_LIGHT : TRIM_DARK);
        graphics.fill(x + 1, y, x + 35, y + 12, color);
        graphics.centeredText(this.font, label, x + 18, y + 2, TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        if (this.confirming != null) {
            int by = (this.height - panelH()) / 2 + panelH() - 22;
            if (my >= by - 1 && my < by + 13 && mx >= yesX() && mx < yesX() + 36) {
                ClientPacketDistributor.sendToServer(new PickJobPacket(this.confirming.ordinal()));
                this.confirming = null;
                return true;
            }
            if (my >= by - 1 && my < by + 13 && mx >= noX() && mx < noX() + 36) {
                this.confirming = null;
                return true;
            }
        }
        for (Job.Category category : Job.Category.values()) {
            List<Job> jobs = column(category);
            for (int row = 0; row < jobs.size(); row++) {
                if (inside(mx, my, tileX(category), tileY(row))) {
                    Job job = jobs.get(row);
                    if (ClientJobData.has(job)) {
                        // held: only a change (Changing Star) lets it go; the server says why not otherwise
                        ClientPacketDistributor.sendToServer(new PickJobPacket(job.ordinal()));
                        this.confirming = null;
                    } else if (ClientJobData.count(category) < ClientJobData.slots(category)) {
                        this.confirming = job;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int mx, int my, int x, int y) {
        return mx >= x && mx < x + TILE_W && my >= y && my < y + TILE_H;
    }
}
