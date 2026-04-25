package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Animated splash screen shown once on first launch.
 * Displays AstroClient branding with a progress bar,
 * then fades out into the custom main menu.
 */
public final class LoadingScreen extends GuiScreen {

    private static final int BG           = 0xFF08080E;
    private static final int ACCENT       = 0xFF00C8FF;
    private static final int ACCENT_END   = 0xFF7B61FF;
    private static final int BAR_BG       = 0xFF1A1A24;
    private static final int TEXT_DIM     = 0xFF555566;
    private static final int TEXT_SUB     = 0xFF888899;

    /** Total duration of the splash in milliseconds. */
    private static final long DURATION_MS = 2800L;

    /** Fade-out starts at this fraction of progress (0.0–1.0). */
    private static final float FADE_START = 0.85f;

    private long startTime = -1;

    /** Loading tips / status messages cycled through. */
    private static final String[] TIPS = {
        "Loading modules...",
        "Preparing HUD...",
        "Initialising settings...",
        "Almost ready..."
    };

    @Override
    public void initGui() {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1.0f, (float) elapsed / DURATION_MS);

        // Smooth ease-out curve
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);

        // ── Fade alpha ─────────────────────────────────────────────
        float screenAlpha = 1.0f;
        if (progress > FADE_START) {
            screenAlpha = 1.0f - (progress - FADE_START) / (1.0f - FADE_START);
            screenAlpha = Math.max(0.0f, screenAlpha);
        }

        // Transition to main menu when done
        if (progress >= 1.0f) {
            mc.displayGuiScreen(new CustomMainMenu());
            return;
        }

        // ── Background ────────────────────────────────────────────
        drawRect(0, 0, this.width, this.height, BG);

        // Subtle top glow
        int glowAlpha = (int) (20 * screenAlpha);
        int glowCol = (glowAlpha << 24) | 0x00C8FF;
        drawGradientRect(this.width / 2 - 150, 0, this.width / 2 + 150, 80, glowCol, 0x00000000);

        FontRenderer fr = mc.fontRendererObj;
        int cx = this.width / 2;
        int cy = this.height / 2;

        // ── Logo text ─────────────────────────────────────────────
        GlStateManager.pushMatrix();
        {
            float logoScale = 4.0f;
            // Subtle scale-in animation
            float animScale = 0.92f + 0.08f * Math.min(1.0f, eased * 3.0f);
            float finalScale = logoScale * animScale;

            String name = AstroClient.NAME;
            float nameW = fr.getStringWidth(name);

            float px = cx / finalScale - nameW / 2f;
            float py = (cy - 40) / finalScale;

            GlStateManager.scale(finalScale, finalScale, 1.0f);
            int logoAlpha = (int) (255 * screenAlpha);
            int logoCol = (logoAlpha << 24) | 0xFFFFFF;
            fr.drawStringWithShadow(name, px, py, logoCol);
        }
        GlStateManager.popMatrix();

        // ── Accent line under logo ────────────────────────────────
        int lineW = 180;
        float lineProgress = Math.min(1.0f, eased * 2.5f);
        int drawnW = (int) (lineW * lineProgress);
        int lineX = cx - drawnW / 2;
        int lineY = cy - 8;
        if (drawnW > 0) {
            RenderUtil.drawGradientH(lineX, lineY, lineX + drawnW, lineY + 2,
                    applyAlpha(ACCENT, screenAlpha), applyAlpha(ACCENT_END, screenAlpha));
        }

        // ── Version ───────────────────────────────────────────────
        String ver = "v" + AstroClient.VERSION;
        int verAlpha = (int) (Math.min(1.0f, eased * 2.0f) * screenAlpha * 255);
        int verCol = (verAlpha << 24) | 0x666677;
        fr.drawStringWithShadow(ver, cx - fr.getStringWidth(ver) / 2f, lineY + 6, verCol);

        // ── Progress bar ──────────────────────────────────────────
        int barW = 220;
        int barH = 3;
        int barX = cx - barW / 2;
        int barY = cy + 30;

        // Bar background
        RenderUtil.drawRect(barX, barY, barX + barW, barY + barH,
                applyAlpha(BAR_BG, screenAlpha));

        // Bar fill with gradient
        int fillW = (int) (barW * eased);
        if (fillW > 0) {
            RenderUtil.drawGradientH(barX, barY, barX + fillW, barY + barH,
                    applyAlpha(ACCENT, screenAlpha), applyAlpha(ACCENT_END, screenAlpha));
        }

        // Glow dot at bar tip
        if (fillW > 2) {
            int dotX = barX + fillW - 1;
            int dotAlpha = (int) (180 * screenAlpha);
            int dotCol = (dotAlpha << 24) | 0x00C8FF;
            RenderUtil.drawRect(dotX - 1, barY - 1, dotX + 2, barY + barH + 1, dotCol);
        }

        // ── Status text ───────────────────────────────────────────
        int tipIdx = Math.min((int) (eased * TIPS.length), TIPS.length - 1);
        String tip = TIPS[tipIdx];
        int tipAlpha = (int) (Math.min(1.0f, eased * 3.0f) * screenAlpha * 255);
        int tipCol = (tipAlpha << 24) | (TEXT_DIM & 0x00FFFFFF);
        fr.drawStringWithShadow(tip, cx - fr.getStringWidth(tip) / 2f, barY + 10, tipCol);

        // ── Module count (appears after 40% progress) ─────────────
        if (eased > 0.4f) {
            int modCount = AstroClient.INSTANCE != null && AstroClient.INSTANCE.getModuleManager() != null
                    ? AstroClient.INSTANCE.getModuleManager().getModules().size() : 0;
            if (modCount > 0) {
                String modText = modCount + " modules loaded";
                float modAlpha = Math.min(1.0f, (eased - 0.4f) * 3.0f) * screenAlpha;
                int modCol = ((int)(modAlpha * 255) << 24) | (TEXT_SUB & 0x00FFFFFF);
                fr.drawStringWithShadow(modText, cx - fr.getStringWidth(modText) / 2f, barY + 22, modCol);
            }
        }

        // ── Bottom text ───────────────────────────────────────────
        int footAlpha = (int) (100 * screenAlpha);
        int footCol = (footAlpha << 24) | 0x333344;
        String foot = "Minecraft 1.8.9";
        fr.drawStringWithShadow(foot, cx - fr.getStringWidth(foot) / 2f, this.height - 14, footCol);

        // ── Floating particles (decorative) ───────────────────────
        drawParticles(cx, cy, elapsed, screenAlpha);
    }

    /**
     * Simple decorative floating dots around the logo area.
     * Uses deterministic sine waves — no random state needed.
     */
    private void drawParticles(int cx, int cy, long elapsed, float alpha) {
        int count = 12;
        for (int i = 0; i < count; i++) {
            float seed = i * 1.618f; // golden ratio spacing
            float t = (elapsed / 1000.0f) + seed;

            float px = cx + (float) Math.sin(t * 0.7 + seed * 2.4) * 120;
            float py = cy + (float) Math.cos(t * 0.5 + seed * 1.7) * 50 - 20;

            float pAlpha = (float) (Math.sin(t * 1.3 + seed) * 0.5 + 0.5) * 0.3f * alpha;
            int pCol = ((int)(pAlpha * 255) << 24) | 0x00C8FF;

            float size = 1.0f + (float) Math.sin(seed * 3.0) * 0.5f;
            RenderUtil.drawRect(px - size, py - size, px + size, py + size, pCol);
        }
    }

    /** Applies a float alpha multiplier to an ARGB colour. */
    private static int applyAlpha(int colour, float alpha) {
        int a = (int) (((colour >> 24) & 0xFF) * alpha);
        return (a << 24) | (colour & 0x00FFFFFF);
    }

    /** Prevent escape key from skipping the splash. */
    @Override
    protected void keyTyped(char c, int keyCode) {
        // Allow space/enter to skip
        if (keyCode == 28 || keyCode == 57) {
            mc.displayGuiScreen(new CustomMainMenu());
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
