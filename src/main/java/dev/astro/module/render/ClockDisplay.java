package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class ClockDisplay extends HUDModule {

    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");

    public ClockDisplay() {
        super("Clock", "Shows the current real-world time.", Category.RENDER, 5, 180);
    }

    @Override
    protected String getText() {
        return fmt.format(new Date());
    }
}
