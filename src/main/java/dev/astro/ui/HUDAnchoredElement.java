package dev.astro.ui;

import dev.astro.module.setting.NumberSetting;

/** Represents a non-HUDModule element anchored at a fixed position (resize only). */
final class HUDAnchoredElement {
    final String name;
    final NumberSetting scaleSetting;
    int x;
    int y;
    int w;
    int h;

    HUDAnchoredElement(String name, NumberSetting scaleSetting) {
        this.name = name;
        this.scaleSetting = scaleSetting;
    }

    boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
