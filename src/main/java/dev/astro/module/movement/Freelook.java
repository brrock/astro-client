package dev.astro.module.movement;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import org.lwjgl.input.Keyboard;

/**
 * Freelook — decouples camera rotation from player movement direction
 * while Left-Alt is held. The player continues moving in the original
 * direction while the camera can look around freely.
 *
 * On each tick while active, the player's rotationYaw and rotationPitch
 * are restored to the stored values so movement direction stays fixed.
 */
public final class Freelook extends Module {

    private boolean active = false;
    private float storedYaw;
    private float storedPitch;
    private float cameraYaw;
    private float cameraPitch;

    public Freelook() {
        super("Freelook",
              "Look around without changing movement direction (hold LAlt).",
              Category.MOVEMENT,
              Keyboard.KEY_LMENU);
    }

    @Override
    public ActivationMode getActivationMode() {
        return ActivationMode.HOLD;
    }

    @Override
    public boolean allowsKeyBind() {
        return true;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        boolean keyHeld = Keyboard.isKeyDown(Keyboard.KEY_LMENU);

        if (keyHeld) {
            if (!active) {
                // Entering freelook: store current rotation
                storedYaw = player.rotationYaw;
                storedPitch = player.rotationPitch;
                cameraYaw = storedYaw;
                cameraPitch = storedPitch;
                active = true;
            }

            // Record where the camera moved this tick
            cameraYaw = player.rotationYaw;
            cameraPitch = player.rotationPitch;

            // Restore movement rotation so the player walks in the original direction
            player.rotationYaw = storedYaw;
            player.rotationPitch = storedPitch;

            // Set render-only rotation so the camera shows the freelook angle
            player.renderYawOffset = cameraYaw;
            player.rotationYawHead = cameraYaw;

        } else if (active) {
            // Release: snap camera back to movement direction
            player.rotationYaw = cameraYaw;
            player.rotationPitch = cameraPitch;
            active = false;
        }
    }

    @Override
    protected void onDisable() {
        if (active) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.rotationYaw = cameraYaw;
                mc.thePlayer.rotationPitch = cameraPitch;
            }
            active = false;
        }
    }
}
