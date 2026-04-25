package dev.astro.module.world;

import dev.astro.module.Category;
import dev.astro.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * MenuBlur — blurs the background when a GUI is open (like Lunar Client).
 * Loads the vanilla blur shader when a GUI opens and removes it when closed.
 */
public final class MenuBlur extends Module {

    private static final ResourceLocation BLUR_SHADER =
            new ResourceLocation("shaders/post/blur.json");

    public MenuBlur() {
        super("MenuBlur",
              "Blurs the game background when a GUI is open.",
              Category.WORLD);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);

        // If a GUI is already open, apply blur immediately
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null && mc.entityRenderer != null) {
            mc.entityRenderer.loadShader(BLUR_SHADER);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer != null) {
            mc.entityRenderer.stopUseShader();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.entityRenderer == null) return;

        if (event.gui != null) {
            mc.entityRenderer.loadShader(BLUR_SHADER);
        } else {
            mc.entityRenderer.stopUseShader();
        }
    }
}
