package dev.astro.module.player;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scoreboard customizer — scales, repositions, or hides the sidebar scoreboard.
 * Intercepts vanilla rendering by temporarily clearing the display slot,
 * then re-renders with custom scale.
 */
public final class ScoreboardMod extends Module {

    private final NumberSetting scale = addSetting(
            new NumberSetting("Scale", "Scoreboard size multiplier", 1.0, 0.3, 2.0, 0.05));
    private final BooleanSetting hideNumbers = addSetting(
            new BooleanSetting("Hide Numbers", "Hide red score numbers", true));
    private final BooleanSetting hideScoreboard = addSetting(
            new BooleanSetting("Hide Scoreboard", "Completely hide the scoreboard", false));

    private ScoreObjective savedObjective;

    /** Exposed for HUDEditor scroll-to-resize. */
    public NumberSetting getScaleSetting() { return scale; }

    public ScoreboardMod() {
        super("Scoreboard",
              "Resize or hide the sidebar scoreboard.",
              Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        // Restore objective if it was cleared
        if (savedObjective != null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null) {
                mc.theWorld.getScoreboard().setObjectiveInDisplaySlot(1, savedObjective);
            }
            savedObjective = null;
        }
    }

    /** Before vanilla HUD renders, remove the scoreboard objective so vanilla skips it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Scoreboard sb = mc.theWorld.getScoreboard();
        savedObjective = sb.getObjectiveInDisplaySlot(1);
        if (savedObjective != null) {
            sb.setObjectiveInDisplaySlot(1, null);
        }
    }

    /** After vanilla HUD renders, restore the objective and draw our own scaled version. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (savedObjective == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        // Restore it immediately for other systems
        mc.theWorld.getScoreboard().setObjectiveInDisplaySlot(1, savedObjective);

        if (hideScoreboard.getValue()) {
            savedObjective = null;
            return;
        }

        renderScoreboard(savedObjective, new ScaledResolution(mc));
        savedObjective = null;
    }

    /** Custom scoreboard renderer based on vanilla, with scale support. */
    @SuppressWarnings("unchecked")
    private void renderScoreboard(ScoreObjective objective, ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        Scoreboard scoreboard = objective.getScoreboard();

        Collection<Score> allScores = scoreboard.getSortedScores(objective);
        List<Score> scores = new ArrayList<Score>();
        for (Score s : allScores) {
            if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) {
                scores.add(s);
            }
        }

        // Limit to top 15
        if (scores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(scores, allScores.size() - 15));
        }

        String title = objective.getDisplayName();
        int titleW = fr.getStringWidth(title);
        int maxW = titleW;

        for (Score s : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(s.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
            if (!hideNumbers.getValue()) {
                line = line + ": " + s.getScorePoints();
            }
            maxW = Math.max(maxW, fr.getStringWidth(line));
        }

        int lineCount = scores.size();
        int lineH = fr.FONT_HEIGHT;
        int totalH = lineCount * lineH;

        int scaledW = sr.getScaledWidth();
        int scaledH = sr.getScaledHeight();

        float s = scale.getFloatValue();

        // Position: right side, vertically centred (same as vanilla)
        int baseY = scaledH / 2 + totalH / 3;

        GlStateManager.pushMatrix();

        // Scale from top-right corner
        float pivotX = scaledW;
        float pivotY = scaledH / 2f;
        GlStateManager.translate(pivotX, pivotY, 0);
        GlStateManager.scale(s, s, 1f);
        GlStateManager.translate(-pivotX, -pivotY, 0);

        int pad = 3;
        int boxRight = scaledW - pad;
        int boxLeft = boxRight - maxW - pad;

        for (int i = 0; i < lineCount; i++) {
            Score score = scores.get(i);
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String points = "\u00a7c" + score.getScorePoints();

            int drawY = baseY - (lineCount - i) * lineH;

            // Background
            Gui.drawRect(boxLeft - 2, drawY, boxRight + 1, drawY + lineH, 0x60000000);

            // Name
            fr.drawStringWithShadow(name, boxLeft, drawY, 0xFFAAAAAA);

            // Score number
            if (!hideNumbers.getValue()) {
                fr.drawStringWithShadow(points, boxRight - fr.getStringWidth(points), drawY, 0xFFFF5555);
            }
        }

        // Title bar
        int titleY = baseY - lineCount * lineH - lineH;
        Gui.drawRect(boxLeft - 2, titleY, boxRight + 1, titleY + lineH, 0x90000000);
        Gui.drawRect(boxLeft - 2, titleY + lineH, boxRight + 1, titleY + lineH + 1, 0x60000000);
        fr.drawStringWithShadow(title, boxLeft + (maxW - titleW) / 2, titleY + 1, 0xFFFFFFFF);

        GlStateManager.popMatrix();
    }
}
