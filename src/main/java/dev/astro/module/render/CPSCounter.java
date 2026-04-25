package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.MouseClickEvent;
import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.LinkedList;

/**
 * CPS Counter — event-driven clicks-per-second display.
 * Extends HUDModule so it is draggable in the HUD Editor.
 */
public final class CPSCounter extends HUDModule {

    private final BooleanSetting rightClick = addSetting(new BooleanSetting("Right Click", "Show right click CPS", true));
    private final ColorSetting textColor = addSetting(new ColorSetting("Text Color", "CPS text color", 0xFFFFFFFF));

    private final LinkedList<Long> leftClicks  = new LinkedList<Long>();
    private final LinkedList<Long> rightClicks = new LinkedList<Long>();

    public CPSCounter() {
        super("CPS Counter",
              "Shows clicks per second for LMB and RMB.",
              Category.RENDER,
              5, 140);
    }

    @EventTarget
    public void onMouseClick(MouseClickEvent event) {
        long now = System.currentTimeMillis();
        if (event.getButton() == 0) leftClicks.add(now);
        if (event.getButton() == 1) rightClicks.add(now);
    }

    @Override
    protected String getText() {
        long now = System.currentTimeMillis();
        while (!leftClicks.isEmpty()  && now - leftClicks.peek()  > 1000) leftClicks.poll();
        while (!rightClicks.isEmpty() && now - rightClicks.peek() > 1000) rightClicks.poll();

        if (rightClick.getValue()) {
            return leftClicks.size() + " | " + rightClicks.size() + " CPS";
        }
        return leftClicks.size() + " CPS";
    }

    @Override
    protected int getTextColour() {
        return textColor.getValue();
    }

    @Override
    protected void onDisable() {
        leftClicks.clear();
        rightClicks.clear();
    }

    @Override
    public int getBaseWidth() {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth("00 | 00 CPS") + 10;
    }
}
