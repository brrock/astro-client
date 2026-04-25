package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;

public final class CoordinatesDisplay extends HUDModule {

    private final NumberSetting decimals = addSetting(new NumberSetting("Decimals", "Decimal places", 1.0, 0.0, 3.0, 1.0));
    private final BooleanSetting showBiome = addSetting(new BooleanSetting("Show Biome", "Show current biome", false));
    private final BooleanSetting showDirection = addSetting(new BooleanSetting("Show Direction", "Show facing direction", false));
    private final ColorSetting textColor = addSetting(new ColorSetting("Text Color", "Coordinates color", 0xFFFFFFFF));

    public CoordinatesDisplay() {
        super("Coordinates", "Shows your XYZ coordinates.", Category.RENDER, 5, 160);
    }

    @Override
    protected String getText() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return "";

        int dec = decimals.getIntValue();
        String fmt = "%." + dec + "f";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("X: " + fmt + "  Y: " + fmt + "  Z: " + fmt, player.posX, player.posY, player.posZ));

        if (showDirection.getValue()) {
            String[] dirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
            int idx = MathHelper.floor_double((double) (player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7;
            sb.append("  ").append(dirs[idx]);
        }

        if (showBiome.getValue() && mc.theWorld != null) {
            BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
            BiomeGenBase biome = mc.theWorld.getBiomeGenForCoords(pos);
            if (biome != null) {
                sb.append("  ").append(biome.biomeName);
            }
        }

        return sb.toString();
    }

    @Override
    protected int getTextColour() {
        return textColor.getValue();
    }
}
