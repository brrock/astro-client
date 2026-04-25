package dev.astro.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A multi-choice setting that cycles through a list of string modes.
 * Rendered as a clickable label in the ClickGUI.
 */
public final class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Collections.unmodifiableList(Arrays.asList(modes));
    }

    public List<String> getModes() { return modes; }

    /** Cycles to the next mode in the list. */
    public void cycle() {
        int idx = modes.indexOf(getValue());
        int next = (idx + 1) % modes.size();
        setValue(modes.get(next));
    }

    /** Cycles to the previous mode. */
    public void cycleBack() {
        int idx = modes.indexOf(getValue());
        int prev = (idx - 1 + modes.size()) % modes.size();
        setValue(modes.get(prev));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            String val = element.getAsString();
            if (modes.contains(val)) {
                setValue(val);
            }
        }
    }
}
