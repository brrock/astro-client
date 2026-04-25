package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovingObjectPosition;

public final class ComboCounter extends HUDModule {

    private final NumberSetting resetTimeout = addSetting(new NumberSetting("Reset Timeout", "Seconds before combo resets", 3.0, 1.0, 10.0, 0.5));
    private final BooleanSetting hideAtZero = addSetting(new BooleanSetting("Hide At Zero", "Hide display when combo is 0", true));

    private int combo;
    private boolean wasAttacking;
    private int lastHurtTime;
    private long lastHitTime;

    public ComboCounter() {
        super("Combo Counter", "Counts consecutive hits on entities.", Category.RENDER, 5, 300);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Reset combo when player gets freshly hurt
        if (player.hurtTime > 0 && player.hurtTime == player.maxHurtTime - 1) {
            combo = 0;
        }

        // Detect player swinging at an entity
        MovingObjectPosition mop = mc.objectMouseOver;
        boolean attacking = mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && player.isSwingInProgress;

        if (attacking && !wasAttacking) {
            combo++;
            lastHitTime = System.currentTimeMillis();
        }
        wasAttacking = attacking;

        // Reset combo after timeout
        if (combo > 0 && System.currentTimeMillis() - lastHitTime > (long)(resetTimeout.getValue() * 1000)) {
            combo = 0;
        }
    }

    @Override
    protected void onDisable() {
        combo = 0;
        wasAttacking = false;
        lastHitTime = 0;
    }

    @Override
    protected String getText() {
        if (hideAtZero.getValue() && combo == 0) return "";
        return combo + " Combo";
    }
}
