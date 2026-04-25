package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.event.EventTarget;
import dev.astro.event.events.Render2DEvent;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * HUD — renders the watermark only.
 * Module elements (Keystrokes, CPS) handle their own rendering.
 * Positions are editable via the HUD Editor.
 */
public final class HUD {

    private static final int ACCENT     = 0xFF00C8FF;
    private static final int ACCENT_END = 0xFF7B61FF;

    @EventTarget
    public void onRender(Render2DEvent event) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr == null) return;
        drawWatermark(fr);
    }

    private void drawWatermark(FontRenderer fr) {
        String text = AstroClient.NAME;
        int pad = 5;
        int w = fr.getStringWidth(text) + pad * 2 + 2;
        int h = fr.FONT_HEIGHT + pad * 2;

        RenderUtil.drawRoundedRect(3, 3, 3 + w, 3 + h, 2, 0xCC111111);
        RenderUtil.drawGradientH(3, 3, 3 + w, 5, ACCENT, ACCENT_END);
        fr.drawStringWithShadow(text, 3 + pad, 3 + pad, 0xFFFFFFFF);
    }
}
