package dev.astro.module.movement;

import dev.astro.event.EventTarget;
import dev.astro.event.events.TickEvent;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

/**
 * ToggleSneak — tap the sneak key once to start sneaking,
 * tap again to stop.  No need to hold the key.
 */
public final class ToggleSneak extends Module {

    private final BooleanSetting flyBoost = addSetting(new BooleanSetting("Fly Boost", "Boost fly speed when sneaking", false));
    private final NumberSetting flyBoostAmount = addSetting(new NumberSetting("Fly Boost Amount", "Fly speed multiplier", 2.0, 1.0, 5.0, 0.5));

    private boolean sneakToggled;
    private boolean keyWasDown;

    public ToggleSneak() {
        super("ToggleSneak",
              "Toggle sneak without holding the key.",
              Category.MOVEMENT);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;

        KeyBinding sneakBind = Minecraft.getMinecraft().gameSettings.keyBindSneak;
        boolean keyDown = sneakBind.isKeyDown();

        // Detect rising edge (key just pressed) to flip the toggle
        if (keyDown && !keyWasDown) {
            sneakToggled = !sneakToggled;
        }
        keyWasDown = keyDown;

        // Force sneak state: hold it down when toggled ON, release when OFF
        if (sneakToggled && !keyDown) {
            KeyBinding.setKeyBindState(sneakBind.getKeyCode(), true);
        } else if (!sneakToggled && !keyDown) {
            KeyBinding.setKeyBindState(sneakBind.getKeyCode(), false);
        }

        // Fly boost while sneaking and flying
        if (flyBoost.getValue() && player.capabilities.isFlying && sneakToggled) {
            float boost = flyBoostAmount.getFloatValue();
            player.motionX *= boost;
            player.motionZ *= boost;
        }
    }

    @Override
    protected void onEnable() {
        sneakToggled = false;
        keyWasDown = false;
    }

    @Override
    protected void onDisable() {
        sneakToggled = false;
        keyWasDown = false;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            KeyBinding sneakBind = Minecraft.getMinecraft().gameSettings.keyBindSneak;
            KeyBinding.setKeyBindState(sneakBind.getKeyCode(), false);
        }
    }
}
