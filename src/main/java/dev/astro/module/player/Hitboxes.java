package dev.astro.module.player;

import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Clean hitbox outlines — no arrows, no eye-height line.
 * Just a crisp coloured wireframe border around entities.
 */
public final class Hitboxes extends Module {

    private final ColorSetting color = addSetting(
            new ColorSetting("Color", "Hitbox outline colour", 0xFFFFFFFF));
    private final NumberSetting lineWidth = addSetting(
            new NumberSetting("Line Width", "Border thickness", 1.5, 0.5, 4.0, 0.5));
    private final BooleanSetting showPlayers = addSetting(
            new BooleanSetting("Players", "Show hitboxes on players", true));
    private final BooleanSetting showMobs = addSetting(
            new BooleanSetting("Mobs", "Show hitboxes on mobs", true));
    private final BooleanSetting showItems = addSetting(
            new BooleanSetting("Items", "Show hitboxes on dropped items", false));
    private final BooleanSetting showSelf = addSetting(
            new BooleanSetting("Show Self", "Show your own hitbox (3rd person)", false));

    public Hitboxes() {
        super("Hitboxes",
              "Clean entity hitbox outlines.",
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

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.gameSettings.hideGUI) return;

        float pt = event.partialTicks;
        double viewX = mc.getRenderManager().viewerPosX;
        double viewY = mc.getRenderManager().viewerPosY;
        double viewZ = mc.getRenderManager().viewerPosZ;

        int col = color.getValue();
        float r = ((col >> 16) & 0xFF) / 255.0f;
        float g = ((col >> 8) & 0xFF) / 255.0f;
        float b = (col & 0xFF) / 255.0f;
        float a = ((col >> 24) & 0xFF) / 255.0f;
        if (a < 0.01f) a = 1.0f;

        // GL setup
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(lineWidth.getFloatValue());

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!shouldRender(entity, mc)) continue;

            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt - viewX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt - viewY;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt - viewZ;

            float hw = entity.width / 2.0f;
            float h = entity.height;

            drawBox(x - hw, y, z - hw, x + hw, y + h, z + hw, r, g, b, a);
        }

        // Restore
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private boolean shouldRender(Entity entity, Minecraft mc) {
        if (entity.isDead) return false;
        if (entity instanceof EntityArmorStand) return false;

        if (entity == mc.thePlayer) {
            return showSelf.getValue() && mc.gameSettings.thirdPersonView != 0;
        }
        if (entity instanceof EntityPlayer) return showPlayers.getValue();
        if (entity instanceof EntityItem || entity instanceof EntityXPOrb) return showItems.getValue();
        if (entity instanceof EntityArrow) return false;

        return showMobs.getValue();
    }

    /** Draw a wireframe box — 12 edges, no fill. */
    private static void drawBox(double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);

        // Bottom face
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y1, z2);
        GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x1, y1, z1);

        // Top face
        GL11.glVertex3d(x1, y2, z1); GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y2, z1); GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x2, y2, z2); GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y2, z2); GL11.glVertex3d(x1, y2, z1);

        // Vertical edges
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x2, y2, z2);
        GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x1, y2, z2);

        GL11.glEnd();
    }
}
