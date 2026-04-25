package dev.astro.module.world;

import com.google.gson.JsonObject;
import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.util.ConfigManager;
import net.minecraft.client.Minecraft;

/**
 * TimeChanger — overrides the visual time of day client-side.
 * Default is 6000 (noon). Does not affect the server.
 */
public final class TimeChanger extends Module implements ConfigManager.Configurable {

    private long targetTime = 6000L;

    public TimeChanger() {
        super("TimeChanger",
              "Changes the visual time of day without affecting the server.",
              Category.WORLD);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            mc.theWorld.setWorldTime(targetTime);
        }
    }

    public long getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(long targetTime) {
        this.targetTime = targetTime;
    }

    @Override
    public void saveConfig(JsonObject obj) {
        obj.addProperty("targetTime", targetTime);
    }

    @Override
    public void loadConfig(JsonObject obj) {
        if (obj.has("targetTime")) {
            targetTime = obj.get("targetTime").getAsLong();
        }
    }
}
