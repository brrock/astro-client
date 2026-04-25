package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovingObjectPosition;

public final class ReachDisplay extends HUDModule {

    private final NumberSetting displayTime = addSetting(new NumberSetting("Display Time", "Seconds to show reach", 3.0, 1.0, 10.0, 0.5));
    private final NumberSetting decimals = addSetting(new NumberSetting("Decimals", "Decimal places", 2.0, 0.0, 3.0, 1.0));

    private double lastDistance;
    private long lastHitTime;

    public ReachDisplay() {
        super("Reach Display", "Shows the distance of your last hit.", Category.RENDER, 5, 320);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mop.entityHit != null
                && player.isSwingInProgress) {
            lastDistance = player.getDistanceToEntity(mop.entityHit);
            lastHitTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onDisable() {
        lastDistance = 0;
        lastHitTime = 0;
    }

    @Override
    protected String getText() {
        long durationMs = (long)(displayTime.getValue() * 1000);
        if (System.currentTimeMillis() - lastHitTime > durationMs) return "";
        int dec = decimals.getIntValue();
        return String.format("%." + dec + "f blocks", lastDistance);
    }
}
