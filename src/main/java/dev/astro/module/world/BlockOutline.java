package dev.astro.module.world;

import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * BlockOutline — replaces the default block selection outline with a custom colour.
 * Registers on Forge's EVENT_BUS to intercept DrawBlockHighlightEvent.
 */
public final class BlockOutline extends Module {

    private final ColorSetting outlineColor = addSetting(new ColorSetting("Outline Color", "Selection outline color", 0xCC00B3FF));
    private final NumberSetting lineWidthSetting = addSetting(new NumberSetting("Line Width", "Outline thickness", 2.0, 1.0, 5.0, 0.5));

    public BlockOutline() {
        super("BlockOutline",
              "Changes the block selection outline colour.",
              Category.WORLD);
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
    public void onDrawHighlight(DrawBlockHighlightEvent event) {
        if (event.target == null) return;
        if (event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        event.setCanceled(true);

        Minecraft mc = Minecraft.getMinecraft();
        BlockPos pos = event.target.getBlockPos();
        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();

        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos);
        if (bb == null) return;

        EntityPlayer player = event.player;
        float pt = event.partialTicks;
        double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        bb = bb.offset(-interpX, -interpY, -interpZ).expand(0.002, 0.002, 0.002);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float r = outlineColor.getRed() / 255.0F;
        float g = outlineColor.getGreen() / 255.0F;
        float b = outlineColor.getBlue() / 255.0F;
        float a = outlineColor.getAlpha() / 255.0F;
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidthSetting.getFloatValue());
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        RenderGlobal.drawSelectionBoundingBox(bb);

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
