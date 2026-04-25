package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import net.minecraft.client.Minecraft;

public final class FPSDisplay extends HUDModule {

    private final BooleanSetting showLabel = addSetting(new BooleanSetting("Show Label", "Show 'FPS' suffix", true));
    private final ColorSetting textColor = addSetting(new ColorSetting("Text Color", "FPS text color", 0xFFFFFFFF));

    public FPSDisplay() {
        super("FPS Display", "Shows current frames per second.", Category.RENDER, 5, 20);
    }

    @Override
    protected String getText() {
        int fps = Minecraft.getDebugFPS();
        return showLabel.getValue() ? fps + " FPS" : String.valueOf(fps);
    }

    @Override
    protected int getTextColour() {
        return textColor.getValue();
    }
}
