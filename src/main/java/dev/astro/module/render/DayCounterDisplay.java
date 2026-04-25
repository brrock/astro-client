package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import net.minecraft.client.Minecraft;

public final class DayCounterDisplay extends HUDModule {

    public DayCounterDisplay() {
        super("Day Counter", "Shows the current in-game day.", Category.RENDER, 5, 260);
    }

    @Override
    protected String getText() {
        if (Minecraft.getMinecraft().theWorld == null) return "";
        long day = Minecraft.getMinecraft().theWorld.getWorldTime() / 24000L;
        return "Day " + day;
    }
}
