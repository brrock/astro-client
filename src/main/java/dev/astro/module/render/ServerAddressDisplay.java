package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Displays the current server IP with the server's favicon icon.
 * The favicon is decoded from the server's base64 icon data.
 */
public final class ServerAddressDisplay extends HUDModule {

    private static final int BG   = new Color(0, 0, 0, 120).getRGB();
    private static final int EDGE = new Color(255, 255, 255, 30).getRGB();
    private static final int ICON_SIZE = 16;
    private static final int PAD = 5;

    private ResourceLocation cachedIcon;
    private String cachedIconServer;

    public ServerAddressDisplay() {
        super("Server Address", "Shows the current server IP with icon.", Category.RENDER, 5, 240);
    }

    @Override
    protected String getText() {
        ServerData data = Minecraft.getMinecraft().getCurrentServerData();
        if (data == null) return "Singleplayer";
        return data.serverIP;
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        ServerData data = Minecraft.getMinecraft().getCurrentServerData();
        String text = (data == null) ? "Singleplayer" : data.serverIP;
        boolean hasIcon = tryLoadIcon(data);

        int textX = x + PAD;
        if (hasIcon) {
            textX += ICON_SIZE + 4;
        }
        int totalW = (textX - x) + fr.getStringWidth(text) + PAD;
        int h = Math.max(fr.FONT_HEIGHT + 8, ICON_SIZE + 6);

        RenderUtil.drawRoundedRect(x, y, x + totalW, y + h, 3, BG);
        RenderUtil.drawOutline(x, y, x + totalW, y + h, EDGE);

        // Icon
        if (hasIcon && cachedIcon != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(cachedIcon);
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            drawModalRectWithCustomSizedTexture(x + PAD, y + (h - ICON_SIZE) / 2,
                    0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            GlStateManager.disableBlend();
        }

        // Text
        fr.drawStringWithShadow(text, textX, y + (h - fr.FONT_HEIGHT) / 2 + 1, 0xFFFFFFFF);
    }

    @Override
    public int getBaseWidth() {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        String text = getText();
        ServerData data = Minecraft.getMinecraft().getCurrentServerData();
        int w = PAD + fr.getStringWidth(text) + PAD;
        if (data != null && data.getBase64EncodedIconData() != null) {
            w += ICON_SIZE + 4;
        }
        return w;
    }

    @Override
    public int getBaseHeight() {
        return Math.max(Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT + 8, ICON_SIZE + 6);
    }

    /**
     * Loads/caches the server favicon from the ServerData base64 icon.
     * Returns true if an icon is available.
     */
    private boolean tryLoadIcon(ServerData data) {
        if (data == null) {
            cachedIcon = null;
            cachedIconServer = null;
            return false;
        }

        String iconData = data.getBase64EncodedIconData();
        if (iconData == null || iconData.isEmpty()) {
            cachedIcon = null;
            cachedIconServer = null;
            return false;
        }

        // Only re-decode if server changed
        String serverKey = data.serverIP;
        if (serverKey.equals(cachedIconServer) && cachedIcon != null) {
            return true;
        }

        try {
            byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(iconData);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(decoded));
            if (img != null) {
                DynamicTexture dynTex = new DynamicTexture(img);
                cachedIcon = Minecraft.getMinecraft().getTextureManager()
                        .getDynamicTextureLocation("astro_server_icon", dynTex);
                cachedIconServer = serverKey;
                return true;
            }
        } catch (Exception e) {
            // Silently fail — icon just won't show
        }

        cachedIcon = null;
        cachedIconServer = null;
        return false;
    }

    /** Draws a texture with custom size (same as Gui.drawModalRectWithCustomSizedTexture). */
    private static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v,
                                                             int w, int h, float texW, float texH) {
        float f = 1.0F / texW;
        float g = 1.0F / texH;
        net.minecraft.client.renderer.Tessellator tes = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tes.getWorldRenderer();
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + h, 0).tex(u * f, (v + h) * g).endVertex();
        wr.pos(x + w, y + h, 0).tex((u + w) * f, (v + h) * g).endVertex();
        wr.pos(x + w, y, 0).tex((u + w) * f, v * g).endVertex();
        wr.pos(x, y, 0).tex(u * f, v * g).endVertex();
        tes.draw();
    }
}
