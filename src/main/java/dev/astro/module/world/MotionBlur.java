package dev.astro.module.world;

import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * MotionBlur — applies a motion blur post-processing shader.
 * Uses the vanilla "phosphor" shader for the trailing effect.
 */
public final class MotionBlur extends Module {

    private final NumberSetting amount = addSetting(new NumberSetting("Amount", "Blur intensity (1-10)", 4.0, 1.0, 10.0, 1.0));

    private static final ResourceLocation PHOSPHOR_SHADER =
            new ResourceLocation("shaders/post/phosphor.json");

    public MotionBlur() {
        super("MotionBlur",
              "Applies a motion blur effect to the camera.",
              Category.WORLD);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null) {
            mc.entityRenderer.loadShader(PHOSPHOR_SHADER);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null) {
            mc.entityRenderer.stopUseShader();
        }
    }
}
