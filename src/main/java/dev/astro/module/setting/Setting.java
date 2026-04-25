package dev.astro.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Base class for all module settings.
 * Each setting has a name, description, value, and default value.
 * Subclasses define type-specific serialisation and UI rendering hints.
 */
public abstract class Setting<T> {

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public T getValue()            { return value; }
    public T getDefaultValue()     { return defaultValue; }

    public void setValue(T value) {
        this.value = value;
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** Serialise this setting's value to JSON. */
    public abstract JsonElement toJson();

    /** Deserialise this setting's value from JSON. */
    public abstract void fromJson(JsonElement element);
}
