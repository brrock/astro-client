package dev.astro.event;

/**
 * Base class for every custom AstroClient event dispatched through our
 * internal {@link EventBus}.  Separate from Forge's event system.
 */
public abstract class Event {

    private boolean cancelled;

    public boolean isCancelled()              { return cancelled; }
    public void    setCancelled(boolean flag) { this.cancelled = flag; }
}
