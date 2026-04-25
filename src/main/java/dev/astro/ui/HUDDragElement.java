package dev.astro.ui;

import dev.astro.module.HUDModule;

/** Wraps a HUDModule for drag interaction. */
final class HUDDragElement {
    final String name;
    final HUDModule hudModule;
    int x;
    int y;
    int w;
    int h;

    HUDDragElement(HUDModule mod) {
        this.name = mod.getName();
        this.hudModule = mod;
        this.x = mod.getRenderX();
        this.y = mod.getRenderY();
        this.w = Math.max(mod.getWidth(), 40);
        this.h = Math.max(mod.getHeight(), 14);
    }

    boolean contains(int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
