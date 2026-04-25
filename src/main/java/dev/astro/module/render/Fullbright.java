package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Fullbright — sets gamma to max so you can see in the dark.
 */
public final class Fullbright extends Module {

    private float originalGamma;

    public Fullbright() {
        super("Fullbright", "See in the dark.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        originalGamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft.getMinecraft().gameSettings.gammaSetting = 100.0F;
    }

    @Override
    protected void onDisable() {
        Minecraft.getMinecraft().gameSettings.gammaSetting = originalGamma;
    }
}
