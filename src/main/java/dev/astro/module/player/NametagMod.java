package dev.astro.module.player;

import dev.astro.AstroClient;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.render.LevelHead;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Customizes player nametag appearance.
 * Depth test stays ENABLED — nametags are NOT visible through walls.
 * Replaces vanilla rendering with configurable background, shadow,
 * scale, and optional health display.
 */
public final class NametagMod extends Module {

    private final BooleanSetting textShadow = addSetting(
            new BooleanSetting("Text Shadow", "Draw shadow behind text", true));
    private final NumberSetting backgroundOpacity = addSetting(
            new NumberSetting("Background Opacity", "0 = no background", 0.25, 0.0, 1.0, 0.05));
    private final ColorSetting backgroundColor = addSetting(
            new ColorSetting("Background Color", "Background tint colour", 0xFF000000));
    private final NumberSetting nameScale = addSetting(
            new NumberSetting("Scale", "Nametag size multiplier", 1.0, 0.5, 3.0, 0.1));
    private final BooleanSetting showHealth = addSetting(
            new BooleanSetting("Show Health", "Display health beside name", true));

    public NametagMod() {
        super("Nametags",
              "Customizes nametag look \u2014 not visible through walls.",
              Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public void onRenderNamePre(RenderLivingEvent.Specials.Pre event) {
        if (!(event.entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.entity;
        Minecraft mc = Minecraft.getMinecraft();

        // Don't replace own nametag in first-person
        if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;

        // Cancel vanilla nametag, render our own instead
        event.setCanceled(true);
        renderCustomNametag(player, event.x, event.y, event.z);
    }

    /* ── Custom nametag renderer ─────────────────────────────────── */

    private void renderCustomNametag(EntityPlayer player, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        double distSq = player.getDistanceSqToEntity(mc.thePlayer);
        if (distSq > 64 * 64) return;

        // Build display string
        String name = player.getDisplayName().getFormattedText();

        LevelHead levelHead = AstroClient.INSTANCE.getModuleManager().getByClass(LevelHead.class);
        if (levelHead != null) {
            String levelText = levelHead.getInlineDisplay(player);
            if (levelText != null && !levelText.isEmpty()) {
                name = "\u00a7e" + levelText + " \u00a7r" + name;
            }
        }

        // AstroClient user icon
        if (AstroClient.INSTANCE.getAstroUsers() != null
                && AstroClient.INSTANCE.getAstroUsers().isAstroUser(player.getUniqueID())) {
            name = AstroClient.INSTANCE.getAstroUsers().getMarkerPrefix() + name;
        }

        if (showHealth.getValue()) {
            float totalHealth = player.getHealth() + player.getAbsorptionAmount();
            int hp = (int) Math.ceil(totalHealth);
            name = name + " \u00a77|\u00a7r " + getHealthColor(totalHealth) + hp + "\u2764";
        }

        float scaleMul = nameScale.getFloatValue();
        float f1 = 0.016666668F * 1.6F * scaleMul;
        int halfW = fr.getStringWidth(name) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + player.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        // Depth test stays ENABLED — nametags hidden behind blocks (fair play)
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // ── Background ──
        float bgAlpha = backgroundOpacity.getFloatValue();
        if (bgAlpha > 0.001F) {
            int bgCol = backgroundColor.getValue();
            float bR = ((bgCol >> 16) & 0xFF) / 255.0F;
            float bG = ((bgCol >> 8) & 0xFF) / 255.0F;
            float bB = (bgCol & 0xFF) / 255.0F;

            Tessellator tes = Tessellator.getInstance();
            WorldRenderer wr = tes.getWorldRenderer();
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha(); // Prevent alpha test from discarding semi-transparent fragments
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-halfW - 1, -1, 0.0D).color(bR, bG, bB, bgAlpha).endVertex();
            wr.pos(-halfW - 1,  8, 0.0D).color(bR, bG, bB, bgAlpha).endVertex();
            wr.pos( halfW + 1,  8, 0.0D).color(bR, bG, bB, bgAlpha).endVertex();
            wr.pos( halfW + 1, -1, 0.0D).color(bR, bG, bB, bgAlpha).endVertex();
            tes.draw();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
        }

        // ── Text ──
        if (textShadow.getValue()) {
            fr.drawStringWithShadow(name, -halfW, 0, 0xFFFFFFFF);
        } else {
            fr.drawString(name, -halfW, 0, 0xFFFFFFFF);
        }

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static String getHealthColor(float health) {
        if (health <= 6.0F) return "\u00a74";
        if (health <= 12.0F) return "\u00a76";
        return "\u00a7a";
    }
}
