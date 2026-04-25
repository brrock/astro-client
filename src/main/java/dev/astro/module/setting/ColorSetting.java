package dev.astro.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An ARGB colour setting.
 * Value is stored as a 32-bit ARGB integer (e.g. 0xFFFF0000 = opaque red).
 * Rendered as a colour swatch in the ClickGUI.
 */
public final class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, String description, int defaultValue) {
        super(name, description, defaultValue);
    }

    public int getRed()   { return (getValue() >> 16) & 0xFF; }
    public int getGreen() { return (getValue() >>  8) & 0xFF; }
    public int getBlue()  { return  getValue()        & 0xFF; }
    public int getAlpha() { return (getValue() >> 24) & 0xFF; }

    public void setRGBA(int r, int g, int b, int a) {
        setValue(((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsInt());
        }
    }
}
