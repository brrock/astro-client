package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Bedwars game-event timer HUD.
 */
public final class BedwarsTimers extends HUDModule {

    private static final String[] START_PHRASES = {
            "protect your bed and destroy",
    };

    private static final String[] END_PHRASES = {
            "game over",
            "victory",
            "you have been eliminated",
            "1st killer"
    };

    private static final long HIDE_PASSED_AFTER_MS = 10_000L;

    private final BooleanSetting showDiamonds = addSetting(
            new BooleanSetting("Show Diamonds", "Show diamond upgrade timers", true));
    private final BooleanSetting showEmeralds = addSetting(
            new BooleanSetting("Show Emeralds", "Show emerald upgrade timers", true));
    private final BooleanSetting showBedGone = addSetting(
            new BooleanSetting("Show Bed Gone", "Show bed destruction timer", true));
    private final BooleanSetting showGameEnd = addSetting(
            new BooleanSetting("Show Game End", "Show sudden death / game over timer", true));
    private final BooleanSetting compactMode = addSetting(
            new BooleanSetting("Compact", "Single-line timers", false));

    private static final int BG      = 0x80101018;
    private static final int OUTLINE = 0x30FFFFFF;
    private static final int GREEN   = 0xFF55FF55;
    private static final int YELLOW  = 0xFFFFFF55;
    private static final int RED     = 0xFFFF5555;
    private static final int AQUA    = 0xFF55FFFF;
    private static final int D_GREEN = 0xFF00AA00;
    private static final int HEADER  = 0xFFFFFFFF;

    private static final int PAD     = 5;
    private static final int ROW_H   = 12;
    private static final int PANEL_W = 150;

    private static final int DIAMOND_II   =  6 * 60;
    private static final int EMERALD_II   = 12 * 60;
    private static final int DIAMOND_III  = 18 * 60;
    private static final int EMERALD_III  = 24 * 60;
    private static final int BED_GONE     = 30 * 60;
    private static final int SUDDEN_DEATH = 40 * 60;
    private static final int GAME_END_T   = 50 * 60;

    private final List<TimerEntry> timers = new ArrayList<TimerEntry>();

    private boolean inGame;
    private long gameStartMs;

    public BedwarsTimers() {
        super("Bedwars Timers", "Countdown timers for Bedwars game events.",
                Category.RENDER, 10, 80);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        resetGame();
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        resetGame();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        resetGame();
    }

    private void resetGame() {
        inGame = false;
        gameStartMs = 0L;
        timers.clear();
    }

    private void startGame() {
        inGame = true;
        gameStartMs = System.currentTimeMillis();
        timers.clear();

        if (showDiamonds.getValue()) addTimer("Diamond II", DIAMOND_II, AQUA);
        if (showEmeralds.getValue()) addTimer("Emerald II", EMERALD_II, D_GREEN);
        if (showDiamonds.getValue()) addTimer("Diamond III", DIAMOND_III, AQUA);
        if (showEmeralds.getValue()) addTimer("Emerald III", EMERALD_III, D_GREEN);
        if (showBedGone.getValue()) addTimer("Bed Gone", BED_GONE, RED);
        if (showGameEnd.getValue()) addTimer("Sudden Death", SUDDEN_DEATH, YELLOW);
        if (showGameEnd.getValue()) addTimer("Game End", GAME_END_T, RED);
    }

    private void addTimer(String name, int targetSec, int colour) {
        timers.add(new TimerEntry(name, targetSec, colour));
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return;

        String msg = normalize(event.message.getUnformattedText());

        if (!inGame) {
            if (containsAny(msg, START_PHRASES)) {
                startGame();
            }
            return;
        }

        if (containsAny(msg, END_PHRASES)) {
            resetGame();
            return;
        }

        if (msg.contains("diamond") && msg.contains("upgraded")) {
            markPassed("Diamond");
        }
        if (msg.contains("emerald") && msg.contains("upgraded")) {
            markPassed("Emerald");
        }
        if (msg.contains("all beds have been destroyed") || msg.contains("bed destruction")) {
            markPassed("Bed Gone");
        }
    }

    private void markPassed(String prefix) {
        long now = System.currentTimeMillis();
        for (TimerEntry timer : timers) {
            if (!timer.passed && timer.name.startsWith(prefix)) {
                timer.passed = true;
                timer.passedAtMs = now;
                return;
            }
        }
    }

    @Override
    protected String getText() {
        return "";
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        if (!inGame || timers.isEmpty()) return;

        long now = System.currentTimeMillis();
        long elapsed = (now - gameStartMs) / 1000L;

        int visible = updateAndCountVisibleTimers(now, elapsed);
        if (visible == 0) return;

        int panelH = PAD + 14 + 2 + (visible * ROW_H) + PAD;

        RenderUtil.drawRoundedRect(x, y, x + PANEL_W, y + panelH, 4, BG);
        RenderUtil.drawOutline(x, y, x + PANEL_W, y + panelH, OUTLINE);

        String header = "Bedwars Timers";
        int hw = fr.getStringWidth(header);
        fr.drawStringWithShadow(header, x + (PANEL_W - hw) / 2.0f, y + PAD, HEADER);

        int barY = y + PAD + 11;
        RenderUtil.drawGradientH(x + PAD, barY, x + PANEL_W - PAD, barY + 2, AQUA, D_GREEN);

        int rowY = barY + 4;
        for (TimerEntry timer : timers) {
            if (timer.passed && now - timer.passedAtMs > HIDE_PASSED_AFTER_MS) continue;

            long remaining = timer.targetSeconds - elapsed;
            int labelCol = timer.passed ? 0xFF555555 : timer.colour;
            int timeCol  = timer.passed ? 0xFF555555 : getTimeColor(remaining);

            String timeStr;
            if (timer.passed || remaining <= 0) {
                timeStr = "\u2713";
            } else {
                long mins = remaining / 60;
                long secs = remaining % 60;
                timeStr = String.format("%d:%02d", mins, secs);
            }

            if (compactMode.getValue()) {
                fr.drawStringWithShadow(timer.name + " " + timeStr, x + PAD, rowY, labelCol);
            } else {
                fr.drawStringWithShadow(timer.name, x + PAD, rowY, labelCol);
                int tw = fr.getStringWidth(timeStr);
                fr.drawStringWithShadow(timeStr, x + PANEL_W - PAD - tw, rowY, timeCol);
            }
            rowY += ROW_H;
        }
    }

    private static int getTimeColor(long remaining) {
        if (remaining <= 30) return RED;
        if (remaining <= 60) return YELLOW;
        return GREEN;
    }

    private static String normalize(String msg) {
        return msg == null ? "" : msg.replaceAll("\u00a7.", "").toLowerCase();
    }

    private static boolean containsAny(String msg, String[] phrases) {
        for (String phrase : phrases) {
            if (msg.contains(phrase)) return true;
        }
        return false;
    }

    @Override
    public int getBaseWidth() {
        return PANEL_W;
    }

    @Override
    public int getBaseHeight() {
        if (!inGame || timers.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        long elapsed = (now - gameStartMs) / 1000L;
        int visible = updateAndCountVisibleTimers(now, elapsed);
        return visible == 0 ? 0 : PAD + 14 + 2 + (visible * ROW_H) + PAD;
    }

    private int updateAndCountVisibleTimers(long now, long elapsed) {
        int visible = 0;
        for (TimerEntry timer : timers) {
            if (elapsed > timer.targetSeconds) {
                timer.passed = true;
                if (timer.passedAtMs == 0L) timer.passedAtMs = now;
            }
            if (isVisible(timer, now)) visible++;
        }
        return visible;
    }

    private static boolean isVisible(TimerEntry timer, long now) {
        return !timer.passed || now - timer.passedAtMs <= HIDE_PASSED_AFTER_MS;
    }

    private static final class TimerEntry {
        final String name;
        final int targetSeconds;
        final int colour;
        boolean passed;
        long passedAtMs;

        TimerEntry(String name, int targetSeconds, int colour) {
            this.name = name;
            this.targetSeconds = targetSeconds;
            this.colour = colour;
        }
    }
}
