package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.module.player.CustomCrosshair;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.module.setting.NumberSetting;
import dev.astro.module.setting.Setting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * CrosshairPreview — a GuiScreen that shows a live preview of the
 * crosshair alongside editable setting controls.
 */
public final class CrosshairPreview extends GuiScreen {

    // ── Layout ─────────────────────────────────────────────────────────
    private static final int GUI_W       = 380;
    private static final int GUI_H       = 280;
    private static final int HEADER_H    = 26;
    private static final int PREVIEW_PAD = 12;
    private static final int CTRL_PAD    = 6;
    private static final int ROW_H       = 18;
    private static final int SLIDER_H    = 8;
    private static final int TOGGLE_W    = 24;
    private static final int TOGGLE_H    = 12;
    private static final int BTN_W       = 80;
    private static final int BTN_H       = 20;
    private static final int SWATCH_SIZE = 12;

    // ── Colours ────────────────────────────────────────────────────────
    private static final int BG_MAIN     = 0xF0101018;
    private static final int BG_HEADER   = 0xF0080810;
    private static final int ACCENT      = 0xFF00C8FF;
    private static final int ACCENT_END  = 0xFF7B61FF;
    private static final int TEXT_DIM    = 0xFF888899;
    private static final int TEXT_BRIGHT = 0xFFE0E0F0;
    private static final int PREVIEW_BG  = 0x80000000;
    private static final int TOGGLE_ON   = 0xFF00C8FF;
    private static final int TOGGLE_OFF  = 0xFF333344;
    private static final int TOGGLE_KNOB = 0xFFFFFFFF;
    private static final int SLIDER_TRACK = 0xFF222233;
    private static final int SLIDER_KNOB  = 0xFFFFFFFF;
    private static final int BTN_BG       = 0xFF1A1A2A;
    private static final int BTN_HOVER    = 0xFF252540;

    // ── Preset colour swatches ─────────────────────────────────────────
    private static final int[] SWATCH_COLOURS = {
        0xFFFFFFFF, // white
        0xFFFF4444, // red
        0xFF44FF44, // green
        0xFF44FFFF, // cyan
        0xFFFFFF44, // yellow
        0xFFFF44FF, // magenta
        0xFFFF8800  // orange
    };

    private final CustomCrosshair module;

    /** Which NumberSetting slider is being dragged, or null. */
    private NumberSetting draggingSetting;

    public CrosshairPreview(CustomCrosshair module) {
        this.module = module;
    }

    // ── Rendering ──────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int left = (sr.getScaledWidth()  - GUI_W) / 2;
        int top  = (sr.getScaledHeight() - GUI_H) / 2;

        FontRenderer fr = mc.fontRendererObj;

        drawPanel(fr, left, top, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Handle slider drag
        if (draggingSetting != null && Mouse.isButtonDown(0)) {
            updateSliderDrag(mouseX, left, top, fr);
        } else {
            draggingSetting = null;
        }
    }

    private void drawPanel(FontRenderer fr, int left, int top,
                           int mouseX, int mouseY) {
        int right  = left + GUI_W;
        int bottom = top  + GUI_H;

        // ── Background ─────────────────────────────────────────────────
        RenderUtil.drawRoundedRect(left, top, right, bottom, 3, BG_MAIN);

        // ── Header ─────────────────────────────────────────────────────
        RenderUtil.drawRect(left, top, right, top + HEADER_H, BG_HEADER);
        RenderUtil.drawGradientH(left, top + HEADER_H - 1,
                                 right, top + HEADER_H, ACCENT, ACCENT_END);
        fr.drawStringWithShadow("Crosshair Preview",
                left + 10, top + (HEADER_H - fr.FONT_HEIGHT) / 2f + 1,
                0xFFFFFFFF);

        // ── Split: preview (left) | controls (right) ───────────────────
        int splitX   = left + GUI_W / 2 - 10;
        int innerTop = top + HEADER_H + PREVIEW_PAD;

        // Divider
        RenderUtil.drawRect(splitX, innerTop, splitX + 1,
                            bottom - BTN_H - 16, 0x20FFFFFF);

        // ── Live preview box ───────────────────────────────────────────
        int pvLeft   = left + PREVIEW_PAD;
        int pvRight  = splitX - PREVIEW_PAD;
        int pvTop    = innerTop;
        int pvBottom = bottom - BTN_H - 16;
        RenderUtil.drawRoundedRect(pvLeft, pvTop, pvRight, pvBottom, 3, PREVIEW_BG);

        float pvCx = (pvLeft + pvRight)  / 2.0F;
        float pvCy = (pvTop  + pvBottom) / 2.0F;

        // Faint grid lines for visual reference
        RenderUtil.drawRect(pvLeft, pvCy, pvRight, pvCy + 1, 0x15FFFFFF);
        RenderUtil.drawRect(pvCx, pvTop, pvCx + 1, pvBottom, 0x15FFFFFF);

        module.renderCrosshair(pvCx, pvCy);

        // ── Controls (right side) ──────────────────────────────────────
        int ctrlLeft  = splitX + PREVIEW_PAD;
        int ctrlRight = right - PREVIEW_PAD;
        int ctrlW     = ctrlRight - ctrlLeft;
        int cy        = innerTop + 2;

        List<Setting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            if (s instanceof ModeSetting) {
                cy = drawModeRow(fr, (ModeSetting) s, ctrlLeft, cy, ctrlW, mouseX, mouseY);
            } else if (s instanceof ColorSetting) {
                cy = drawColorRow(fr, (ColorSetting) s, ctrlLeft, cy, ctrlW, mouseX, mouseY);
            } else if (s instanceof NumberSetting) {
                cy = drawSliderRow(fr, (NumberSetting) s, ctrlLeft, cy, ctrlW, mouseX, mouseY);
            } else if (s instanceof BooleanSetting) {
                cy = drawToggleRow(fr, (BooleanSetting) s, ctrlLeft, cy, ctrlW, mouseX, mouseY);
            }
        }

        // ── Done button ────────────────────────────────────────────────
        int btnX = left + (GUI_W - BTN_W) / 2;
        int btnY = bottom - BTN_H - 8;
        boolean btnHover = mouseX >= btnX && mouseX < btnX + BTN_W
                        && mouseY >= btnY && mouseY < btnY + BTN_H;
        RenderUtil.drawRoundedRect(btnX, btnY, btnX + BTN_W, btnY + BTN_H,
                                   2, btnHover ? BTN_HOVER : BTN_BG);
        RenderUtil.drawOutline(btnX, btnY, btnX + BTN_W, btnY + BTN_H,
                               ACCENT);
        String doneTxt = "Done";
        fr.drawStringWithShadow(doneTxt,
                btnX + (BTN_W - fr.getStringWidth(doneTxt)) / 2f,
                btnY + (BTN_H - fr.FONT_HEIGHT) / 2f,
                TEXT_BRIGHT);
    }

    // ── Control row renderers ──────────────────────────────────────────

    private int drawModeRow(FontRenderer fr, ModeSetting s,
                            int x, int y, int w, int mx, int my) {
        fr.drawStringWithShadow(s.getName() + ":", x, y + 4, TEXT_DIM);
        String val = s.getValue();
        int valW = fr.getStringWidth(val + " \u25B8");
        int valX = x + w - valW;
        boolean hovered = mx >= valX && mx < x + w
                       && my >= y && my < y + ROW_H;
        fr.drawStringWithShadow(val + " \u25B8", valX, y + 4,
                                hovered ? 0xFFFFFFFF : ACCENT);
        return y + ROW_H + CTRL_PAD;
    }

    private int drawColorRow(FontRenderer fr, ColorSetting s,
                             int x, int y, int w, int mx, int my) {
        fr.drawStringWithShadow(s.getName() + ":", x, y + 4, TEXT_DIM);

        // Current colour swatch
        int swX = x + fr.getStringWidth(s.getName() + ": ");
        RenderUtil.drawRect(swX, y + 2, swX + SWATCH_SIZE, y + 2 + SWATCH_SIZE,
                            s.getValue());
        RenderUtil.drawOutline(swX, y + 2, swX + SWATCH_SIZE, y + 2 + SWATCH_SIZE,
                               0xFFAAAAAA);

        // Preset swatches
        int swatchX = swX + SWATCH_SIZE + 6;
        for (int i = 0; i < SWATCH_COLOURS.length; i++) {
            int sx = swatchX + i * (SWATCH_SIZE + 3);
            boolean hovered = mx >= sx && mx < sx + SWATCH_SIZE
                           && my >= y + 2 && my < y + 2 + SWATCH_SIZE;
            RenderUtil.drawRect(sx, y + 2, sx + SWATCH_SIZE, y + 2 + SWATCH_SIZE,
                                SWATCH_COLOURS[i]);
            if (hovered || SWATCH_COLOURS[i] == s.getValue()) {
                RenderUtil.drawOutline(sx, y + 2,
                                       sx + SWATCH_SIZE, y + 2 + SWATCH_SIZE,
                                       0xFFFFFFFF);
            }
        }
        return y + ROW_H + CTRL_PAD;
    }

    private int drawSliderRow(FontRenderer fr, NumberSetting s,
                              int x, int y, int w, int mx, int my) {
        String label = s.getName();
        String valStr;
        if (s.getStep() >= 1.0) {
            valStr = String.valueOf(s.getIntValue());
        } else {
            valStr = String.format("%.1f", s.getValue());
        }
        fr.drawStringWithShadow(label, x, y + 1, TEXT_DIM);

        int valTextW = fr.getStringWidth(valStr);
        fr.drawStringWithShadow(valStr, x + w - valTextW, y + 1, TEXT_BRIGHT);

        // Slider track
        int sliderX = x;
        int sliderW = w - valTextW - 6;
        int sliderY = y + fr.FONT_HEIGHT + 2;
        float pct = s.getPercent();

        RenderUtil.drawRoundedRect(sliderX, sliderY,
                                   sliderX + sliderW, sliderY + SLIDER_H,
                                   2, SLIDER_TRACK);
        // Fill
        int fillW = (int) (sliderW * pct);
        if (fillW > 0) {
            RenderUtil.drawRoundedRect(sliderX, sliderY,
                                       sliderX + fillW, sliderY + SLIDER_H,
                                       2, ACCENT);
        }
        // Knob
        int knobX = sliderX + fillW - 3;
        int knobSize = SLIDER_H + 2;
        RenderUtil.drawRoundedRect(knobX, sliderY - 1,
                                   knobX + 6, sliderY + SLIDER_H + 1,
                                   2, SLIDER_KNOB);

        return y + fr.FONT_HEIGHT + SLIDER_H + CTRL_PAD + 4;
    }

    private int drawToggleRow(FontRenderer fr, BooleanSetting s,
                              int x, int y, int w, int mx, int my) {
        fr.drawStringWithShadow(s.getName(), x, y + 4, TEXT_DIM);

        int tX = x + w - TOGGLE_W;
        int tY = y + (ROW_H - TOGGLE_H) / 2;
        float anim = s.getValue() ? 1.0F : 0.0F;

        int trackCol = lerpColour(TOGGLE_OFF, TOGGLE_ON, anim);
        RenderUtil.drawRoundedRect(tX, tY, tX + TOGGLE_W, tY + TOGGLE_H,
                                   2, trackCol);
        float knobX = tX + 2 + (TOGGLE_W - TOGGLE_H) * anim;
        RenderUtil.drawRoundedRect(knobX, tY + 2,
                                   knobX + TOGGLE_H - 4, tY + TOGGLE_H - 2,
                                   1, TOGGLE_KNOB);

        // ON/OFF label
        String state = s.getValue() ? "ON" : "OFF";
        fr.drawStringWithShadow(state, tX - fr.getStringWidth(state) - 4,
                                y + 4, s.getValue() ? ACCENT : TEXT_DIM);

        return y + ROW_H + CTRL_PAD;
    }

    // ── Input ──────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, btn);
        if (btn != 0) return;

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int left = (sr.getScaledWidth()  - GUI_W) / 2;
        int top  = (sr.getScaledHeight() - GUI_H) / 2;
        int right  = left + GUI_W;
        int bottom = top  + GUI_H;

        FontRenderer fr = mc.fontRendererObj;

        // ── Done button ────────────────────────────────────────────────
        int btnX = left + (GUI_W - BTN_W) / 2;
        int btnY = bottom - BTN_H - 8;
        if (mouseX >= btnX && mouseX < btnX + BTN_W
         && mouseY >= btnY && mouseY < btnY + BTN_H) {
            AstroClient.INSTANCE.getConfigManager().save();
            mc.displayGuiScreen(null);
            return;
        }

        // ── Controls area ──────────────────────────────────────────────
        int splitX   = left + GUI_W / 2 - 10;
        int innerTop = top + HEADER_H + PREVIEW_PAD;
        int ctrlLeft  = splitX + PREVIEW_PAD;
        int ctrlRight = right - PREVIEW_PAD;
        int ctrlW     = ctrlRight - ctrlLeft;
        int cy        = innerTop + 2;

        List<Setting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            if (s instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) s;
                String val = ms.getValue();
                int valW = fr.getStringWidth(val + " \u25B8");
                int valX = ctrlLeft + ctrlW - valW;
                if (mouseX >= valX && mouseX < ctrlLeft + ctrlW
                 && mouseY >= cy && mouseY < cy + ROW_H) {
                    ms.cycle();
                    return;
                }
                cy += ROW_H + CTRL_PAD;

            } else if (s instanceof ColorSetting) {
                ColorSetting cs = (ColorSetting) s;
                int swX = ctrlLeft + fr.getStringWidth(cs.getName() + ": ")
                        + SWATCH_SIZE + 6;
                for (int j = 0; j < SWATCH_COLOURS.length; j++) {
                    int sx = swX + j * (SWATCH_SIZE + 3);
                    if (mouseX >= sx && mouseX < sx + SWATCH_SIZE
                     && mouseY >= cy + 2 && mouseY < cy + 2 + SWATCH_SIZE) {
                        cs.setValue(SWATCH_COLOURS[j]);
                        return;
                    }
                }
                cy += ROW_H + CTRL_PAD;

            } else if (s instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) s;
                String valStr;
                if (ns.getStep() >= 1.0) {
                    valStr = String.valueOf(ns.getIntValue());
                } else {
                    valStr = String.format("%.1f", ns.getValue());
                }
                int valTextW = fr.getStringWidth(valStr);
                int sliderX = ctrlLeft;
                int sliderW = ctrlW - valTextW - 6;
                int sliderY = cy + fr.FONT_HEIGHT + 2;

                if (mouseX >= sliderX && mouseX < sliderX + sliderW
                 && mouseY >= sliderY - 2 && mouseY < sliderY + SLIDER_H + 2) {
                    draggingSetting = ns;
                    float pct = (float) (mouseX - sliderX) / sliderW;
                    ns.setPercent(pct);
                    return;
                }
                cy += fr.FONT_HEIGHT + SLIDER_H + CTRL_PAD + 4;

            } else if (s instanceof BooleanSetting) {
                BooleanSetting bs = (BooleanSetting) s;
                int tX = ctrlLeft + ctrlW - TOGGLE_W;
                int tY = cy + (ROW_H - TOGGLE_H) / 2;
                if (mouseX >= tX && mouseX < tX + TOGGLE_W
                 && mouseY >= tY && mouseY < tY + TOGGLE_H) {
                    bs.toggle();
                    return;
                }
                cy += ROW_H + CTRL_PAD;
            }
        }
    }

    /**
     * Updates the slider value while dragging.
     */
    private void updateSliderDrag(int mouseX, int left, int top,
                                  FontRenderer fr) {
        if (draggingSetting == null) return;

        int splitX   = left + GUI_W / 2 - 10;
        int innerTop = top + HEADER_H + PREVIEW_PAD;
        int ctrlLeft  = splitX + PREVIEW_PAD;
        int ctrlRight = left + GUI_W - PREVIEW_PAD;
        int ctrlW     = ctrlRight - ctrlLeft;

        // Walk through settings to find the correct slider X/W
        List<Setting<?>> settings = module.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            if (s == draggingSetting) {
                NumberSetting ns = (NumberSetting) s;
                String valStr;
                if (ns.getStep() >= 1.0) {
                    valStr = String.valueOf(ns.getIntValue());
                } else {
                    valStr = String.format("%.1f", ns.getValue());
                }
                int valTextW = fr.getStringWidth(valStr);
                int sliderX = ctrlLeft;
                int sliderW = ctrlW - valTextW - 6;
                float pct = (float) (mouseX - sliderX) / sliderW;
                ns.setPercent(pct);
                return;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ── Colour math ────────────────────────────────────────────────────

    private static int lerpColour(int c1, int c2, float t) {
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >>  8) & 0xFF) + (((c2 >>  8) & 0xFF) - ((c1 >>  8) & 0xFF)) * t);
        int b = (int) (( c1        & 0xFF) + (( c2        & 0xFF) - ( c1        & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
