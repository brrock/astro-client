package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;

public final class MemoryDisplay extends HUDModule {

    public MemoryDisplay() {
        super("Memory Display", "Shows JVM memory usage.", Category.RENDER, 5, 220);
    }

    @Override
    protected String getText() {
        Runtime rt = Runtime.getRuntime();
        long usedMB  = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long totalMB = rt.totalMemory() / (1024L * 1024L);
        return usedMB + " MB / " + totalMB + " MB";
    }
}
