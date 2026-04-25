package dev.astro.module.world;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import net.minecraft.client.Minecraft;

/**
 * WeatherChanger — disables rain and snow rendering client-side.
 * Clears rain and thunder strength every tick.
 */
public final class WeatherChanger extends Module {

    public WeatherChanger() {
        super("WeatherChanger",
              "Disables rain and snow rendering.",
              Category.WORLD);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            mc.theWorld.setRainStrength(0.0F);
            mc.theWorld.setThunderStrength(0.0F);
        }
    }
}
