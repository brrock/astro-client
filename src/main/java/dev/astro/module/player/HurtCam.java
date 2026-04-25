package dev.astro.module.player;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import net.minecraft.client.Minecraft;

/**
 * HurtCam — disables the screen shake effect when taking damage.
 * Resets the player's hurtTime to 0 every tick.
 */
public final class HurtCam extends Module {

    public HurtCam() {
        super("NoHurtCam",
              "Disables the screen shake when taking damage.",
              Category.PLAYER);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.hurtTime = 0;
        }
    }
}
