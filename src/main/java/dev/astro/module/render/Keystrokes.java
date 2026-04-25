package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.MouseClickEvent;
import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.util.LinkedList;

/**
 * Keystrokes — renders a compact W-A-S-D + LMB/RMB overlay.
 * Extends HUDModule so it is draggable in the HUD Editor.
 */
public final class Keystrokes extends HUDModule {

    private final BooleanSetting showCPS = addSetting(new BooleanSetting("Show CPS", "Show CPS in mouse buttons", false));
    private final NumberSetting scale = addSetting(new NumberSetting("Scale", "Size scale", 1.0, 0.5, 2.0, 0.1));
    private final NumberSetting backgroundOpacity = addSetting(new NumberSetting("Background Opacity", "Background transparency", 100.0, 0.0, 255.0, 5.0));

    private static final int BOX   = 22;
    private static final int GAP   = 2;

    private static final int BG_ACTIVE   = new Color(255, 255, 255, 180).getRGB();
    private static final int BORDER      = new Color(255, 255, 255, 40).getRGB();

    private int leftCPS;
    private int rightCPS;
    private final LinkedList<Long> leftClicks = new LinkedList<Long>();
    private final LinkedList<Long> rightClicks = new LinkedList<Long>();

    public Keystrokes() {
        super("Keystrokes",
              "Shows W/A/S/D and mouse buttons on screen.",
              Category.RENDER,
              5, 40);
    }

    @Override
    protected String getText() {
        return null; // custom drawHUD
    }

    @EventTarget
    public void onMouseClick(MouseClickEvent event) {
        long now = System.currentTimeMillis();
        if (event.getButton() == 0) {
            leftClicks.add(now);
        } else if (event.getButton() == 1) {
            rightClicks.add(now);
        }
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        GameSettings gs = mc.gameSettings;

        long now = System.currentTimeMillis();
        pruneClicks(leftClicks, now);
        pruneClicks(rightClicks, now);
        leftCPS = leftClicks.size();
        rightCPS = rightClicks.size();

        int bgInactive = new Color(0, 0, 0, backgroundOpacity.getIntValue()).getRGB();

        float s = scale.getFloatValue();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(s, s, 1.0F);

        int kx = 0, ky = 0;

        // Row 1: W
        drawKey(fr, gs.keyBindForward, "W", kx + BOX + GAP, ky, bgInactive);
        // Row 2: A S D
        ky += BOX + GAP;
        drawKey(fr, gs.keyBindLeft,  "A", kx,                ky, bgInactive);
        drawKey(fr, gs.keyBindBack,  "S", kx + BOX + GAP,    ky, bgInactive);
        drawKey(fr, gs.keyBindRight, "D", kx + 2*(BOX+GAP),  ky, bgInactive);
        // Row 3: LMB / RMB
        ky += BOX + GAP;
        int halfW = (3 * BOX + 2 * GAP - GAP) / 2;
        String lmbLabel = showCPS.getValue() ? String.valueOf(leftCPS) : "LMB";
        String rmbLabel = showCPS.getValue() ? String.valueOf(rightCPS) : "RMB";
        drawButton(fr, Mouse.isButtonDown(0), lmbLabel, kx, ky, halfW, bgInactive);
        drawButton(fr, Mouse.isButtonDown(1), rmbLabel, kx + halfW + GAP, ky, halfW, bgInactive);

        GlStateManager.popMatrix();
    }

    private void pruneClicks(LinkedList<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peek() > 1000) {
            clicks.poll();
        }
    }

    private void drawKey(FontRenderer fr, KeyBinding bind, String label, int x, int y, int bgInactive) {
        boolean on = bind.isKeyDown();
        RenderUtil.drawRoundedRect(x, y, x + BOX, y + BOX, 2, on ? BG_ACTIVE : bgInactive);
        RenderUtil.drawOutline(x, y, x + BOX, y + BOX, BORDER);
        fr.drawStringWithShadow(label,
                x + (BOX - fr.getStringWidth(label)) / 2f,
                y + (BOX - fr.FONT_HEIGHT) / 2f,
                on ? 0xFF1a1a1a : 0xFFFFFFFF);
    }

    private void drawButton(FontRenderer fr, boolean pressed, String label, int x, int y, int w, int bgInactive) {
        RenderUtil.drawRoundedRect(x, y, x + w, y + BOX, 2, pressed ? BG_ACTIVE : bgInactive);
        RenderUtil.drawOutline(x, y, x + w, y + BOX, BORDER);
        fr.drawStringWithShadow(label,
                x + (w - fr.getStringWidth(label)) / 2f,
                y + (BOX - fr.FONT_HEIGHT) / 2f,
                pressed ? 0xFF1a1a1a : 0xFFFFFFFF);
    }

    @Override
    public int getBaseWidth()  { return (int)((3 * BOX + 2 * GAP) * scale.getFloatValue()); }

    @Override
    public int getBaseHeight() { return (int)((3 * BOX + 2 * GAP) * scale.getFloatValue()); }

    @Override
    protected void onDisable() {
        leftClicks.clear();
        rightClicks.clear();
    }
}
