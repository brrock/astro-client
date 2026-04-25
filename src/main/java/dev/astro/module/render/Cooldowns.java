package dev.astro.module.render;

import dev.astro.event.EventTarget;
import dev.astro.event.events.Render2DEvent;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Shows item cooldown indicators (ender pearl, sword) on screen.
 */
public final class Cooldowns extends HUDModule {

    private final BooleanSetting enderPearl = addSetting(new BooleanSetting("Ender Pearl", "Track ender pearl cooldown", true));
    private final BooleanSetting sword = addSetting(new BooleanSetting("Sword", "Track sword cooldown", true));

    private static final long PEARL_COOLDOWN_MS  = 16000L;  // 16 seconds
    private static final long SWORD_COOLDOWN_MS  = 1600L;   // 1.6 seconds

    private static final int BAR_WIDTH  = 80;
    private static final int BAR_HEIGHT = 4;
    private static final int ROW_HEIGHT = 16;

    private static final int BG_BAR   = new Color(0, 0, 0, 140).getRGB();
    private static final int FG_BAR   = new Color(100, 220, 255, 200).getRGB();
    private static final int BG_PANEL = new Color(0, 0, 0, 120).getRGB();
    private static final int EDGE     = new Color(255, 255, 255, 30).getRGB();

    private final List<CooldownEntry> activeCooldowns = new ArrayList<CooldownEntry>();

    private int  lastPearlCount = -1;
    private long lastAttackTime = 0L;
    private boolean wasAttacking = false;

    public Cooldowns() {
        super("Cooldowns", "Shows item cooldown timers.", Category.RENDER, 5, 500);
    }

    @Override
    protected void onEnable() {
        activeCooldowns.clear();
        lastPearlCount = -1;
        lastAttackTime = 0L;
        wasAttacking = false;
    }

    @Override
    protected void onDisable() {
        activeCooldowns.clear();
    }

    /* ── Tick: detect item usage ──────────────────────────────── */

    @EventTarget
    public void onTick(TickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Ender pearl detection: track pearl count in inventory
        if (enderPearl.getValue()) {
            int pearlCount = countItem(mc, Items.ender_pearl);
            if (lastPearlCount > 0 && pearlCount < lastPearlCount) {
                startCooldown("Ender Pearl", PEARL_COOLDOWN_MS);
            }
            lastPearlCount = pearlCount;
        }

        // Sword swing detection
        if (sword.getValue()) {
            boolean attacking = mc.gameSettings.keyBindAttack.isKeyDown();
            if (attacking && !wasAttacking) {
                ItemStack held = mc.thePlayer.getHeldItem();
                if (held != null && held.getItem() instanceof net.minecraft.item.ItemSword) {
                    startCooldown("Sword", SWORD_COOLDOWN_MS);
                }
            }
            wasAttacking = attacking;
        }

        // Prune expired cooldowns
        long now = System.currentTimeMillis();
        Iterator<CooldownEntry> it = activeCooldowns.iterator();
        while (it.hasNext()) {
            if (now >= it.next().endTime) {
                it.remove();
            }
        }
    }

    private void startCooldown(String name, long durationMs) {
        long now = System.currentTimeMillis();
        // Refresh if same cooldown already active
        for (CooldownEntry entry : activeCooldowns) {
            if (entry.name.equals(name)) {
                entry.startTime = now;
                entry.endTime   = now + durationMs;
                entry.totalMs   = durationMs;
                return;
            }
        }
        activeCooldowns.add(new CooldownEntry(name, now, now + durationMs, durationMs));
    }

    private int countItem(Minecraft mc, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < mc.thePlayer.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    /* ── HUD rendering ────────────────────────────────────────── */

    @Override
    protected String getText() {
        // Not used — we override drawHUD
        return null;
    }

    @Override
    public void drawHUD(FontRenderer fr, int x, int y) {
        if (activeCooldowns.isEmpty()) return;

        long now = System.currentTimeMillis();
        int panelWidth  = BAR_WIDTH + 10;
        int panelHeight = activeCooldowns.size() * ROW_HEIGHT + 6;

        RenderUtil.drawRoundedRect(x, y, x + panelWidth, y + panelHeight, 3, BG_PANEL);
        RenderUtil.drawOutline(x, y, x + panelWidth, y + panelHeight, EDGE);

        int rowY = y + 4;
        for (CooldownEntry entry : activeCooldowns) {
            float remaining = Math.max(0, entry.endTime - now) / 1000.0F;
            float progress  = Math.max(0, 1.0F - (float) (now - entry.startTime) / entry.totalMs);

            String label = entry.name + " " + String.format("%.1fs", remaining);
            fr.drawStringWithShadow(label, x + 5, rowY, 0xFFFFFFFF);

            int barY = rowY + fr.FONT_HEIGHT + 1;
            RenderUtil.drawRect(x + 5, barY, x + 5 + BAR_WIDTH, barY + BAR_HEIGHT, BG_BAR);
            int filled = (int) (BAR_WIDTH * progress);
            if (filled > 0) {
                RenderUtil.drawRect(x + 5, barY, x + 5 + filled, barY + BAR_HEIGHT, FG_BAR);
            }

            rowY += ROW_HEIGHT;
        }
    }

    @Override
    public int getBaseWidth() {
        return BAR_WIDTH + 10;
    }

    @Override
    public int getBaseHeight() {
        return activeCooldowns.isEmpty() ? 0 : activeCooldowns.size() * ROW_HEIGHT + 6;
    }

    /* ── Internal ─────────────────────────────────────────────── */

    private static final class CooldownEntry {
        final String name;
        long startTime;
        long endTime;
        long totalMs;

        CooldownEntry(String name, long startTime, long endTime, long totalMs) {
            this.name      = name;
            this.startTime = startTime;
            this.endTime   = endTime;
            this.totalMs   = totalMs;
        }
    }
}
