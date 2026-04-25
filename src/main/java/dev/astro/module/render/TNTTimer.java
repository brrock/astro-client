package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Renders a floating countdown timer above every primed TNT entity in the world.
 */
public final class TNTTimer extends Module {

    private static final float LABEL_SCALE = 0.026666668F;

    private final NumberSetting maxDistance = addSetting(new NumberSetting("Max Distance", "Max render distance in blocks", 64.0, 16.0, 128.0, 8.0));
    private final NumberSetting lineWidth = addSetting(new NumberSetting("Line Width", "Outline line width", 2.0, 1.0, 5.0, 0.5));
    private final BooleanSetting background = addSetting(new BooleanSetting("Background", "Show background behind timer", true));

    public TNTTimer() {
        super("TNT Timer", "Shows a countdown above TNT entities.", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        float partial = event.partialTicks;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityTNTPrimed)) continue;
            EntityTNTPrimed tnt = (EntityTNTPrimed) entity;

            float seconds = tnt.fuse / 20.0F;
            String label = String.format("%.1fs", seconds);
            int colour = seconds <= 1.0F ? 0xFFFF5555 : 0xFFFFFF55; // red when ≤1s

            double x = interpolate(tnt.prevPosX, tnt.posX, partial);
            double y = interpolate(tnt.prevPosY, tnt.posY, partial);
            double z = interpolate(tnt.prevPosZ, tnt.posZ, partial);

            renderLabel(mc, label, colour, x, y + tnt.height + 0.5, z);
        }
    }

    private void renderLabel(Minecraft mc, String text, int colour,
                             double x, double y, double z) {
        RenderManager rm = mc.getRenderManager();
        FontRenderer  fr = mc.fontRendererObj;

        double dx = x - rm.viewerPosX;
        double dy = y - rm.viewerPosY;
        double dz = z - rm.viewerPosZ;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > maxDistance.getValue()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) dx, (float) dy, (float) dz);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        int halfWidth = fr.getStringWidth(text) / 2;

        // Semi-transparent background
        if (background.getValue()) {
            GlStateManager.disableTexture2D();
            net.minecraft.client.renderer.Tessellator tes =
                    net.minecraft.client.renderer.Tessellator.getInstance();
            net.minecraft.client.renderer.WorldRenderer wr = tes.getWorldRenderer();
            wr.begin(GL11.GL_QUADS,
                    net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-halfWidth - 2, -1, 0).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            wr.pos(-halfWidth - 2, fr.FONT_HEIGHT + 1, 0).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            wr.pos(halfWidth + 2, fr.FONT_HEIGHT + 1, 0).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            wr.pos(halfWidth + 2, -1, 0).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            tes.draw();
            GlStateManager.enableTexture2D();
        }

        fr.drawStringWithShadow(text, -halfWidth, 0, colour);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static double interpolate(double prev, double current, float partial) {
        return prev + (current - prev) * partial;
    }
}
