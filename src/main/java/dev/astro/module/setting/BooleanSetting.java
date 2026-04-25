package dev.astro.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A boolean on/off setting, rendered as a toggle in the ClickGUI.
 */
public final class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsBoolean());
        }
    }

    /** Convenience toggle. */
    public void toggle() {
        setValue(!getValue());
    }
}
