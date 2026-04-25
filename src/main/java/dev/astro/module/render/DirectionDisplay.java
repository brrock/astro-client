package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public final class DirectionDisplay extends HUDModule {

    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionDisplay() {
        super("Direction", "Shows compass direction you are facing.", Category.RENDER, 5, 280);
    }

    @Override
    protected String getText() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return "";
        float yaw = player.rotationYaw % 360.0F;
        if (yaw < 0) yaw += 360.0F;
        int index = Math.round(yaw / 45.0F) & 7;
        return DIRS[index];
    }
}
