package com.alexh.capitalism.client;

import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.occupation.ClientJobData;
import com.alexh.capitalism.occupation.Job;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

// Opened by /guides: a tile for every guide you can read. Job guides only show for players with that job (the
// General Guide is for everyone). Click one to open it; Esc in a guide comes back here.
public class GuidesScreen extends Screen {

    // job = null: everyone sees it
    private record Guide(String title, String description, @Nullable Job job, Supplier<ItemStack> icon, Function<Screen, Screen> open) {
    }

    // Add future guides here.
    private static final List<Guide> GUIDES = List.of(
        new Guide("General Guide", "Shops, gold & server basics", null,
            () -> new ItemStack(ModItems.SHOPFRONT.get()), GeneralGuideScreen::new),
        new Guide("Bartender's Guide", "Juices, liquors & drinks", Job.BARTENDER,
            () -> new ItemStack(ModItems.BARTENDING_TABLE.get()), BartendersGuideScreen::new),
        new Guide("Cook's Guide", "Kitchen, sushi & oven meals", Job.COOK,
            () -> new ItemStack(ModItems.OVEN.get()), CooksGuideScreen::new),
        new Guide("Weaponsmith's Guide", "Forge, sharpening & weapons", Job.WEAPONSMITH,
            () -> new ItemStack(ModItems.WEAPONSMITH_TABLE.get()), WeaponsmithsGuideScreen::new),
        new Guide("Toolsmith's Guide", "Tools, efficiency & rerolls", Job.TOOLSMITH,
            () -> new ItemStack(ModItems.TOOLSMITH_TABLE.get()), ToolsmithsGuideScreen::new),
        new Guide("Armorsmith's Guide", "Armor, dents & socket gems", Job.ARMORSMITH,
            () -> new ItemStack(ModItems.ARMORSMITH_TABLE.get()), ArmorsmithsGuideScreen::new),
        new Guide("Enchanter's Guide", "Level 30, XP & removal", Job.ENCHANTER,
            () -> new ItemStack(ModItems.ENCHANTERS_TABLE.get()), EnchantersGuideScreen::new),
        new Guide("Librarian's Guide", "Enchanted books", Job.LIBRARIAN,
            () -> new ItemStack(Items.LECTERN), LibrariansGuideScreen::new),
        new Guide("Pet Trainer's Guide", "Claiming & training pets", Job.PET_TRAINER,
            () -> new ItemStack(ModItems.TRAINING_STICK.get()), PetTrainersGuideScreen::new),
        new Guide("Designer's Guide", "Patches, sewing & bags", Job.DESIGNER,
            () -> new ItemStack(ModItems.SEWING_MACHINE.get()), DesignersGuideScreen::new),
        new Guide("Farmer's Guide", "Harvests, fertilizer & butchering", Job.FARMER,
            () -> new ItemStack(ModItems.SCYTHE.get()), FarmersGuideScreen::new),
        new Guide("Real Estate Agent's Guide", "Lots, signs, selling & leasing", Job.REAL_ESTATE_AGENT,
            () -> new ItemStack(ModItems.FOR_SALE_SIGN.get()), RealEstateGuideScreen::new)
    );

    // The guides this player can see right now (jobs can change while the game is running).
    private static List<Guide> visible() {
        return GUIDES.stream().filter(guide -> guide.job() == null || ClientJobData.has(guide.job())).toList();
    }

    private static final int TILE_W = 176;
    private static final int TILE_H = 36;
    private static final int GAP = 8;

    private static final int PANEL = 0xFF3A2A1C;
    private static final int TRIM_LIGHT = 0xFFD4A040;
    private static final int TRIM_DARK = 0xFF7A5418;
    private static final int TILE = 0xFF5A3420;
    private static final int TILE_HOVER = 0xFF7A4A2C;
    private static final int GOLD = 0xFFFFD24A;
    private static final int TEXT = 0xFFF2E2C0;
    private static final int TEXT_DIM = 0xFFBFA888;

    public GuidesScreen() {
        super(Component.literal("Guides"));
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new GuidesScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // two columns once there are more than 4 guides
    private static int cols() {
        return visible().size() > 4 ? 2 : 1;
    }

    private int panelW() {
        int cols = cols();
        return cols * TILE_W + (cols - 1) * GAP + 24;
    }

    private int panelH() {
        int rows = (visible().size() + cols() - 1) / cols();
        return rows * TILE_H + (rows - 1) * GAP + 40;
    }

    private int tileX(int i) {
        return (this.width - panelW()) / 2 + 12 + (i % cols()) * (TILE_W + GAP);
    }

    private int tileY(int i) {
        return (this.height - panelH()) / 2 + 28 + (i / cols()) * (TILE_H + GAP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int l = (this.width - panelW()) / 2;
        int t = (this.height - panelH()) / 2;
        graphics.fill(l - 2, t - 2, l + panelW() + 2, t + panelH() + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + panelW() + 1, t + panelH() + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + panelW(), t + panelH(), PANEL);
        graphics.centeredText(this.font, this.title, l + panelW() / 2, t + 9, GOLD);

        List<Guide> guides = visible();
        for (int i = 0; i < guides.size(); i++) {
            Guide guide = guides.get(i);
            int x = tileX(i);
            int y = tileY(i);
            boolean hover = inside(mouseX, mouseY, x, y);
            graphics.fill(x, y, x + TILE_W, y + TILE_H, hover ? TRIM_LIGHT : TRIM_DARK);
            graphics.fill(x + 1, y + 1, x + TILE_W - 1, y + TILE_H - 1, hover ? TILE_HOVER : TILE);
            graphics.item(guide.icon().get(), x + 8, y + 10);
            graphics.text(this.font, guide.title(), x + 30, y + 8, TEXT, false);
            graphics.text(this.font, guide.description(), x + 30, y + 20, TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        List<Guide> guides = visible();
        for (int i = 0; i < guides.size(); i++) {
            if (inside((int) event.x(), (int) event.y(), tileX(i), tileY(i))) {
                this.minecraft.gui.setScreen(guides.get(i).open().apply(this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(int mx, int my, int x, int y) {
        return mx >= x && mx < x + TILE_W && my >= y && my < y + TILE_H;
    }
}
