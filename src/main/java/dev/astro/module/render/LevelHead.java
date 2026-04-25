package dev.astro.module.render;

import dev.astro.AstroClient;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.module.setting.NumberSetting;
import dev.astro.util.HypixelAPI;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.potion.Potion;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Displays each player's Bedwars star, Hypixel level, or both above their nametag.
 * Also exposes inline text so NametagMod can fold level data into its custom tag.
 */
public final class LevelHead extends Module {

    private static final String[] START_PHRASES = {
            "protect your bed and destroy",
            "the game starts in 1 second",
            "the game starts in 5 seconds",
            "the game starts in 10 seconds"
    };

    private static final String[] END_PHRASES = {
            "game over",
            "victory",
            "you have been eliminated",
            "1st killer"
    };

    private final BooleanSetting showBackground = addSetting(
            new BooleanSetting("Background", "Show background behind level", true));
    private final BooleanSetting showOwnLevel = addSetting(
            new BooleanSetting("Show Own Level", "Display your own level", true));
    private final ModeSetting displayMode = addSetting(
            new ModeSetting("Display", "What to show above heads", "BW Stars", "BW Stars", "Hypixel Level", "Both"));
    private final BooleanSetting useLevelColors = addSetting(
            new BooleanSetting("Level Colors", "Use Bedwars prestige colours", true));
    private final BooleanSetting textShadow = addSetting(
            new BooleanSetting("Text Shadow", "Draw text shadow", true));
    private final NumberSetting renderDistance = addSetting(
            new NumberSetting("Render Distance", "Max blocks away to render", 64, 16, 128, 8));
    private final NumberSetting bgOpacity = addSetting(
            new NumberSetting("BG Opacity", "Background transparency", 0.25, 0.0, 1.0, 0.05));

    private static final float TEXT_SCALE = 0.016666668F * 1.6F;
    private static final float SEE_THROUGH_ALPHA = 0.2F;

    private final Map<UUID, Integer> bedwarsLevelCache = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> hypixelLevelCache = new HashMap<UUID, Integer>();
    private final Map<UUID, IChatComponent> originalTabNames = new HashMap<UUID, IChatComponent>();
    private long lastCacheClear;
    private long lastPrefetch;
    private boolean inGame;

    public LevelHead() {
        super("Level Head", "Shows Bedwars stars, Hypixel levels, or both.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        bedwarsLevelCache.clear();
        hypixelLevelCache.clear();
        originalTabNames.clear();
        lastCacheClear = System.currentTimeMillis();
        lastPrefetch = 0L;
        inGame = false;
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        restoreTabNames();
        bedwarsLevelCache.clear();
        hypixelLevelCache.clear();
        originalTabNames.clear();
        inGame = false;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2) return;
        String msg = normalize(event.message.getUnformattedText());

        if (!inGame && containsAny(msg, START_PHRASES)) {
            inGame = true;
            preloadLevels();
            refreshTabNames();
            return;
        }

        if (inGame && containsAny(msg, END_PHRASES)) {
            inGame = false;
            restoreTabNames();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!inGame) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) {
            inGame = false;
            restoreTabNames();
            return;
        }

        if (System.currentTimeMillis() - lastPrefetch > 1500L) {
            preloadLevels();
            refreshTabNames();
            lastPrefetch = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.gameSettings.hideGUI) return;
        if (!inGame) return;

        if (System.currentTimeMillis() - lastCacheClear > 30000L) {
            bedwarsLevelCache.clear();
            hypixelLevelCache.clear();
            lastCacheClear = System.currentTimeMillis();
        }

        float pt = event.partialTicks;
        double viewX = mc.getRenderManager().viewerPosX;
        double viewY = mc.getRenderManager().viewerPosY;
        double viewZ = mc.getRenderManager().viewerPosZ;
        int maxDistSq = (int) renderDistance.getValue().doubleValue();
        maxDistSq = maxDistSq * maxDistSq;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!shouldRender(player, mc, maxDistSq)) continue;

            int bedwarsLevel = getBedwarsLevel(player);
            int hypixelLevel = getHypixelLevel(player);
            if (bedwarsLevel < 0 && hypixelLevel < 0) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt - viewX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt - viewY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt - viewZ;

            double offset = 0.3;
            ScoreObjective obj = player.getWorldScoreboard().getObjectiveInDisplaySlot(2);
            if (obj != null && player.getDistanceSqToEntity(mc.thePlayer) < 100) {
                offset *= 2;
            }

            renderTag(player, bedwarsLevel, hypixelLevel, x, y + offset, z, mc);
        }
    }

    private void preloadLevels() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            getBedwarsLevel(player);
            getHypixelLevel(player);
        }
    }

    private void refreshTabNames() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;

            if (!originalTabNames.containsKey(uuid)) {
                originalTabNames.put(uuid, info.getDisplayName());
            }

            String text = buildTabText(uuid, info.getGameProfile().getName());
            setTabDisplayName(info, text == null ? originalTabNames.get(uuid) : new ChatComponentText(text));
        }
    }

    private void restoreTabNames() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;
            if (originalTabNames.containsKey(uuid)) {
                setTabDisplayName(info, originalTabNames.get(uuid));
            }
        }
    }

    private String buildTabText(UUID uuid, String name) {
        int bedwarsLevel = bedwarsLevelCache.containsKey(uuid) ? bedwarsLevelCache.get(uuid) : -1;
        int hypixelLevel = hypixelLevelCache.containsKey(uuid) ? hypixelLevelCache.get(uuid) : -1;

        if (bedwarsLevel < 0 && hypixelLevel < 0) return null;

        String mode = displayMode.getValue();
        if ("Hypixel Level".equals(mode)) {
            return "[" + (hypixelLevel >= 0 ? hypixelLevel : "?") + "] " + name;
        }
        if ("Both".equals(mode)) {
            return "[BW " + (bedwarsLevel >= 0 ? bedwarsLevel + getPrestigeIcon(bedwarsLevel) : "?")
                    + " | LEVEL " + (hypixelLevel >= 0 ? hypixelLevel : "?") + "] " + name;
        }
        return "[BW " + (bedwarsLevel >= 0 ? bedwarsLevel + getPrestigeIcon(bedwarsLevel) : "?") + "] " + name;
    }

    private static void setTabDisplayName(NetworkPlayerInfo info, IChatComponent name) {
        try {
            Method m = NetworkPlayerInfo.class.getDeclaredMethod("setDisplayName", IChatComponent.class);
            m.invoke(info, name);
            return;
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Field f = NetworkPlayerInfo.class.getDeclaredField("displayName");
            f.setAccessible(true);
            f.set(info, name);
        } catch (Exception ignored) {
        }
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

    private boolean shouldRender(EntityPlayer player, Minecraft mc, int maxDistSq) {
        if (player.isInvisibleToPlayer(mc.thePlayer)) return false;
        if (player.isInvisible()) return false;
        if (player.isSneaking()) return false;
        if (player.isPotionActive(Potion.invisibility)) return false;
        if (player.riddenByEntity != null) return false;
        if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return false;
        if (!showOwnLevel.getValue() && player == mc.thePlayer) return false;
        if (player.getDistanceSqToEntity(mc.thePlayer) > maxDistSq) return false;
        return shouldRenderForTeam(player, mc);
    }

    private boolean shouldRenderForTeam(EntityPlayer player, Minecraft mc) {
        if (player == mc.thePlayer) return true;
        Team team = player.getTeam();
        Team myTeam = mc.thePlayer.getTeam();
        if (team != null) {
            Team.EnumVisible vis = team.getNameTagVisibility();
            if (vis == null) return true;
            switch (vis) {
                case NEVER: return false;
                case HIDE_FOR_OTHER_TEAMS:
                    return myTeam == null || team.isSameTeam(myTeam);
                case HIDE_FOR_OWN_TEAM:
                    return myTeam == null || !team.isSameTeam(myTeam);
                default: return true;
            }
        }
        return true;
    }

    private int getBedwarsLevel(EntityPlayer player) {
        UUID uuid = player.getUniqueID();

        HypixelAPI api = AstroClient.INSTANCE.getHypixelAPI();
        if (api.hasKey()) {
            int apiLevel = api.getBedwarsLevel(uuid);
            if (apiLevel >= 0) return apiLevel;
        }

        if (bedwarsLevelCache.containsKey(uuid)) {
            return bedwarsLevelCache.get(uuid);
        }
        int level = parseLevelFromTab(player);
        bedwarsLevelCache.put(uuid, level);
        return level;
    }

    private int getHypixelLevel(EntityPlayer player) {
        UUID uuid = player.getUniqueID();

        HypixelAPI api = AstroClient.INSTANCE.getHypixelAPI();
        if (api.hasKey()) {
            int apiLevel = api.getHypixelLevel(uuid);
            if (apiLevel >= 0) return apiLevel;
        }
        return -1;
    }

    private int parseLevelFromTab(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return -1;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info == null) return -1;

        String raw = null;
        IChatComponent displayName = info.getDisplayName();
        if (displayName != null) {
            raw = displayName.getFormattedText();
        }
        if (raw == null || raw.isEmpty()) {
            raw = info.getGameProfile().getName();
        }
        if (raw == null) return -1;

        raw = raw.replaceAll("\u00a7.", "");

        int from = 0;
        while (from >= 0 && from < raw.length()) {
            int open = raw.indexOf('[', from);
            if (open == -1) return -1;
            int close = raw.indexOf(']', open);
            if (close == -1) return -1;

            String inside = raw.substring(open + 1, close);
            StringBuilder digits = new StringBuilder();
            for (char c : inside.toCharArray()) {
                if (Character.isDigit(c)) {
                    digits.append(c);
                } else if (digits.length() > 0) {
                    break;
                }
            }
            if (digits.length() > 0) {
                try {
                    return Integer.parseInt(digits.toString());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
            from = close + 1;
        }
        return -1;
    }

    private void renderTag(EntityPlayer player, int bedwarsLevel, int hypixelLevel, double x, double y, double z, Minecraft mc) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        String[] parts = buildDisplayParts(bedwarsLevel, hypixelLevel);
        String header = parts[0];
        String footer = parts[1];
        String fullText = header + footer;
        int halfW = fr.getStringWidth(fullText) / 2;

        int headerColor = 0xFFFFFFFF;
        int footerColor = useLevelColors.getValue() ? getDisplayColor(bedwarsLevel, hypixelLevel) : 0xFFFFFF55;

        int xMul = (mc.gameSettings.thirdPersonView == 2) ? -1 : 1;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + player.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX * xMul, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float bgAlpha = bgOpacity.getFloatValue();
        if (showBackground.getValue() && bgAlpha > 0.001F) {
            GlStateManager.disableTexture2D();
            Tessellator tes = Tessellator.getInstance();
            WorldRenderer wr = tes.getWorldRenderer();

            // See-through background pass
            GlStateManager.disableDepth();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-halfW - 2, -1, 0.01D).color(0.0F, 0.0F, 0.0F, bgAlpha * SEE_THROUGH_ALPHA).endVertex();
            wr.pos(-halfW - 2, 8, 0.01D).color(0.0F, 0.0F, 0.0F, bgAlpha * SEE_THROUGH_ALPHA).endVertex();
            wr.pos(halfW + 2, 8, 0.01D).color(0.0F, 0.0F, 0.0F, bgAlpha * SEE_THROUGH_ALPHA).endVertex();
            wr.pos(halfW + 2, -1, 0.01D).color(0.0F, 0.0F, 0.0F, bgAlpha * SEE_THROUGH_ALPHA).endVertex();
            tes.draw();

            // Opaque background pass
            GlStateManager.enableDepth();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-halfW - 2, -1, 0.0D).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            wr.pos(-halfW - 2, 8, 0.0D).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            wr.pos(halfW + 2, 8, 0.0D).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            wr.pos(halfW + 2, -1, 0.0D).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            tes.draw();

            GlStateManager.enableTexture2D();
        }

        // See-through text pass
        GlStateManager.disableDepth();
        drawTagText(fr, header, footer, -halfW, headerColor, footerColor, true);

        // Opaque text pass
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTagText(fr, header, footer, -halfW, headerColor, footerColor, false);

        // Restore
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public String getInlineDisplay(EntityPlayer player) {
        if (!isEnabled() || !inGame || player == null) return null;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        if (!showOwnLevel.getValue() && player == mc.thePlayer) return null;

        int bedwarsLevel = getBedwarsLevel(player);
        int hypixelLevel = getHypixelLevel(player);
        if (bedwarsLevel < 0 && hypixelLevel < 0) return null;

        String mode = displayMode.getValue();
        if ("Hypixel Level".equals(mode)) {
            return "[LEVEL " + (hypixelLevel >= 0 ? hypixelLevel : "?") + "]";
        }
        if ("Both".equals(mode)) {
            return "[BW " + (bedwarsLevel >= 0 ? bedwarsLevel + getPrestigeIcon(bedwarsLevel) : "?")
                    + " | LEVEL " + (hypixelLevel >= 0 ? hypixelLevel : "?") + "]";
        }
        return "[BW " + (bedwarsLevel >= 0 ? bedwarsLevel + getPrestigeIcon(bedwarsLevel) : "?") + "]";
    }

    private String[] buildDisplayParts(int bedwarsLevel, int hypixelLevel) {
        String mode = displayMode.getValue();
        String starText = bedwarsLevel >= 0 ? (bedwarsLevel + getPrestigeIcon(bedwarsLevel)) : "?";
        String hypixelText = hypixelLevel >= 0 ? String.valueOf(hypixelLevel) : "?";

        if ("Hypixel Level".equals(mode)) {
            return new String[] { "Level: ", hypixelText };
        }

        if ("Both".equals(mode)) {
            if (bedwarsLevel < 0) {
                return new String[] { "Level: ", hypixelText };
            }
            if (hypixelLevel < 0) {
                return new String[] { "BW: ", starText };
            }
            return new String[] { "BW: " + starText + " | Level: ", hypixelText };
        }

        return new String[] { "BW: ", starText };
    }

    private int getDisplayColor(int bedwarsLevel, int hypixelLevel) {
        if ("Hypixel Level".equals(displayMode.getValue())) {
            return 0xFF55FFFF;
        }
        if ("Both".equals(displayMode.getValue())) {
            return bedwarsLevel >= 0 ? getPrestigeColor(bedwarsLevel) : 0xFF55FFFF;
        }
        return bedwarsLevel >= 0 ? getPrestigeColor(bedwarsLevel) : 0xFF55FFFF;
    }

    private void drawTagText(FontRenderer fr, String header, String footer,
                              int x, int headerColor, int footerColor, boolean seeThrough) {
        float alpha = seeThrough ? SEE_THROUGH_ALPHA : 1.0F;
        int hColor = applyAlpha(headerColor, alpha);
        int fColor = applyAlpha(footerColor, alpha);
        int headerW = fr.getStringWidth(header);

        if (textShadow.getValue() && !seeThrough) {
            fr.drawStringWithShadow(header, x, 0, hColor);
            fr.drawStringWithShadow(footer, x + headerW, 0, fColor);
        } else {
            fr.drawString(header, x, 0, hColor);
            fr.drawString(footer, x + headerW, 0, fColor);
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        if (a <= 0 && alpha > 0) a = (int)(alpha * 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int getPrestigeColor(int star) {
        int prestige = star / 100;
        switch (prestige) {
            case 0:  return 0xFF808080;
            case 1:  return 0xFFD1D5D8;
            case 2:  return 0xFFFAC51C;
            case 3:  return 0xFF00FFFF;
            case 4:  return 0xFF348017;
            case 5:  return 0xFF008080;
            case 6:  return 0xFFE41B17;
            case 7:  return 0xFFF535AA;
            case 8:  return 0xFF000FFF;
            case 9:  return 0xFF800080;
            case 10: return 0xFFF75D59;
            case 11: return 0xFFD1D5D8;
            case 12: return 0xFFF7DA64;
            case 13: return 0xFF00FFFF;
            case 14: return 0xFF6AFB92;
            case 15: return 0xFF3B9C9C;
            case 16: return 0xFFF75D59;
            case 17: return 0xFFF535AA;
            case 18: return 0xFF0000FF;
            case 19: return 0xFF800080;
            case 20: return 0xFF7C706B;
            default: return 0xFFFFAA00;
        }
    }

    private static String getPrestigeIcon(int star) {
        if (star >= 3100) return "\u2725";
        if (star >= 2100) return "\u269D";
        if (star >= 1100) return "\u272A";
        return "\u272B";
    }
}
