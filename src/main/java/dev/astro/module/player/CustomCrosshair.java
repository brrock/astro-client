package dev.astro.module.player;

import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.module.setting.NumberSetting;
import dev.astro.ui.CrosshairPreview;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * CustomCrosshair — replaces the default crosshair with a configurable shape.
 * Supports Plus, Circle, Dot, and Cross types with full colour/outline control.
 */
public final class CustomCrosshair extends Module {

    private final ModeSetting type = addSetting(
            new ModeSetting("Type", "Crosshair shape", "Plus", "Plus", "Circle", "Dot", "Cross"));
    private final ColorSetting colour = addSetting(
            new ColorSetting("Color", "Crosshair color", 0xFFFFFFFF));
    private final NumberSetting gap = addSetting(
            new NumberSetting("Gap", "Gap from center", 3.0, 0.0, 10.0, 1.0));
    private final NumberSetting length = addSetting(
            new NumberSetting("Length", "Arm length", 6.0, 1.0, 20.0, 1.0));
    private final NumberSetting thickness = addSetting(
            new NumberSetting("Thickness", "Line thickness", 1.0, 1.0, 5.0, 1.0));
    private final BooleanSetting dot = addSetting(
            new BooleanSetting("Center Dot", "Draw a dot in the center", false));
    private final BooleanSetting outline = addSetting(
            new BooleanSetting("Outline", "Draw black outline around crosshair", true));
    private final NumberSetting outlineWidth = addSetting(
            new NumberSetting("Outline Width", "Outline thickness", 1.0, 1.0, 3.0, 1.0));
    private final NumberSetting opacity = addSetting(
            new NumberSetting("Opacity", "Crosshair opacity", 100.0, 10.0, 100.0, 5.0));

    public CustomCrosshair() {
        super("CustomCrosshair",
              "Replaces the default crosshair with a custom shape.",
              Category.PLAYER);
    }

    // ── Accessors for CrosshairPreview ──────────────────────────────────

    public ModeSetting    getTypeSetting()         { return type; }
    public ColorSetting   getColourSetting()       { return colour; }
    public NumberSetting  getGapSetting()          { return gap; }
    public NumberSetting  getLengthSetting()        { return length; }
    public NumberSetting  getThicknessSetting()     { return thickness; }
    public BooleanSetting getDotSetting()          { return dot; }
    public BooleanSetting getOutlineSetting()      { return outline; }
    public NumberSetting  getOutlineWidthSetting() { return outlineWidth; }
    public NumberSetting  getOpacitySetting()      { return opacity; }

    // ── Forge event bus lifecycle ───────────────────────────────────────

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    // ── Overlay event ──────────────────────────────────────────────────

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        event.setCanceled(true);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float cx = sr.getScaledWidth() / 2.0F;
        float cy = sr.getScaledHeight() / 2.0F;

        renderCrosshair(cx, cy);
    }

    /**
     * Renders the crosshair centred at (cx, cy).
     * Public so CrosshairPreview can call it for the live preview.
     */
    public void renderCrosshair(float cx, float cy) {
        int col = applyOpacity(colour.getValue());
        int outCol = applyOpacity(0xFF000000);
        float g = gap.getFloatValue();
        float l = length.getFloatValue();
        float t = thickness.getFloatValue();
        float ow = outlineWidth.getFloatValue();
        boolean drawOutline = outline.getValue();

        String mode = type.getValue();
        if ("Plus".equals(mode)) {
            if (drawOutline) drawPlus(cx, cy, g, l, t, ow, outCol);
            drawPlus(cx, cy, g, l, t, 0, col);
        } else if ("Circle".equals(mode)) {
            if (drawOutline) drawCircle(cx, cy, l + ow, t + ow, outCol);
            drawCircle(cx, cy, l, t, col);
        } else if ("Dot".equals(mode)) {
            float dotSize = t + 1;
            if (drawOutline) {
                drawDot(cx, cy, dotSize + ow, outCol);
            }
            drawDot(cx, cy, dotSize, col);
        } else if ("Cross".equals(mode)) {
            if (drawOutline) drawCross(cx, cy, g, l, t, ow, outCol);
            drawCross(cx, cy, g, l, t, 0, col);
        }

        if (dot.getValue()) {
            float ds = Math.max(1, t);
            float half = ds / 2.0F;
            if (drawOutline) {
                RenderUtil.drawRect(cx - half - ow, cy - half - ow,
                                    cx + half + ow, cy + half + ow, outCol);
            }
            RenderUtil.drawRect(cx - half, cy - half, cx + half, cy + half, col);
        }
    }

    // ── Shape helpers ──────────────────────────────────────────────────

    private void drawPlus(float cx, float cy, float g, float l,
                          float t, float expand, int col) {
        float half = t / 2.0F + expand;
        float gInner = Math.max(0, g - expand);
        float lOuter = l + expand;
        // Horizontal arms
        RenderUtil.drawRect(cx - lOuter - gInner, cy - half,
                            cx - gInner,           cy + half, col);
        RenderUtil.drawRect(cx + gInner,           cy - half,
                            cx + gInner + lOuter,  cy + half, col);
        // Vertical arms
        RenderUtil.drawRect(cx - half, cy - lOuter - gInner,
                            cx + half, cy - gInner,           col);
        RenderUtil.drawRect(cx - half, cy + gInner,
                            cx + half, cy + gInner + lOuter,  col);
    }

    private void drawCircle(float cx, float cy, float radius, float lineW, int col) {
        float a = ((col >> 24) & 0xFF) / 255.0F;
        float r = ((col >> 16) & 0xFF) / 255.0F;
        float g = ((col >>  8) & 0xFF) / 255.0F;
        float b = ( col        & 0xFF) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineW);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            GL11.glVertex2d(cx + Math.cos(angle) * radius,
                            cy + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    private void drawDot(float cx, float cy, float size, int col) {
        float half = size / 2.0F;
        RenderUtil.drawRect(cx - half, cy - half, cx + half, cy + half, col);
    }

    private void drawCross(float cx, float cy, float g, float l,
                           float t, float expand, int col) {
        float half = t / 2.0F + expand;
        float gInner = Math.max(0, g - expand);
        float lOuter = l + expand;

        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GL11.glRotatef(45, 0, 0, 1);

        // Draw as a plus rotated 45°
        RenderUtil.drawRect(-lOuter - gInner, -half,
                            -gInner,           half, col);
        RenderUtil.drawRect( gInner,          -half,
                             gInner + lOuter,  half, col);
        RenderUtil.drawRect(-half, -lOuter - gInner,
                             half, -gInner,           col);
        RenderUtil.drawRect(-half,  gInner,
                             half,  gInner + lOuter,  col);

        GlStateManager.popMatrix();
    }

    // ── Utility ────────────────────────────────────────────────────────

    private int applyOpacity(int col) {
        int origAlpha = (col >> 24) & 0xFF;
        int scaled = (int) (origAlpha * (opacity.getValue() / 100.0));
        return (scaled << 24) | (col & 0x00FFFFFF);
    }

    /** Opens the crosshair preview/settings GUI. */
    public static void openPreview(CustomCrosshair module) {
        Minecraft.getMinecraft().displayGuiScreen(new CrosshairPreview(module));
    }
}
