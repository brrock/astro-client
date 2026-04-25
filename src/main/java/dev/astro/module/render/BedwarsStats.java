package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Tracks Bedwars statistics with two sections:
 *   • Game  — current game only, resets each new game
 *   • Session — since client launch, resets every restart (never persisted)
 * Renders a compact two-column HUD panel.
 */
public final class BedwarsStats extends HUDModule {

    private final ModeSetting displayMode = addSetting(
            new ModeSetting("Display", "What to show", "Both", "Both", "Session Only", "Game Only"));
    private final BooleanSetting showWhenEmpty = addSetting(
            new BooleanSetting("Show When Empty", "Show panel even with no stats", true));

    /* ── Self-death phrases (no attacker) ───────────────────────────── */
    private static final List<String> SELF_DEATH_PHRASES = Arrays.asList(
            "fell into the void", "walked into the void", "was doomed to fall",
            "fell from a high place", "fell to their death", "hit the ground too hard",
            "burned to death", "went up in flames", "was blown up", "blew up",
            "suffocated in a wall", "drowned", "died", "disconnected",
            "was squished", "was killed"
    );

    private static final List<String> CHAT_PREFIXES = Arrays.asList(
            "[ALL]", "[TEAM]", "[SHOUT]", "[PARTY]", "[PM]", "[MSG]", "[REPLY]"
    );

    /* ── Colour palette ────────────────────────────────────────────── */
    private static final int BG             = 0x80101018;
    private static final int OUTLINE        = 0x30FFFFFF;
    private static final int GRAD_LEFT      = 0xFF00C8FF;
    private static final int GRAD_RIGHT     = 0xFF7B61FF;
    private static final int COL_LABEL      = 0xFF888899;
    private static final int COL_VALUE      = 0xFFE0E0F0;
    private static final int COL_HEADER     = 0xFFFFFFFF;
    private static final int COL_SECTION    = 0xFF00C8FF;
    private static final int COL_DIVIDER    = 0x40FFFFFF;

    /* ── Layout ────────────────────────────────────────────────────── */
    private static final int PANEL_WIDTH = 165;
    private static final int LINE_H      = 11;
    private static final int PAD_X       = 6;
    private static final int PAD_Y       = 4;
    private static final int HEADER_H    = 14;
    private static final int GRAD_H      = 2;
    private static final int SECTION_H   = 12;
    private static final int COL2_OFFSET = 85;

    /* ── Game stats (reset each game) ──────────────────────────────── */
    private int gKills, gDeaths, gFinalKills, gFinalDeaths, gBedsBroken, gBedsLost;

    /* ── Session stats (reset each launch) ─────────────────────────── */
    private int sKills, sDeaths, sFinalKills, sFinalDeaths;
    private int sWins, sLosses, sWinstreak, sBedsBroken, sBedsLost;
    private int sGamesPlayed;

    /* ── Game-state tracking ───────────────────────────────────────── */
    private boolean gameResultCounted;
    private boolean wasEliminated;
    private boolean inBedwarsGame;

    private boolean dirty = true;

    /* ────────────────────────────────────────────────────────────── */

    public BedwarsStats() {
        super("Bedwars Stats", "Tracks game + session kills, finals, beds, wins.",
                Category.RENDER, 200, 15);
    }

    /* ── Lifecycle ─────────────────────────────────────────────────── */

    @Override
    protected void onEnable() {
        // Session stats start fresh every launch — just zero everything
        resetSession();
        resetGame();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private void resetSession() {
        sKills = 0; sDeaths = 0; sFinalKills = 0; sFinalDeaths = 0;
        sWins = 0; sLosses = 0; sWinstreak = 0; sBedsBroken = 0; sBedsLost = 0;
        sGamesPlayed = 0;
        dirty = true;
    }

    private void resetGame() {
        gKills = 0; gDeaths = 0; gFinalKills = 0; gFinalDeaths = 0;
        gBedsBroken = 0; gBedsLost = 0;
        gameResultCounted = false;
        wasEliminated = false;
        dirty = true;
    }

    /* ── Chat parsing ──────────────────────────────────────────────── */

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        String msg = event.message.getUnformattedText().replaceAll("\u00a7.", "").trim();
        String ign = mc.thePlayer.getName();

        for (String pfx : CHAT_PREFIXES) {
            if (msg.startsWith(pfx)) return;
        }

        // ── New game ──
        if (msg.contains("Protect your bed and destroy")) {
            resetGame();
            inBedwarsGame = true;
            sGamesPlayed++;
            dirty = true;
            return;
        }

        // ── Loss ──
        if (msg.contains("You have been eliminated!")) {
            wasEliminated = true;
            if (!gameResultCounted) {
                sLosses++;
                sWinstreak = 0;
                gameResultCounted = true;
                dirty = true;
            }
            return;
        }

        // ── Win (multi-signal) ──
        if (!gameResultCounted && inBedwarsGame) {
            boolean directWin = msg.contains("VICTORY");
            boolean endSignal = msg.contains("1st Killer")
                    || msg.contains("Bed Wars Experience")
                    || msg.contains("Winners -")
                    || msg.contains("Winner:");
            if (directWin || (endSignal && !wasEliminated)) {
                sWins++;
                sWinstreak++;
                gameResultCounted = true;
                dirty = true;
                return;
            }
        }

        // ── Bed broken by us ──
        if (msg.contains("BED DESTRUCTION") && msg.contains(ign)) {
            gBedsBroken++;
            sBedsBroken++;
            dirty = true;
            return;
        }

        // ── Our bed destroyed ──
        if (msg.contains("Your Bed was destroyed") || msg.contains("your bed was destroyed")
                || msg.contains("Bed was destroyed!")) {
            gBedsLost++;
            sBedsLost++;
            dirty = true;
            return;
        }

        // ── Kill / death parsing ──
        parseKillDeath(msg, ign);
    }

    private void parseKillDeath(String msg, String ign) {
        boolean isFinal = msg.contains("FINAL KILL!");
        String clean = msg.replace("FINAL KILL!", "").trim();
        if (clean.endsWith(".") || clean.endsWith("!")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }
        if (clean.isEmpty()) return;

        String victim = extractVictim(clean);
        victim = normalizeParticipant(victim, ign);
        if (!isValidUsername(victim)) return;

        // PvP kill via prepositions
        String killer = extractKiller(clean, " by ");
        if (killer == null) killer = extractKiller(clean, " with ");
        if (killer == null) killer = extractKiller(clean, " to ");
        if (killer == null) killer = extractKiller(clean, " from ");
        killer = normalizeParticipant(killer, ign);

        if (killer == null && clean.startsWith("You ") && !clean.startsWith("You were ")) {
            if (clean.contains(" killed ") || clean.contains(" eliminated ") || clean.contains(" defeated ")) {
                addKill(isFinal);
                return;
            }
        }

        if (killer != null && isValidUsername(killer)) {
            if (killer.equals(ign)) {
                addKill(isFinal);
            } else if (victim.equals(ign)) {
                addDeath(isFinal);
            }
            return;
        }

        // "whilst trying to escape" pattern
        int escIdx = clean.lastIndexOf(" whilst trying to escape ");
        if (escIdx > 0) {
            String escKiller = clean.substring(escIdx + 25).trim().split("[^A-Za-z0-9_]")[0];
            if (isValidUsername(escKiller)) {
                if (escKiller.equals(ign)) {
                    addKill(isFinal);
                } else if (victim.equals(ign)) {
                    addDeath(isFinal);
                }
                return;
            }
        }

        // Self-death
        if (victim.equals(ign)) {
            for (String phrase : SELF_DEATH_PHRASES) {
                if (clean.contains(phrase)) {
                    addDeath(isFinal);
                    return;
                }
            }
        }
    }

    private void addKill(boolean isFinal) {
        if (isFinal) { gFinalKills++; sFinalKills++; }
        else         { gKills++;      sKills++;       }
        dirty = true;
    }

    private void addDeath(boolean isFinal) {
        if (isFinal) { gFinalDeaths++; sFinalDeaths++; }
        else         { gDeaths++;      sDeaths++;       }
        dirty = true;
    }

    /* ── Helpers ────────────────────────────────────────────────────── */

    private static boolean isValidUsername(String s) {
        if (s == null || s.isEmpty() || s.length() > 16) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '_' && !Character.isLetterOrDigit(c)) return false;
        }
        return true;
    }

    private static String extractKiller(String clean, String prep) {
        int idx = clean.lastIndexOf(prep);
        if (idx <= 0) return null;
        String after = clean.substring(idx + prep.length()).trim();
        if (after.isEmpty()) return null;
        String candidate = after.split("[^A-Za-z0-9_]")[0];
        if (candidate.isEmpty()) return null;
        if (" to ".equals(prep) && isCommonWord(candidate)) return null;
        return candidate;
    }

    private static String extractVictim(String clean) {
        String[] words = clean.split("\\s+");
        for (String word : words) {
            String candidate = stripTrailingPunctuation(word);
            if (isValidUsername(candidate)) return candidate;
            if ("was".equalsIgnoreCase(candidate) || "were".equalsIgnoreCase(candidate)
                    || "is".equalsIgnoreCase(candidate) || "are".equalsIgnoreCase(candidate)) {
                break;
            }
        }

        int sp = clean.indexOf(' ');
        if (sp > 0) {
            String first = stripTrailingPunctuation(clean.substring(0, sp));
            if (isValidUsername(first)) return first;
        }
        return null;
    }

    private static String stripTrailingPunctuation(String s) {
        if (s == null) return null;
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (Character.isLetterOrDigit(c) || c == '_') break;
            end--;
        }
        return s.substring(0, end);
    }

    private static String normalizeParticipant(String name, String ign) {
        if (name == null) return null;
        if ("You".equalsIgnoreCase(name) || "Yourself".equalsIgnoreCase(name)) {
            return ign;
        }
        return name;
    }

    private static boolean isCommonWord(String s) {
        String low = s.toLowerCase();
        return "the".equals(low) || "their".equals(low) || "death".equals(low)
                || "a".equals(low) || "an".equals(low) || "void".equals(low)
                || "fall".equals(low) || "pieces".equals(low);
    }

    /* ── Rendering ─────────────────────────────────────────────────── */

    @Override
    protected String getText() { return ""; }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        String mode = displayMode.getValue();
        boolean showGame    = "Both".equals(mode) || "Game Only".equals(mode);
        boolean showSession = "Both".equals(mode) || "Session Only".equals(mode);

        int totalH = computeHeight(showGame, showSession);
        if (!showWhenEmpty.getValue() && sGamesPlayed == 0 && !inBedwarsGame) return;

        // Background
        RenderUtil.drawRoundedRect(x, y, x + PANEL_WIDTH, y + totalH, 4, BG);
        RenderUtil.drawOutline(x, y, x + PANEL_WIDTH, y + totalH, OUTLINE);

        int cy = y + PAD_Y;

        // Title
        String title = "Bedwars Stats";
        fr.drawStringWithShadow(title, x + (PANEL_WIDTH - fr.getStringWidth(title)) / 2.0f, cy, COL_HEADER);
        cy += HEADER_H - 2;
        RenderUtil.drawGradientH(x + PAD_X, cy, x + PANEL_WIDTH - PAD_X, cy + GRAD_H, GRAD_LEFT, GRAD_RIGHT);
        cy += GRAD_H + 3;

        // ── Game section ──
        if (showGame) {
            fr.drawStringWithShadow("\u25B8 Current Game", x + PAD_X, cy, COL_SECTION);
            cy += SECTION_H;

            double gKdr = gKills / Math.max(1.0, gDeaths);

            cy = drawRow(fr, x, cy, "Kills: ",  str(gKills),      "Deaths: ",  str(gDeaths));
            cy = drawRow(fr, x, cy, "Finals: ", str(gFinalKills), "F Deaths: ", str(gFinalDeaths));
            cy = drawRow(fr, x, cy, "Beds: ",   str(gBedsBroken), "KDR: ",      fmt(gKdr));

            if (showSession) {
                // Divider line between sections
                cy += 2;
                RenderUtil.drawRect(x + PAD_X, cy, x + PANEL_WIDTH - PAD_X, cy + 1, COL_DIVIDER);
                cy += 4;
            }
        }

        // ── Session section ──
        if (showSession) {
            fr.drawStringWithShadow("\u25B8 Session", x + PAD_X, cy, COL_SECTION);
            cy += SECTION_H;

            double sKdr  = sKills      / Math.max(1.0, sDeaths);
            double sFkdr = sFinalKills / Math.max(1.0, sFinalDeaths);
            double sWlr  = sWins      / Math.max(1.0, sLosses);

            cy = drawRow(fr, x, cy, "Kills: ",  str(sKills),      "Deaths: ",  str(sDeaths));
            cy = drawRow(fr, x, cy, "Finals: ", str(sFinalKills), "F Deaths: ", str(sFinalDeaths));
            cy = drawRow(fr, x, cy, "KDR: ",    fmt(sKdr),        "FKDR: ",     fmt(sFkdr));
            cy = drawRow(fr, x, cy, "Wins: ",   str(sWins),       "Losses: ",   str(sLosses));
            cy = drawRow(fr, x, cy, "WS: ",     str(sWinstreak),  "W/L: ",      fmt(sWlr));
            cy = drawRow(fr, x, cy, "Beds: ",   str(sBedsBroken), "B Lost: ",   str(sBedsLost));
            drawRow(fr, x, cy, "Games: ", str(sGamesPlayed), "", "");
        }
    }

    /** Draws one stat row (two label+value pairs) and returns next Y. */
    private int drawRow(FontRenderer fr, int x, int y,
                        String l1, String v1, String l2, String v2) {
        fr.drawStringWithShadow(l1, x + PAD_X, y, COL_LABEL);
        fr.drawStringWithShadow(v1, x + PAD_X + fr.getStringWidth(l1), y, COL_VALUE);
        if (!l2.isEmpty()) {
            fr.drawStringWithShadow(l2, x + COL2_OFFSET, y, COL_LABEL);
            fr.drawStringWithShadow(v2, x + COL2_OFFSET + fr.getStringWidth(l2), y, COL_VALUE);
        }
        return y + LINE_H;
    }

    private int computeHeight(boolean showGame, boolean showSession) {
        int h = PAD_Y + HEADER_H + GRAD_H + 3;
        if (showGame) {
            h += SECTION_H + (3 * LINE_H);   // header + 3 rows
            if (showSession) h += 7;          // divider gap
        }
        if (showSession) {
            h += SECTION_H + (7 * LINE_H);   // header + 7 rows
        }
        h += PAD_Y;
        return h;
    }

    @Override
    public int getBaseWidth() { return PANEL_WIDTH; }

    @Override
    public int getBaseHeight() {
        String mode = displayMode.getValue();
        return computeHeight(
                "Both".equals(mode) || "Game Only".equals(mode),
                "Both".equals(mode) || "Session Only".equals(mode));
    }

    private static String str(int v) { return String.valueOf(v); }
    private static String fmt(double v) { return String.format("%.2f", v); }
}
