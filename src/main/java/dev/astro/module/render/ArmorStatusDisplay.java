package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.awt.Color;

public final class ArmorStatusDisplay extends HUDModule {

    private final BooleanSetting showDurability = addSetting(new BooleanSetting("Show Durability", "Show durability numbers", true));
    private final ModeSetting durabilityMode = addSetting(new ModeSetting("Durability Mode", "How to show durability", "Remaining", "Remaining", "Percentage", "Damage"));

    private static final int BG   = new Color(0, 0, 0, 120).getRGB();
    private static final int EDGE = new Color(255, 255, 255, 30).getRGB();

    public ArmorStatusDisplay() {
        super("Armor Status", "Displays equipped armor and durability.", Category.RENDER, 5, 200);
    }

    @Override
    protected String getText() {
        return null;
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        ItemStack[] armor = mc.thePlayer.inventory.armorInventory;

        // Count non-null pieces to size the background
        int count = 0;
        for (ItemStack piece : armor) {
            if (piece != null) count++;
        }
        if (count == 0) return;

        int w = getBaseWidth();
        int h = count * 17;
        RenderUtil.drawRoundedRect(x, y, x + w, y + h, 3, BG);
        RenderUtil.drawOutline(x, y, x + w, y + h, EDGE);

        // Set up GL state for item rendering in a 2D HUD context.
        // renderItemIntoGUI needs depth test + proper lighting to display items.
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();

        int slot = 0;
        // Render from helmet (index 3) down to boots (index 0)
        for (int i = 3; i >= 0; i--) {
            ItemStack piece = armor[i];
            if (piece == null) continue;

            int slotY = y + slot * 17;
            mc.getRenderItem().renderItemIntoGUI(piece, x + 1, slotY);

            int maxDmg = piece.getMaxDamage();
            if (maxDmg > 0 && showDurability.getValue()) {
                int remaining = maxDmg - piece.getItemDamage();
                String durText;
                String mode = durabilityMode.getValue();
                if ("Percentage".equals(mode)) {
                    int pct = (int) ((remaining / (float) maxDmg) * 100);
                    durText = pct + "%";
                } else if ("Damage".equals(mode)) {
                    durText = String.valueOf(piece.getItemDamage());
                } else {
                    durText = String.valueOf(remaining);
                }
                int colour = remaining > maxDmg / 2 ? 0xFF55FF55 : 0xFFFF5555;
                GlStateManager.enableTexture2D();
                fr.drawStringWithShadow(durText, x + 19, slotY + 4, colour);
            }
            slot++;
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public int getBaseWidth() {
        return 60;
    }

    @Override
    public int getBaseHeight() {
        return 68;
    }
}
