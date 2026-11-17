package com.alexh.capitalism.client;

import com.alexh.capitalism.enchanter.Apprentice;
import com.alexh.capitalism.network.RenameApprenticePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// The naming box: the owner right-clicked their Apprentice with the Apprentice Wand (network/OpenApprenticeNamePacket).
// Type a name and press Enter or Done; an empty name goes back to "Iron Apprentice". The server checks the rest.
public class ApprenticeNameScreen extends Screen {

    private static final int W = 180;
    private static final int H = 74;
    private static final int PANEL = 0xFF231636;
    private static final int TRIM_LIGHT = 0xFFC9A4FF;
    private static final int TRIM_DARK = 0xFF5A3A8A;
    private static final int GOLD = 0xFFE8C14A;

    private final int entityId;
    private final String current;
    private EditBox name;

    public ApprenticeNameScreen(int entityId, String current) {
        super(Component.literal("Name Your Apprentice"));
        this.entityId = entityId;
        this.current = current;
    }

    public static void open(int entityId, String current) {
        Minecraft.getInstance().gui.setScreen(new ApprenticeNameScreen(entityId, current));
    }

    @Override
    protected void init() {
        int l = (this.width - W) / 2;
        int t = (this.height - H) / 2;
        String typed = this.name == null ? this.current : this.name.getValue();
        this.name = new EditBox(this.font, l + 12, t + 24, W - 24, 16, Component.literal("Name"));
        this.name.setMaxLength(Apprentice.MAX_NAME);
        this.name.setValue(typed);
        this.name.setHint(Component.literal("Apprentice"));
        this.addRenderableWidget(this.name);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.done())
            .bounds(l + W / 2 - 40, t + H - 26, 80, 18).build());
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.name);
    }

    private void done() {
        ClientPacketDistributor.sendToServer(new RenameApprenticePacket(this.entityId, this.name.getValue()));
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            this.done();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int l = (this.width - W) / 2;
        int t = (this.height - H) / 2;
        graphics.fill(l - 2, t - 2, l + W + 2, t + H + 2, TRIM_DARK);
        graphics.fill(l - 1, t - 1, l + W + 1, t + H + 1, TRIM_LIGHT);
        graphics.fill(l, t, l + W, t + H, PANEL);
        graphics.centeredText(this.font, this.title, l + W / 2, t + 9, GOLD);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
