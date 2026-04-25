package dev.astro.module.misc;

import com.google.gson.JsonObject;
import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.util.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically sends a configurable message (default "gg") in chat
 * when a Hypixel game ends.
 */
public final class AutoGG extends Module implements ConfigManager.Configurable {

    private static final List<String> END_PHRASES = Arrays.asList(
            "1st Killer", "1st Place", "Winner:", "Bed Wars -", "BedWars -",
            "You won!", "You lost!", "Game Over!", " - Effective",
            "Top Kills", "Winners -", "Game End"
    );

    private static final int SEND_DELAY_TICKS = 20;       // 1 second
    private static final long COOLDOWN_MS     = 5000L;     // 5 seconds

    private String  ggMessage       = "gg";
    private int     pendingTicks    = -1;
    private long    lastSentTime    = 0L;

    public AutoGG() {
        super("AutoGG", "Sends 'gg' when a Hypixel game ends.", Category.MISC);
    }

    /* ── Forge bus: chat detection ────────────────────────────── */

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        pendingTicks = -1;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        pendingTicks = -1;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // ignore action-bar messages

        String msg = event.message.getUnformattedText();
        if (pendingTicks >= 0) return; // already queued

        for (String phrase : END_PHRASES) {
            if (msg.contains(phrase)) {
                long now = System.currentTimeMillis();
                if (now - lastSentTime < COOLDOWN_MS) return;
                pendingTicks = SEND_DELAY_TICKS;
                return;
            }
        }
    }

    /* ── Internal bus: tick countdown ─────────────────────────── */

    @EventTarget
    public void onTick(TickEvent event) {
        if (pendingTicks < 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            pendingTicks = -1;
            return;
        }

        if (--pendingTicks <= 0) {
            mc.thePlayer.sendChatMessage(ggMessage);
            lastSentTime = System.currentTimeMillis();
            pendingTicks = -1;
        }
    }

    /* ── Config persistence ───────────────────────────────────── */

    @Override
    public void saveConfig(JsonObject obj) {
        obj.addProperty("ggMessage", ggMessage);
    }

    @Override
    public void loadConfig(JsonObject obj) {
        if (obj.has("ggMessage")) {
            ggMessage = obj.get("ggMessage").getAsString();
        }
    }
}
