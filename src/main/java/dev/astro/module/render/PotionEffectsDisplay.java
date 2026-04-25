package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.awt.Color;
import java.util.Collection;

public final class PotionEffectsDisplay extends HUDModule {

    private static final int BG   = new Color(0, 0, 0, 120).getRGB();
    private static final int EDGE = new Color(255, 255, 255, 30).getRGB();
    private static final int LINE_HEIGHT = 12;

    public PotionEffectsDisplay() {
        super("Potion Effects", "Lists active potion effects with duration.", Category.RENDER, 5, 450);
    }

    @Override
    protected String getText() {
        return null;
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty()) return;

        int w = getBaseWidth();
        int h = effects.size() * LINE_HEIGHT + 6;

        RenderUtil.drawRoundedRect(x, y, x + w, y + h, 3, BG);
        RenderUtil.drawOutline(x, y, x + w, y + h, EDGE);

        int line = 0;
        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            String name = I18n.format(potion.getName());
            int amp = effect.getAmplifier();
            if (amp > 0) {
                name += " " + toRoman(amp + 1);
            }

            int seconds = effect.getDuration() / 20;
            int mins = seconds / 60;
            int secs = seconds % 60;
            String duration = String.format("%d:%02d", mins, secs);

            String text = name + " " + duration;
            int colour = potion.getLiquidColor() | 0xFF000000;
            fr.drawStringWithShadow(text, x + 5, y + 3 + line * LINE_HEIGHT, colour);
            line++;
        }
    }

    private static String toRoman(int num) {
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(num);
        }
    }

    @Override
    public int getBaseWidth() {
        return 140;
    }

    @Override
    public int getBaseHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return LINE_HEIGHT + 6;
        int count = mc.thePlayer.getActivePotionEffects().size();
        return Math.max(1, count) * LINE_HEIGHT + 6;
    }
}
