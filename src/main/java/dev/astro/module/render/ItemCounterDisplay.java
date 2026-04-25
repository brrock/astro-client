package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

public final class ItemCounterDisplay extends HUDModule {

    public ItemCounterDisplay() {
        super("Item Counter", "Shows the count and name of your held item.", Category.RENDER, 5, 340);
    }

    @Override
    protected String getText() {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return "";
        ItemStack held = player.getHeldItem();
        if (held == null) return "";
        return "x" + held.stackSize + " " + held.getDisplayName();
    }
}
