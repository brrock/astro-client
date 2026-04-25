package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

public final class PingDisplay extends HUDModule {

    private final BooleanSetting showLabel = addSetting(new BooleanSetting("Show Label", "Show 'ms' suffix", true));
    private final BooleanSetting colorCoded = addSetting(new BooleanSetting("Color Coded", "Color by ping quality", true));

    private int lastPing;

    public PingDisplay() {
        super("Ping Display", "Shows your latency to the server.", Category.RENDER, 5, 200);
    }

    @Override
    protected String getText() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return "N/A";

        NetHandlerPlayClient handler = mc.getNetHandler();
        if (handler == null) return "N/A";

        NetworkPlayerInfo info = handler.getPlayerInfo(player.getUniqueID());
        if (info == null) return "N/A";

        lastPing = info.getResponseTime();
        return showLabel.getValue() ? lastPing + " ms" : String.valueOf(lastPing);
    }

    @Override
    protected int getTextColour() {
        if (!colorCoded.getValue()) return 0xFFFFFFFF;
        if (lastPing < 50) return 0xFF55FF55;
        if (lastPing < 100) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
}
