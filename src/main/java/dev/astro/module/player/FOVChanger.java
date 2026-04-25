package dev.astro.module.player;

import com.google.gson.JsonObject;
import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.util.ConfigManager;
import net.minecraft.client.Minecraft;

/**
 * FOVChanger — overrides the field of view to a configurable value.
 * Default is 90. Persisted via ConfigManager.
 */
public final class FOVChanger extends Module implements ConfigManager.Configurable {

    private float fov = 90.0F;
    private float originalFov = 70.0F;

    public FOVChanger() {
        super("FOVChanger",
              "Changes the field of view to a custom value.",
              Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        originalFov = Minecraft.getMinecraft().gameSettings.fovSetting;
    }

    @Override
    protected void onDisable() {
        Minecraft.getMinecraft().gameSettings.fovSetting = originalFov;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft.getMinecraft().gameSettings.fovSetting = fov;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    @Override
    public void saveConfig(JsonObject obj) {
        obj.addProperty("fov", fov);
    }

    @Override
    public void loadConfig(JsonObject obj) {
        if (obj.has("fov")) {
            fov = obj.get("fov").getAsFloat();
        }
    }
}
