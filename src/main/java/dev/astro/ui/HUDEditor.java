package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.module.HUDModule;
import dev.astro.module.Module;
import dev.astro.module.player.ScoreboardMod;
import dev.astro.module.setting.NumberSetting;
import dev.astro.util.ConfigManager;
import dev.astro.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lunar Client-style "Arrange HUD" editor.
 * Transparent overlay over the game world with drag-and-drop positioning,
 * snap guides, tooltips, and a bottom toolbar.
 */
public final class HUDEditor extends GuiScreen {

    // ── Lunar-inspired palette ─────────────────────────────────────────
    private static final int OVERLAY_BG       = 0x60000000;
    private static final int GUIDE_LINE       = 0x30FFFFFF;
    private static final int GUIDE_ACTIVE     = 0x80FFFFFF;
    private static final int GRID_DOT         = 0x18FFFFFF;
    private static final int OUTLINE_NORMAL   = 0xCC5B8DEF;
    private static final int OUTLINE_DRAG     = 0xCC4ADE80;
    private static final int FILL_HOVER       = 0x205B8DEF;
    private static final int FILL_DRAG        = 0x204ADE80;
    private static final int TOOLTIP_BG       = 0xE0101018;
    private static final int TOOLTIP_TEXT     = 0xFFE8E8F0;
    private static final int BAR_BG           = 0xD0101018;
    private static final int BAR_ACCENT       = 0xFF5B8DEF;
    private static final int DONE_BTN_BG      = 0xFF5B8DEF;
    private static final int DONE_BTN_HOVER   = 0xFF7BA5FF;
    private static final int DONE_BTN_TEXT    = 0xFFFFFFFF;
    private static final int BAR_DIM_TEXT     = 0xFF666680;

    private static final int SNAP_THRESHOLD = 4;
    private static final int BAR_HEIGHT     = 30;
    private static final int GRID_SPACING   = 20;
    private static final int DONE_BTN_W     = 52;
    private static final int DONE_BTN_H     = 18;
    private static final int DONE_BTN_PAD   = 8;

    // ── State ──────────────────────────────────────────────────────────
    private final List<HUDDragElement> elements = new ArrayList<HUDDragElement>();
    private HUDDragElement dragging;
    private HUDDragElement hovered;
    private int dragOffX, dragOffY;
    private boolean snappedCenterX;
    private boolean snappedCenterY;

    /** Scoreboard anchor - resizable but not draggable. */
    private HUDAnchoredElement scoreboardAnchor;
    private boolean scoreboardHovered;

    // ── Init ───────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        elements.clear();
        scoreboardAnchor = null;

        for (Module mod : AstroClient.INSTANCE.getModuleManager().getModules()) {
            if (mod instanceof HUDModule && mod.isEnabled()) {
                elements.add(new HUDDragElement((HUDModule) mod));
            }
            // Add scoreboard as a special anchored element
            if (mod instanceof ScoreboardMod && mod.isEnabled()) {
                ScoreboardMod sbMod = (ScoreboardMod) mod;
                scoreboardAnchor = new HUDAnchoredElement("Scoreboard", sbMod.getScaleSetting());
            }
        }
    }

    // ── Rendering ──────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 1. Subtle dark overlay — game world visible behind
        RenderUtil.drawRect(0, 0, sw, sh, OVERLAY_BG);

        // 2. Grid dots
        for (int gx = GRID_SPACING; gx < sw; gx += GRID_SPACING) {
            for (int gy = GRID_SPACING; gy < sh; gy += GRID_SPACING) {
                RenderUtil.drawRect(gx, gy, gx + 1, gy + 1, GRID_DOT);
            }
        }

        // 3. Centre crosshair guide lines
        int centerGuideX = snappedCenterX ? GUIDE_ACTIVE : GUIDE_LINE;
        int centerGuideY = snappedCenterY ? GUIDE_ACTIVE : GUIDE_LINE;
        RenderUtil.drawRect(sw / 2f, 0, sw / 2f + 1, sh, centerGuideX);
        RenderUtil.drawRect(0, sh / 2f, sw, sh / 2f + 1, centerGuideY);

        // 4. Resolve hovered element (topmost under cursor)
        hovered = null;
        if (dragging == null) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                if (elements.get(i).contains(mouseX, mouseY)) {
                    hovered = elements.get(i);
                    break;
                }
            }
        }

        // 5. Draw each HUD element
        for (HUDDragElement el : elements) {
            // Sync size in case module content changed
            el.w = Math.max(el.hudModule.getWidth(), 40);
            el.h = Math.max(el.hudModule.getHeight(), 14);

            boolean isDragging = (el == dragging);
            boolean isHovered  = (el == hovered);

            // Render the module's actual HUD at its position (with scale)
            float s = el.hudModule.getScale();
            if (Math.abs(s - 1.0f) < 0.01f) {
                el.hudModule.drawHUD(fr, el.x, el.y);
            } else {
                GlStateManager.pushMatrix();
                GlStateManager.translate(el.x, el.y, 0);
                GlStateManager.scale(s, s, 1.0f);
                GlStateManager.translate(-el.x, -el.y, 0);
                el.hudModule.drawHUD(fr, el.x, el.y);
                GlStateManager.popMatrix();
            }

            // Selection fill
            if (isDragging) {
                RenderUtil.drawRect(el.x - 2, el.y - 2, el.x + el.w + 2, el.y + el.h + 2, FILL_DRAG);
            } else if (isHovered) {
                RenderUtil.drawRect(el.x - 2, el.y - 2, el.x + el.w + 2, el.y + el.h + 2, FILL_HOVER);
            }

            // Outline
            if (isDragging || isHovered) {
                int outCol = isDragging ? OUTLINE_DRAG : OUTLINE_NORMAL;
                drawDashedOutline(el.x - 2, el.y - 2, el.x + el.w + 2, el.y + el.h + 2, outCol);
            }

            // Tooltip label above hovered/dragging element
            if (isDragging || isHovered) {
                drawTooltip(fr, el);
            }
        }

        // 5b. Scoreboard anchored element
        scoreboardHovered = false;
        if (scoreboardAnchor != null) {
            computeScoreboardBounds(scoreboardAnchor, sr, fr);
            if (scoreboardAnchor.w > 0 && scoreboardAnchor.h > 0) {
                // Check hover (only if not dragging a HUD element)
                if (dragging == null && hovered == null
                        && scoreboardAnchor.contains(mouseX, mouseY)) {
                    scoreboardHovered = true;
                }

                int ax = scoreboardAnchor.x;
                int ay = scoreboardAnchor.y;
                int aw = scoreboardAnchor.w;
                int ah = scoreboardAnchor.h;

                if (scoreboardHovered) {
                    RenderUtil.drawRect(ax - 2, ay - 2, ax + aw + 2, ay + ah + 2, FILL_HOVER);
                    drawDashedOutline(ax - 2, ay - 2, ax + aw + 2, ay + ah + 2, OUTLINE_NORMAL);
                    // Tooltip
                    float sbScale = scoreboardAnchor.scaleSetting.getFloatValue();
                    String label = scoreboardAnchor.name + " (" + (int)(sbScale * 100) + "%)";
                    int tw = fr.getStringWidth(label) + 8;
                    int th = fr.FONT_HEIGHT + 4;
                    int tx = ax + (aw - tw) / 2;
                    int ty = ay - th - 4;
                    if (ty < 2) ty = ay + ah + 4;
                    RenderUtil.drawRoundedRect(tx, ty, tx + tw, ty + th, 2, TOOLTIP_BG);
                    fr.drawStringWithShadow(label, tx + 4, ty + 2, TOOLTIP_TEXT);
                } else {
                    // Always show a subtle outline so user knows it's resizable
                    drawDashedOutline(ax - 2, ay - 2, ax + aw + 2, ay + ah + 2, 0x40FFFFFF);
                }
            }
        }

        // 6. Floating Done button (bottom-right corner)
        drawBottomBar(fr, sw, sh, 0, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Draws a dashed outline: alternating 4px dash / 3px gap segments. */
    private void drawDashedOutline(int x1, int y1, int x2, int y2, int colour) {
        int dash = 4;
        int gap = 3;
        int step = dash + gap;

        // Top edge
        for (int x = x1; x < x2; x += step) {
            int end = Math.min(x + dash, x2);
            RenderUtil.drawRect(x, y1, end, y1 + 1, colour);
        }
        // Bottom edge
        for (int x = x1; x < x2; x += step) {
            int end = Math.min(x + dash, x2);
            RenderUtil.drawRect(x, y2 - 1, end, y2, colour);
        }
        // Left edge
        for (int y = y1; y < y2; y += step) {
            int end = Math.min(y + dash, y2);
            RenderUtil.drawRect(x1, y, x1 + 1, end, colour);
        }
        // Right edge
        for (int y = y1; y < y2; y += step) {
            int end = Math.min(y + dash, y2);
            RenderUtil.drawRect(x2 - 1, y, x2, end, colour);
        }
    }

    /** Floating name label above an element (shows scale if not 1x). */
    private void drawTooltip(FontRenderer fr, HUDDragElement el) {
        String label = el.name;
        float s = el.hudModule.getScale();
        if (Math.abs(s - 1.0f) > 0.05f) {
            label = label + " (" + (int)(s * 100) + "%)";
        }
        int tw = fr.getStringWidth(label) + 8;
        int th = fr.FONT_HEIGHT + 4;
        int tx = el.x + (el.w - tw) / 2;
        int ty = el.y - th - 4;
        if (ty < 2) ty = el.y + el.h + 4;

        RenderUtil.drawRoundedRect(tx, ty, tx + tw, ty + th, 2, TOOLTIP_BG);
        fr.drawStringWithShadow(label, tx + 4, ty + 2, TOOLTIP_TEXT);
    }

    /** Bottom bar: small floating Done button in bottom-right corner. */
    private void drawBottomBar(FontRenderer fr, int sw, int sh, int barTop,
                               int mouseX, int mouseY) {
        // Minimal floating pill — no full-width bar
        int btnX = sw - DONE_BTN_W - DONE_BTN_PAD;
        int btnY = sh - DONE_BTN_H - DONE_BTN_PAD;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + DONE_BTN_W
                && mouseY >= btnY && mouseY <= btnY + DONE_BTN_H;

        RenderUtil.drawRoundedRect(btnX, btnY, btnX + DONE_BTN_W, btnY + DONE_BTN_H,
                3, btnHover ? DONE_BTN_HOVER : DONE_BTN_BG);

        String doneLabel = "Done";
        fr.drawStringWithShadow(doneLabel,
                btnX + (DONE_BTN_W - fr.getStringWidth(doneLabel)) / 2f,
                btnY + (DONE_BTN_H - fr.FONT_HEIGHT) / 2f,
                DONE_BTN_TEXT);

        // Subtle instruction text above button
        String instr = "Drag to move \u2022 Scroll to resize \u2022 ESC to close";
        int instrW = fr.getStringWidth(instr);
        fr.drawStringWithShadow(instr, sw - instrW - DONE_BTN_PAD,
                btnY - fr.FONT_HEIGHT - 3, BAR_DIM_TEXT);
    }

    // ── Input handling ─────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        super.mouseClicked(mouseX, mouseY, btn);
        if (btn != 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // Done button hit test (floating bottom-right)
        int btnX = sw - DONE_BTN_W - DONE_BTN_PAD;
        int btnY = sh - DONE_BTN_H - DONE_BTN_PAD;
        if (mouseX >= btnX && mouseX <= btnX + DONE_BTN_W
                && mouseY >= btnY && mouseY <= btnY + DONE_BTN_H) {
            mc.displayGuiScreen(null);
            return;
        }

        // Element pick (reverse order = topmost first)
        for (int i = elements.size() - 1; i >= 0; i--) {
            HUDDragElement el = elements.get(i);
            if (el.contains(mouseX, mouseY)) {
                dragging = el;
                dragOffX = mouseX - el.x;
                dragOffY = mouseY - el.y;
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int btn, long timeSinceClick) {
        if (dragging == null || btn != 0) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int newX = mouseX - dragOffX;
        int newY = mouseY - dragOffY;

        // Clamp within full screen bounds
        newX = Math.max(0, Math.min(newX, sw - dragging.w));
        newY = Math.max(0, Math.min(newY, sh - dragging.h));

        // Snap to centre ±4px
        int cx = (sw - dragging.w) / 2;
        int cy = (sh - dragging.h) / 2;
        snappedCenterX = Math.abs(newX - cx) < SNAP_THRESHOLD;
        snappedCenterY = Math.abs(newY - cy) < SNAP_THRESHOLD;
        if (snappedCenterX) newX = cx;
        if (snappedCenterY) newY = cy;

        // Snap to edges ±4px
        if (newX < SNAP_THRESHOLD) newX = 0;
        if (newY < SNAP_THRESHOLD) newY = 0;
        if (sw - dragging.w - newX < SNAP_THRESHOLD) newX = sw - dragging.w;
        if (sh - dragging.h - newY < SNAP_THRESHOLD) newY = sh - dragging.h;

        dragging.x = newX;
        dragging.y = newY;
        applyPosition(dragging);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int btn) {
        if (btn == 0 && dragging != null) {
            applyPosition(dragging);
            ConfigManager cfg = AstroClient.INSTANCE.getConfigManager();
            if (cfg != null) cfg.save();
            dragging = null;
            snappedCenterX = false;
            snappedCenterY = false;
        }
        super.mouseReleased(mouseX, mouseY, btn);
    }

    private void applyPosition(HUDDragElement el) {
        el.hudModule.setRenderX(el.x);
        el.hudModule.setRenderY(el.y);
    }

    /**
     * Computes the approximate screen bounds of the scoreboard
     * based on current scores and scale, matching ScoreboardMod's renderer.
     */
    @SuppressWarnings("unchecked")
    private void computeScoreboardBounds(HUDAnchoredElement anchor, ScaledResolution sr, FontRenderer fr) {
        if (mc.theWorld == null) {
            anchor.w = 0;
            anchor.h = 0;
            return;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective obj = scoreboard.getObjectiveInDisplaySlot(1);
        if (obj == null) {
            // No active scoreboard — show placeholder
            float s = anchor.scaleSetting.getFloatValue();
            int pw = (int)(120 * s);
            int ph = (int)(80 * s);
            anchor.x = sr.getScaledWidth() - pw - 5;
            anchor.y = (sr.getScaledHeight() - ph) / 2;
            anchor.w = pw;
            anchor.h = ph;
            return;
        }

        Collection<Score> allScores = scoreboard.getSortedScores(obj);
        int lineCount = 0;
        int maxW = fr.getStringWidth(obj.getDisplayName());

        for (Score score : allScores) {
            if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                maxW = Math.max(maxW, fr.getStringWidth(line));
                lineCount++;
                if (lineCount >= 15) break;
            }
        }

        if (lineCount == 0) {
            anchor.w = 0;
            anchor.h = 0;
            return;
        }

        int lineH = fr.FONT_HEIGHT;
        float s = anchor.scaleSetting.getFloatValue();

        // Match ScoreboardMod's rendering math
        int scaledW = sr.getScaledWidth();
        int scaledH = sr.getScaledHeight();
        int totalH = lineCount * lineH;
        int baseY = scaledH / 2 + totalH / 3;
        int titleY = baseY - lineCount * lineH - lineH;

        int pad = 3;
        int boxRight = scaledW - pad;
        int boxLeft = boxRight - maxW - pad;

        // Apply scale from pivot (scaledW, scaledH/2)
        float pivotX = scaledW;
        float pivotY = scaledH / 2f;

        float rawX = boxLeft - 2;
        float rawY = titleY;
        float rawW = (boxRight + 1) - (boxLeft - 2);
        float rawH = baseY - titleY;

        // Transform: translate(pivot), scale, translate(-pivot)
        anchor.x = (int)(pivotX + (rawX - pivotX) * s);
        anchor.y = (int)(pivotY + (rawY - pivotY) * s);
        anchor.w = (int)(rawW * s);
        anchor.h = (int)(rawH * s);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    /** Scroll wheel resizes hovered/dragging element (including scoreboard). */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        // HUD module elements first
        HUDDragElement target = (dragging != null) ? dragging : hovered;
        if (target != null) {
            float step = 0.1f;
            float current = target.hudModule.getScale();
            float next = current + (wheel > 0 ? step : -step);
            next = Math.max(0.5f, Math.min(3.0f, next));
            target.hudModule.setScale(next);
            target.w = Math.max(target.hudModule.getWidth(), 40);
            target.h = Math.max(target.hudModule.getHeight(), 14);
            return;
        }

        // Scoreboard anchored element
        if (scoreboardHovered && scoreboardAnchor != null) {
            float step = 0.05f;
            NumberSetting ss = scoreboardAnchor.scaleSetting;
            float current = ss.getFloatValue();
            float next = current + (wheel > 0 ? step : -step);
            next = Math.max(0.3f, Math.min(2.0f, next));
            ss.setValue((double) next);
        }
    }
}
