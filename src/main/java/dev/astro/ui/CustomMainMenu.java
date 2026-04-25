package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.io.IOException;

/**
 * Custom main menu for AstroClient.
 * Clean, dark design with centred branding and minimal buttons.
 */
public final class CustomMainMenu extends GuiScreen {

    private static final int ACCENT      = 0xFF00C8FF;
    private static final int ACCENT_END  = 0xFF7B61FF;
    private static final int BTN_BG      = new Color(22, 22, 30, 230).getRGB();
    private static final int BTN_HOVER   = new Color(35, 35, 48, 240).getRGB();
    private static final int BTN_BORDER  = new Color(255, 255, 255, 25).getRGB();

    private static final int BTN_WIDTH  = 200;
    private static final int BTN_HEIGHT = 22;
    private static final int BTN_GAP    = 4;

    // Button IDs
    private static final int ID_SINGLE    = 1;
    private static final int ID_MULTI     = 2;
    private static final int ID_OPTIONS   = 3;
    private static final int ID_QUIT      = 4;

    @Override
    public void initGui() {
        int cx = this.width / 2;
        int startY = this.height / 2 + 10;

        this.buttonList.clear();
        this.buttonList.add(new FlatButton(ID_SINGLE,  cx - BTN_WIDTH/2, startY,                            BTN_WIDTH, BTN_HEIGHT, "Singleplayer"));
        this.buttonList.add(new FlatButton(ID_MULTI,   cx - BTN_WIDTH/2, startY + BTN_HEIGHT + BTN_GAP,     BTN_WIDTH, BTN_HEIGHT, "Multiplayer"));
        this.buttonList.add(new FlatButton(ID_OPTIONS,  cx - BTN_WIDTH/2, startY + 2*(BTN_HEIGHT + BTN_GAP), BTN_WIDTH, BTN_HEIGHT, "Options"));
        this.buttonList.add(new FlatButton(ID_QUIT,    cx - BTN_WIDTH/2, startY + 3*(BTN_HEIGHT + BTN_GAP), BTN_WIDTH, BTN_HEIGHT, "Quit Game"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Full dark background
        drawRect(0, 0, this.width, this.height, 0xFF0a0a12);

        // Subtle radial vignette effect (dark edges)
        drawGradientRect(0, 0, this.width, this.height / 3, 0x00000000, 0x00000000);
        drawGradientRect(0, this.height * 2 / 3, this.width, this.height, 0x00000000, 0x80000000);

        FontRenderer fr = mc.fontRendererObj;
        int cx = this.width / 2;

        // ── Client name ────────────────────────────────────────────────
        String name = AstroClient.NAME;
        // Draw large title (scale 3x)
        GlStateManager.pushMatrix();
        float scale = 3.0f;
        float titleX = cx / scale - fr.getStringWidth(name) / 2f;
        float titleY = (this.height / 2f - 55) / scale;
        GlStateManager.scale(scale, scale, 1);
        fr.drawStringWithShadow(name, titleX, titleY, 0xFFFFFFFF);
        GlStateManager.popMatrix();

        // Accent line under title
        int lineW = (int)(fr.getStringWidth(name) * scale) + 20;
        int lineX = cx - lineW / 2;
        int lineY = this.height / 2 - 18;
        RenderUtil.drawGradientH(lineX, lineY, lineX + lineW, lineY + 2, ACCENT, ACCENT_END);

        // Version text
        String ver = "v" + AstroClient.VERSION;
        fr.drawStringWithShadow(ver, cx - fr.getStringWidth(ver) / 2f, lineY + 6, 0xFF666677);

        // Draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Copyright footer
        String copy = "Minecraft 1.8.9  \u2022  Not affiliated with Mojang";
        fr.drawStringWithShadow(copy, cx - fr.getStringWidth(copy) / 2f,
                this.height - 12, 0xFF333344);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case ID_SINGLE:
                mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case ID_MULTI:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case ID_OPTIONS:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case ID_QUIT:
                mc.shutdown();
                break;
        }
    }

    // ── Custom flat button ─────────────────────────────────────────────

    private static class FlatButton extends GuiButton {

        public FlatButton(int id, int x, int y, int w, int h, String text) {
            super(id, x, y, w, h, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) return;

            FontRenderer fr = mc.fontRendererObj;
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                        && mouseX < this.xPosition + this.width
                        && mouseY < this.yPosition + this.height;

            int bg = this.hovered ? BTN_HOVER : BTN_BG;

            // Background
            RenderUtil.drawRoundedRect(this.xPosition, this.yPosition,
                    this.xPosition + this.width, this.yPosition + this.height, 3, bg);

            // Subtle border
            RenderUtil.drawOutline(this.xPosition, this.yPosition,
                    this.xPosition + this.width, this.yPosition + this.height, BTN_BORDER);

            // Left accent on hover
            if (this.hovered) {
                RenderUtil.drawRect(this.xPosition, this.yPosition + 2,
                        this.xPosition + 2, this.yPosition + this.height - 2, ACCENT);
            }

            // Text
            int textCol = this.hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
            fr.drawStringWithShadow(this.displayString,
                    this.xPosition + (this.width - fr.getStringWidth(this.displayString)) / 2f,
                    this.yPosition + (this.height - fr.FONT_HEIGHT) / 2f + 1,
                    textCol);
        }
    }
}
