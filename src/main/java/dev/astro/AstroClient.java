package dev.astro;

import dev.astro.command.ApiKeyCommand;
import dev.astro.event.EventBus;
import dev.astro.module.ModuleManager;
import dev.astro.ui.HUD;
import dev.astro.util.AstroUsers;
import dev.astro.util.ConfigManager;
import dev.astro.util.HypixelAPI;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileWriter;

/**
 * AstroClient — a Forge 1.8.9 mod for Bedwars with a modular HUD
 * and clean ClickGUI.
 */
@Mod(modid = AstroClient.MOD_ID, name = AstroClient.NAME, version = AstroClient.VERSION)
public class AstroClient {

    public static final String MOD_ID  = "astroclient";
    public static final String NAME    = "AstroClient";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    public static AstroClient INSTANCE;

    private EventBus      eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private HypixelAPI    hypixelAPI;
    private AstroUsers    astroUsers;
    private HUD           hud;

    /** Forge-registered keybinding for the ClickGUI (RIGHT_SHIFT). */
    public static KeyBinding keyOpenGui;

    /** Forge-registered keybinding for the HUD Editor. */
    public static KeyBinding keyHudEditor;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Write AstroClient-branded Forge splash for next launch
        writeSplashProperties(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("[" + NAME + "] Initialising v" + VERSION + "...");

        eventBus      = new EventBus();
        moduleManager = new ModuleManager();
        configManager = new ConfigManager();
        hypixelAPI    = new HypixelAPI();
        hud           = new HUD();

        eventBus.register(hud);
        moduleManager.init();

        // Persistence
        configManager.init();
        configManager.load();

        // Hypixel API (reads key from astroclient/apikey.txt)
        hypixelAPI.init();

        // AstroClient user detection
        astroUsers = new AstroUsers();
        astroUsers.init();

        // Register commands
        ClientCommandHandler.instance.registerCommand(new ApiKeyCommand());

        // Register keybinds through Forge's system
        keyOpenGui = new KeyBinding("Open AstroClient GUI", Keyboard.KEY_RSHIFT, "AstroClient");
        keyHudEditor = new KeyBinding("HUD Editor", Keyboard.KEY_H, "AstroClient");
        ClientRegistry.registerKeyBinding(keyOpenGui);
        ClientRegistry.registerKeyBinding(keyHudEditor);

        // Register the Forge-event bridge so our custom events fire
        MinecraftForge.EVENT_BUS.register(new dev.astro.event.ForgeEventBridge());

        System.out.println("[" + NAME + "] Loaded " +
                moduleManager.getModules().size() + " modules.");
    }

    public EventBus      getEventBus()      { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public HypixelAPI    getHypixelAPI()    { return hypixelAPI; }
    public AstroUsers    getAstroUsers()    { return astroUsers; }
    public HUD           getHud()           { return hud; }

    /**
     * Writes config/splash.properties with AstroClient-branded colours.
     * Takes effect on the NEXT launch (Forge reads it before mods init).
     */
    private void writeSplashProperties(File configDir) {
        File splash = new File(configDir, "splash.properties");
        if (splash.exists()) return; // Don't overwrite user customizations

        try {
            if (!configDir.exists()) configDir.mkdirs();
            FileWriter fw = new FileWriter(splash);
            fw.write("# AstroClient branded Forge loading screen\n");
            fw.write("background=0x08080E\n");
            fw.write("font=0xFFFFFF\n");
            fw.write("barBackground=0x1A1A24\n");
            fw.write("barBorder=0x333344\n");
            fw.write("barColor=0x00C8FF\n");
            fw.write("logoOffset=0\n");
            fw.write("rotate=true\n");
            fw.close();
            System.out.println("[" + NAME + "] Wrote splash.properties (takes effect next launch)");
        } catch (Exception e) {
            // Non-critical, ignore
        }
    }
}
