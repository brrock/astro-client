package dev.astro.event.events;

import dev.astro.event.Event;

/** Fired on every mouse button press (not release). */
public final class MouseClickEvent extends Event {

    private final int button; // 0 = LMB, 1 = RMB, 2 = MMB

    public MouseClickEvent(int button) { this.button = button; }
    public int getButton()             { return button; }
}
