package dev.astro.module;

import dev.astro.event.EventTarget;
import dev.astro.event.events.Render2DEvent;
import dev.astro.module.setting.NumberSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

/**
 * Base class for all HUD modules that render a draggable, scalable element on screen.
 * Subclasses only need to implement {@link #getText()} for simple text displays,
 * or override {@link #drawHUD(FontRenderer, int, int)} for complex rendering.
 */
public abstract class HUDModule extends Module {

    protected int renderX;
    protected int renderY;

    /** Scale multiplier — adjustable via ClickGUI slider or scroll in HUDEditor. */
    private final NumberSetting hudScale = addSetting(
            new NumberSetting("Element Scale", "Element size multiplier", 1.0, 0.5, 3.0, 0.1));

    private static final int BG   = new Color(0, 0, 0, 120).getRGB();
    private static final int EDGE = new Color(255, 255, 255, 30).getRGB();

    protected HUDModule(String name, String description, Category category, int keyBind,
                        int defaultX, int defaultY) {
        super(name, description, category, keyBind);
        this.renderX = defaultX;
        this.renderY = defaultY;
    }

    protected HUDModule(String name, String description, Category category,
                        int defaultX, int defaultY) {
        super(name, description, category);
        this.renderX = defaultX;
        this.renderY = defaultY;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr == null) return;

        float s = getScale();
        if (Math.abs(s - 1.0f) < 0.01f) {
            drawHUD(fr, renderX, renderY);
        } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(renderX, renderY, 0);
            GlStateManager.scale(s, s, 1.0f);
            GlStateManager.translate(-renderX, -renderY, 0);
            drawHUD(fr, renderX, renderY);
            GlStateManager.popMatrix();
        }
    }

    /**
     * Override for custom rendering. Default implementation draws a rounded
     * box with {@link #getText()} inside it. Public so HUDEditor can call it.
     */
    public void drawHUD(FontRenderer fr, int x, int y) {
        String text = getText();
        if (text == null || text.isEmpty()) return;

        int w = fr.getStringWidth(text) + 10;
        int h = fr.FONT_HEIGHT + 8;

        RenderUtil.drawRoundedRect(x, y, x + w, y + h, 3, BG);
        RenderUtil.drawOutline(x, y, x + w, y + h, EDGE);
        fr.drawStringWithShadow(text, x + 5, y + 4, getTextColour());
    }

    /** Simple modules just return the display text here. */
    protected abstract String getText();

    /** Override to change text colour. Default is white. */
    protected int getTextColour() { return 0xFFFFFFFF; }

    /** Unscaled width of this element. */
    public int getBaseWidth() {
        String text = getText();
        if (text == null) return 60;
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) + 10;
    }

    /** Unscaled height of this element. */
    public int getBaseHeight() {
        return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 8;
    }

    /** Scaled width for HUD editor hit-testing. */
    public int getWidth() {
        return (int) (getBaseWidth() * getScale());
    }

    /** Scaled height for HUD editor hit-testing. */
    public int getHeight() {
        return (int) (getBaseHeight() * getScale());
    }

    public float getScale()          { return hudScale.getFloatValue(); }
    public void  setScale(float s)   { hudScale.setValue((double) s); }

    public int  getRenderX()      { return renderX; }
    public int  getRenderY()      { return renderY; }
    public void setRenderX(int x) { this.renderX = x; }
    public void setRenderY(int y) { this.renderY = y; }
}
