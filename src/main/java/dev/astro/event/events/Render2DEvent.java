package dev.astro.event.events;

import dev.astro.event.Event;

/** Fired every frame after the in-game HUD is drawn. */
public final class Render2DEvent extends Event {

    private final float partialTicks;

    public Render2DEvent(float partialTicks) { this.partialTicks = partialTicks; }
    public float getPartialTicks()           { return partialTicks; }
}
