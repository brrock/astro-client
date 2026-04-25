package dev.astro.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A numeric setting with min/max bounds and a step size.
 * Rendered as a slider in the ClickGUI.
 */
public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue,
                         double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin()  { return min; }
    public double getMax()  { return max; }
    public double getStep() { return step; }

    @Override
    public void setValue(Double value) {
        // Clamp to bounds and snap to step
        double clamped = Math.max(min, Math.min(max, value));
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
        }
        super.setValue(clamped);
    }

    /** Convenience: get as int. */
    public int getIntValue() {
        return (int) Math.round(getValue());
    }

    /** Convenience: get as float. */
    public float getFloatValue() {
        return getValue().floatValue();
    }

    /** Returns the normalised position (0..1) within min..max. */
    public float getPercent() {
        if (max == min) return 0;
        return (float) ((getValue() - min) / (max - min));
    }

    /** Sets from normalised position (0..1). */
    public void setPercent(float pct) {
        setValue(min + (max - min) * Math.max(0, Math.min(1, pct)));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(getValue());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            setValue(element.getAsDouble());
        }
    }
}
