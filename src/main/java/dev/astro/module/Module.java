package dev.astro.module;

import dev.astro.AstroClient;
import dev.astro.module.setting.Setting;
import dev.astro.util.ConfigManager;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class every module extends.
 *
 * Lifecycle:
 *   toggle() → onEnable() / onDisable()
 *   onEnable  registers  this on the EventBus automatically.
 *   onDisable unregisters this from the EventBus automatically.
 *
 * Settings:
 *   Modules register typed {@link Setting} objects in their constructor.
 *   These are auto-serialised by the ConfigManager and rendered in the ClickGUI.
 */
public abstract class Module {

    public enum ActivationMode {
        TOGGLE,
        HOLD
    }

    private final String   name;
    private final String   description;
    private final Category category;

    private boolean enabled;
    private int     keyBind;
    private boolean visible = true;

    /** Typed settings registered by subclasses. */
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected Module(String name, String description, Category category, int keyBind) {
        this.name        = name;
        this.description = description;
        this.category    = category;
        this.keyBind     = keyBind;
    }

    protected Module(String name, String description, Category category) {
        this(name, description, category, Keyboard.KEY_NONE);
    }

    /** Register a setting. Call in subclass constructors. */
    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    /** All registered settings (unmodifiable). */
    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /** Whether this module has any configurable settings. */
    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public void toggle() { setEnabled(!enabled); }

    public ActivationMode getActivationMode() {
        return ActivationMode.TOGGLE;
    }

    public boolean shouldPersistEnabledState() {
        return getActivationMode() == ActivationMode.TOGGLE;
    }

    public boolean allowsKeyBind() { return false; }

    public void setEnabled(boolean state) {
        if (this.enabled == state) return;

        this.enabled = state;

        if (state) {
            onEnable();
            AstroClient.INSTANCE.getEventBus().register(this);
        } else {
            AstroClient.INSTANCE.getEventBus().unregister(this);
            onDisable();
        }

        // Auto-save config on every toggle (but not during initial config load)
        ConfigManager cfg = AstroClient.INSTANCE.getConfigManager();
        if (cfg != null && !cfg.isLoading() && shouldPersistEnabledState()) {
            cfg.save();
        }
    }

    protected void onEnable()  {}
    protected void onDisable() {}

    public String   getName()        { return name; }
    public String   getDescription() { return description; }
    public Category getCategory()    { return category; }
    public boolean  isEnabled()      { return enabled; }
    public int      getKeyBind()     { return keyBind; }
    public boolean  isVisible()      { return visible; }

    public void setKeyBind(int keyBind) {
        this.keyBind = allowsKeyBind() ? keyBind : Keyboard.KEY_NONE;
    }
    public void setVisible(boolean v)   { this.visible = v; }
}
