package dev.astro.module;

/** Categories for organising modules in the ClickGUI. */
public enum Category {

    COMBAT   ("Combat",   0xFFEF4444),   // red
    MOVEMENT ("Movement", 0xFF3B82F6),   // blue
    RENDER   ("Render",   0xFF8B5CF6),   // purple
    PLAYER   ("Player",   0xFF10B981),   // green
    WORLD    ("World",    0xFFF59E0B),   // amber
    MISC     ("Misc",     0xFF6B7280);   // grey

    private final String displayName;
    private final int    colour;

    Category(String displayName, int colour) {
        this.displayName = displayName;
        this.colour      = colour;
    }

    public String getDisplayName() { return displayName; }
    public int    getColour()      { return colour; }
}
