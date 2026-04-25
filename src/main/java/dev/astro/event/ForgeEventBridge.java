package dev.astro.event;

import dev.astro.AstroClient;
import dev.astro.event.events.KeyEvent;
import dev.astro.event.events.MouseClickEvent;
import dev.astro.event.events.Render2DEvent;
import dev.astro.event.events.TickEvent;
import dev.astro.ui.ClickGUI;
import dev.astro.ui.CustomMainMenu;
import dev.astro.ui.HUDEditor;
import dev.astro.ui.LoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.input.Keyboard;

/**
 * Bridges Forge events → AstroClient's internal EventBus.
 * Registered on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
 */
public final class ForgeEventBridge {

    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;
        AstroClient.INSTANCE.getEventBus().post(new TickEvent());
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        AstroClient.INSTANCE.getEventBus().post(
                new Render2DEvent(event.partialTicks));
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();

        // Dispatch to our internal EventBus (module key-binds)
        AstroClient.INSTANCE.getEventBus().post(new KeyEvent(key));

        // ClickGUI
        if (AstroClient.keyOpenGui.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof ClickGUI) {
                mc.displayGuiScreen(null);
            } else {
                mc.displayGuiScreen(new ClickGUI());
            }
        }

        // HUD Editor
        if (AstroClient.keyHudEditor.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            try {
                if (mc.currentScreen instanceof HUDEditor) {
                    mc.displayGuiScreen(null);
                } else {
                    mc.displayGuiScreen(new HUDEditor());
                }
            } catch (Throwable t) {
                t.printStackTrace();
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(
                            EnumChatFormatting.AQUA + "[AstroClient] " +
                            EnumChatFormatting.RED + "Failed to open HUD Editor. See launcher log."));
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        int button = org.lwjgl.input.Mouse.getEventButton();
        if (button >= 0 && org.lwjgl.input.Mouse.getEventButtonState()) {
            AstroClient.INSTANCE.getEventBus().post(new MouseClickEvent(button));
        }
    }

    /** Whether the initial loading splash has been shown. */
    private boolean splashShown = false;

    /** Replace the vanilla title screen with our custom one (loading splash on first open). */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu) {
            if (!splashShown) {
                splashShown = true;
                event.gui = new LoadingScreen();
            } else {
                event.gui = new CustomMainMenu();
            }
        }
    }
}
