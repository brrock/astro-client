package dev.astro.event.events;

import dev.astro.event.Event;

/** Fired when a key is pressed in-game. */
public final class KeyEvent extends Event {

    private final int keyCode;

    public KeyEvent(int keyCode) { this.keyCode = keyCode; }
    public int getKeyCode()      { return keyCode; }
}
