package dev.astro.module.misc;

import com.google.gson.JsonObject;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.util.ConfigManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * Chat improvements: optional timestamps and duplicate-message stacking.
 */
public final class ChatMod extends Module implements ConfigManager.Configurable {

    private static final int STACK_HISTORY = 64;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    private boolean timestamps = true;
    private boolean stacking   = true;

    private final LinkedList<StackEntry> recentMessages = new LinkedList<StackEntry>();

    public ChatMod() {
        super("ChatMod", "Adds timestamps and message stacking to chat.", Category.MISC);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        recentMessages.clear();
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        recentMessages.clear();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return; // ignore action-bar

        String raw = event.message.getUnformattedText();
        String formatted = event.message.getFormattedText();

        /* ── Stacking ─────────────────────────────────────────── */
        if (stacking) {
            for (StackEntry entry : recentMessages) {
                if (entry.text.equals(raw)) {
                    entry.count++;
                    event.setCanceled(true);

                    IChatComponent updated = buildMessage(entry.displayText, entry.count);
                    // Replace the previous line by sending a new message
                    net.minecraft.client.Minecraft.getMinecraft()
                            .ingameGUI.getChatGUI().printChatMessage(updated);
                    return;
                }
            }

            // New unique message — track it
            if (recentMessages.size() >= STACK_HISTORY) {
                recentMessages.removeFirst();
            }
            recentMessages.addLast(new StackEntry(raw, formatted));
        }

        /* ── Timestamp ────────────────────────────────────────── */
        if (timestamps) {
            String time = "\u00a77[" + TIME_FMT.format(new Date()) + "]\u00a7r ";
            event.message = new ChatComponentText(time).appendSibling(event.message);
        }
    }

    private IChatComponent buildMessage(String displayText, int count) {
        StringBuilder sb = new StringBuilder();
        if (timestamps) {
            sb.append("\u00a77[").append(TIME_FMT.format(new Date())).append("]\u00a7r ");
        }
        sb.append(displayText);
        sb.append(" \u00a77(x").append(count).append(")\u00a7r");
        return new ChatComponentText(sb.toString());
    }

    /* ── Config persistence ───────────────────────────────────── */

    @Override
    public void saveConfig(JsonObject obj) {
        obj.addProperty("timestamps", timestamps);
        obj.addProperty("stacking", stacking);
    }

    @Override
    public void loadConfig(JsonObject obj) {
        if (obj.has("timestamps")) {
            timestamps = obj.get("timestamps").getAsBoolean();
        }
        if (obj.has("stacking")) {
            stacking = obj.get("stacking").getAsBoolean();
        }
    }

    /* ── Internal ─────────────────────────────────────────────── */

    private static final class StackEntry {
        final String text;
        final String displayText;
        int count;

        StackEntry(String text, String displayText) {
            this.text  = text;
            this.displayText = displayText;
            this.count = 1;
        }
    }
}
