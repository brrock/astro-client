package dev.astro.util;

import com.google.gson.*;
import dev.astro.AstroClient;
import dev.astro.module.HUDModule;
import dev.astro.module.Module;
import dev.astro.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.List;

/**
 * Saves and loads all module state to a JSON file in .minecraft/astroclient/config.json.
 *
 * Persisted per module:
 *   - enabled state
 *   - key bind
 *   - visible flag
 *   - renderX / renderY (for HUDModules)
 *   - custom options (modules can override saveCustom / loadCustom)
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private File configDir;
    private File configFile;

    /** Suppresses auto-save in Module.setEnabled() during config load. */
    private boolean loading;

    public void init() {
        configDir  = new File(Minecraft.getMinecraft().mcDataDir, "astroclient");
        configFile = new File(configDir, "config.json");

        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    /** True while loading config — modules should not auto-save during this time. */
    public boolean isLoading() { return loading; }

    /** Load config from disk. Call after all modules are registered. */
    public void load() {
        if (!configFile.exists()) return;

        loading = true;
        boolean needsSave = false;
        try (Reader reader = new FileReader(configFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

            for (Module mod : AstroClient.INSTANCE.getModuleManager().getModules()) {
                if (!root.has(mod.getName())) continue;
                JsonObject obj = root.getAsJsonObject(mod.getName());

                // Load keybind and visibility first
                if (obj.has("keyBind")) {
                    int keyBind = obj.get("keyBind").getAsInt();
                    mod.setKeyBind(keyBind);
                    if (mod.getKeyBind() != keyBind) {
                        needsSave = true;
                    }
                }
                if (obj.has("visible")) {
                    mod.setVisible(obj.get("visible").getAsBoolean());
                }

                // Load HUD position before enable
                if (mod instanceof HUDModule) {
                    HUDModule hud = (HUDModule) mod;
                    if (obj.has("renderX")) hud.setRenderX(obj.get("renderX").getAsInt());
                    if (obj.has("renderY")) hud.setRenderY(obj.get("renderY").getAsInt());
                }

                // Load settings BEFORE enabling so onEnable sees correct values
                if (obj.has("settings")) {
                    JsonObject settingsObj = obj.getAsJsonObject("settings");
                    for (Setting<?> setting : mod.getSettings()) {
                        if (settingsObj.has(setting.getName())) {
                            setting.fromJson(settingsObj.get(setting.getName()));
                        }
                    }
                }

                // Legacy Configurable support
                if (mod instanceof Configurable) {
                    ((Configurable) mod).loadConfig(obj);
                }

                // Now enable if saved as enabled
                if (mod.shouldPersistEnabledState()
                        && obj.has("enabled")
                        && obj.get("enabled").getAsBoolean()) {
                    mod.setEnabled(true);
                }
            }

            System.out.println("[AstroClient] Config loaded.");
        } catch (Exception e) {
            System.err.println("[AstroClient] Failed to load config: " + e.getMessage());
        } finally {
            loading = false;
        }

        if (needsSave) {
            save();
        }
    }

    /** Save all module state to disk. */
    public void save() {
        JsonObject root = new JsonObject();

        for (Module mod : AstroClient.INSTANCE.getModuleManager().getModules()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", mod.shouldPersistEnabledState() && mod.isEnabled());
            obj.addProperty("keyBind", mod.getKeyBind());
            obj.addProperty("visible", mod.isVisible());

            if (mod instanceof HUDModule) {
                HUDModule hud = (HUDModule) mod;
                obj.addProperty("renderX", hud.getRenderX());
                obj.addProperty("renderY", hud.getRenderY());
            }

            // Auto-save typed settings
            if (mod.hasSettings()) {
                JsonObject settingsObj = new JsonObject();
                for (Setting<?> setting : mod.getSettings()) {
                    settingsObj.add(setting.getName(), setting.toJson());
                }
                obj.add("settings", settingsObj);
            }

            // Legacy Configurable support
            if (mod instanceof Configurable) {
                ((Configurable) mod).saveConfig(obj);
            }

            root.add(mod.getName(), obj);
        }

        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            System.err.println("[AstroClient] Failed to save config: " + e.getMessage());
        }
    }

    /** Interface for modules with extra settings. */
    public interface Configurable {
        void saveConfig(JsonObject obj);
        void loadConfig(JsonObject obj);
    }

}
