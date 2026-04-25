package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.Render2DEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Zoom — smoothly zooms in while KEY_Q is held.
 * Scroll wheel adjusts the zoom level while active.
 */
public final class Zoom extends Module {

    private final NumberSetting defaultFov = addSetting(new NumberSetting("Default FOV", "Starting zoom FOV", 30.0, 5.0, 60.0, 1.0));
    private final BooleanSetting smoothCamera = addSetting(new BooleanSetting("Smooth Camera", "Enable cinematic camera while zoomed", true));
    private final NumberSetting scrollStep = addSetting(new NumberSetting("Scroll Step", "FOV change per scroll", 5.0, 1.0, 15.0, 1.0));

    private static final float MIN_ZOOM_FOV = 5.0F;
    private static final float MAX_ZOOM_FOV = 60.0F;
    private static final float LERP_SPEED = 0.3F;

    private float originalFov = 70.0F;
    private float targetFov;
    private float currentFov;
    private boolean wasZooming = false;
    private float originalSensitivity;
    private boolean originalSmoothCamera;

    public Zoom() {
        super("Zoom",
              "Smoothly zooms in while key is held. Scroll to adjust.",
              Category.RENDER,
              Keyboard.KEY_Q);
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
    public void onRender(Render2DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || getKeyBind() == Keyboard.KEY_NONE) {
            resetZoom(mc);
            return;
        }

        boolean keyHeld = Keyboard.isKeyDown(getKeyBind());

        if (keyHeld) {
            if (!wasZooming) {
                originalFov = mc.gameSettings.fovSetting;
                originalSensitivity = mc.gameSettings.mouseSensitivity;
                originalSmoothCamera = mc.gameSettings.smoothCamera;
                currentFov = originalFov;
                targetFov = defaultFov.getFloatValue();
                wasZooming = true;
                if (smoothCamera.getValue()) {
                    mc.gameSettings.smoothCamera = true;
                }
            }

            // Mouse scroll to adjust zoom level
            float step = scrollStep.getFloatValue();
            int wheel = Mouse.getDWheel();
            if (wheel > 0) {
                targetFov = Math.max(MIN_ZOOM_FOV, targetFov - step);
            } else if (wheel < 0) {
                targetFov = Math.min(MAX_ZOOM_FOV, targetFov + step);
            }

            // Smooth lerp toward target
            currentFov = currentFov + (targetFov - currentFov) * LERP_SPEED;
            mc.gameSettings.fovSetting = currentFov;

            // Reduce sensitivity proportionally while zoomed
            float fovRatio = currentFov / originalFov;
            mc.gameSettings.mouseSensitivity = originalSensitivity * fovRatio;

        } else {
            resetZoom(mc);
        }
    }

    @Override
    protected void onDisable() {
        resetZoom(Minecraft.getMinecraft());
    }

    private void resetZoom(Minecraft mc) {
        if (wasZooming) {
            mc.gameSettings.fovSetting = originalFov;
            mc.gameSettings.mouseSensitivity = originalSensitivity;
            mc.gameSettings.smoothCamera = originalSmoothCamera;
            wasZooming = false;
        }
        targetFov = defaultFov.getFloatValue();
    }
}
