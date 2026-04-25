package dev.astro.module.movement;

import dev.astro.event.EventTarget;
import dev.astro.event.events.Render2DEvent;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;

/**
 * ToggleSprint — keeps the player sprinting when moving forward.
 * Purely client-side, no unfair advantage — just convenience.
 */
public final class ToggleSprint extends Module {

    private final BooleanSetting showHUD = addSetting(new BooleanSetting("Show HUD", "Show sprint status on screen", false));

    private String status = "";

    public ToggleSprint() {
        super("ToggleSprint",
              "Automatically sprints when moving forward.",
              Category.MOVEMENT);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        boolean forward  = player.movementInput.moveForward > 0;
        boolean canSprint = !player.isSneaking()
                         && player.getFoodStats().getFoodLevel() > 6.0F;

        if (forward && canSprint) {
            player.setSprinting(true);
            status = "[Sprinting (toggled)]";
        } else {
            status = "[ToggleSprint]";
        }
    }

    public String getStatus() { return status; }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!showHUD.getValue()) return;
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        if (fr != null) {
            fr.drawStringWithShadow(status, 2, 2, 0xFFFFFFFF);
        }
    }

    @Override
    protected void onEnable()  { status = "[ToggleSprint]"; }

    @Override
    protected void onDisable() { status = ""; }
}
