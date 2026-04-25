package dev.astro.ui;

import dev.astro.AstroClient;
import dev.astro.module.Category;
import dev.astro.module.Module;
import dev.astro.module.setting.BooleanSetting;
import dev.astro.module.setting.ColorSetting;
import dev.astro.module.setting.ModeSetting;
import dev.astro.module.setting.NumberSetting;
import dev.astro.module.setting.Setting;
import dev.astro.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickGUI — Lunar Client "Mods" style full-screen panel.
 *
 * Layout:
 *   ┌──────────────────────────────────────────────────────────┐
 *   │                        Mods                          [x] │
 *   │                   [ Search...          ]                 │
 *   │     All  Combat  Movement  Render  Player  World  Misc   │
 *   │  ┌───────────────────────────────────────────────────┐   │
 *   │  │ ToggleSprint                             [=====] │   │
 *   │  │ Automatically holds sprint key                    │   │
 *   │  ├───────────────────────────────────────────────────┤   │
 *   │  │ Keystrokes                               [     ] │   │
 *   │  │ Shows WASD and mouse buttons on screen            │   │
 *   │  └───────────────────────────────────────────────────┘   │
 *   └──────────────────────────────────────────────────────────┘
 *
 * Horizontal category tabs, search bar, scrollable module cards.
 */
public final class ClickGUI extends GuiScreen {

    // ── Layout ─────────────────────────────────────────────────────────
    private static final int HEADER_H      = 32;
    private static final int SEARCH_BAR_H  = 20;
    private static final int TAB_H         = 22;
    private static final int TAB_PAD_X     = 10;
    private static final int TAB_GAP       = 4;
    private static final int CARD_H        = 36;
    private static final int CARD_PAD      = 2;
    private static final int SETTING_ROW_H = 20;
    private static final int SETTING_PAD   = 2;
    private static final int SCROLLBAR_W   = 3;
    private static final int TOGGLE_W      = 28;
    private static final int TOGGLE_H      = 14;

    private static final int[] COLOR_PRESETS = {
        0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF00FFFF,
        0xFFFFFF00, 0xFFFF00FF, 0xFFFF8800
    };

    // ── Colours (Lunar-inspired) ───────────────────────────────────────
    private static final int PANEL_BG       = 0xF0181820;
    private static final int HEADER_BG      = 0xF0101018;
    private static final int CARD_BG        = 0xF01E1E28;
    private static final int CARD_HOVER     = 0xF02A2A36;
    private static final int TAB_ACTIVE_BG  = 0xFF3A3A4A;
    private static final int ACCENT         = 0xFF5B8DEF;
    private static final int TEXT_PRIMARY   = 0xFFE8E8F0;
    private static final int TEXT_SECONDARY = 0xFF888898;
    private static final int TOGGLE_ON_COL  = 0xFF5B8DEF;
    private static final int TOGGLE_OFF_COL = 0xFF3A3A4A;
    private static final int TOGGLE_KNOB    = 0xFFFFFFFF;
    private static final int SEPARATOR      = 0x20FFFFFF;
    private static final int SEARCH_BG      = 0xF0141420;
    private static final int SETTINGS_BG    = 0xF0161620;

    // ── State ──────────────────────────────────────────────────────────
    /** null means "All" tab is selected. */
    private Category selectedCategory = null;
    private String searchQuery = "";

    /** Per-context scroll offset keyed by scroll context string. */
    private final Map<String, Integer> scrollOffsets = new HashMap<String, Integer>();

    /** Smooth toggle animation per module (0..1). */
    private final Map<Module, Float> toggleAnim = new HashMap<Module, Float>();

    /** Currently expanded module showing settings, or null. */
    private Module expandedModule = null;

    /** Slider currently being dragged, or null. */
    private NumberSetting draggingSlider = null;

    // ── Helpers ────────────────────────────────────────────────────────

    private String getScrollKey() {
        if (searchQuery.length() > 0) return "__search__";
        if (selectedCategory == null) return "__all__";
        return selectedCategory.name();
    }

    private List<Module> getFilteredModules() {
        String query = searchQuery.toLowerCase();
        List<Module> source;
        if (query.length() > 0 || selectedCategory == null) {
            source = AstroClient.INSTANCE.getModuleManager().getModules();
        } else {
            source = AstroClient.INSTANCE.getModuleManager()
                    .getByCategory(selectedCategory);
        }
        if (query.length() == 0) return source;

        List<Module> result = new ArrayList<Module>();
        for (Module m : source) {
            if (m.getName().toLowerCase().contains(query)) {
                result.add(m);
            }
        }
        return result;
    }

    /** Panel bounds: {x, y, width, height}. */
    private int[] getPanelBounds() {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int pw = Math.max(400, (int) (sr.getScaledWidth() * 0.7));
        int ph = Math.max(300, (int) (sr.getScaledHeight() * 0.8));
        int px = (sr.getScaledWidth() - pw) / 2;
        int py = (sr.getScaledHeight() - ph) / 2;
        return new int[]{px, py, pw, ph};
    }

    /** Y where the search bar starts. */
    private int getSearchY(int py) { return py + HEADER_H + 6; }
    /** Y where the category tabs start. */
    private int getTabsY(int py) { return getSearchY(py) + SEARCH_BAR_H + 8; }
    /** Y where the module list starts. */
    private int getListTopY(int py) { return getTabsY(py) + TAB_H + 6; }

    // ── Rendering ──────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        FontRenderer fr = mc.fontRendererObj;
        int[] b = getPanelBounds();
        drawPanel(fr, b[0], b[1], b[2], b[3], mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(FontRenderer fr, int px, int py, int pw, int ph,
                           int mouseX, int mouseY) {
        int right = px + pw;
        int bottom = py + ph;

        // Panel background
        RenderUtil.drawRoundedRect(px, py, right, bottom, 4, PANEL_BG);

        // ── Header ─────────────────────────────────────────────────────
        RenderUtil.drawRoundedRect(px, py, right, py + HEADER_H, 4, HEADER_BG);
        // Patch bottom corners so header merges cleanly into panel
        RenderUtil.drawRect(px, py + HEADER_H - 4, right, py + HEADER_H, HEADER_BG);
        RenderUtil.drawRect(px, py + HEADER_H - 1, right, py + HEADER_H, SEPARATOR);

        // Title "Mods" scaled 1.5x, centered
        String title = "Mods";
        float titleScale = 1.5f;
        int rawTitleW = fr.getStringWidth(title);
        float scaledW = rawTitleW * titleScale;
        float scaledH = fr.FONT_HEIGHT * titleScale;
        float titleX = px + (pw - scaledW) / 2f;
        float titleY = py + (HEADER_H - scaledH) / 2f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0);
        GlStateManager.scale(titleScale, titleScale, 1f);
        fr.drawStringWithShadow(title, 0, 0, TEXT_PRIMARY);
        GlStateManager.popMatrix();

        // Close button "x" top-right
        String closeBtn = "x";
        int closeBtnW = fr.getStringWidth(closeBtn);
        int closeBtnX = right - closeBtnW - 10;
        boolean closeHover = mouseX >= closeBtnX - 4 && mouseX <= right - 4
                          && mouseY >= py && mouseY < py + HEADER_H;
        fr.drawStringWithShadow(closeBtn, closeBtnX,
                py + (HEADER_H - fr.FONT_HEIGHT) / 2f,
                closeHover ? 0xFFFF6666 : TEXT_SECONDARY);

        // ── Search bar ─────────────────────────────────────────────────
        int searchW = (int) (pw * 0.6f);
        int searchX = px + (pw - searchW) / 2;
        int searchY = getSearchY(py);

        RenderUtil.drawRoundedRect(searchX, searchY, searchX + searchW,
                searchY + SEARCH_BAR_H, 3, SEARCH_BG);
        RenderUtil.drawOutline(searchX, searchY, searchX + searchW,
                searchY + SEARCH_BAR_H, 0x18FFFFFF);

        float searchTextY = searchY + (SEARCH_BAR_H - fr.FONT_HEIGHT) / 2f;
        if (searchQuery.length() > 0) {
            fr.drawStringWithShadow(searchQuery, searchX + 8, searchTextY, TEXT_PRIMARY);
            if (System.currentTimeMillis() % 1000 < 500) {
                int cursorX = searchX + 8 + fr.getStringWidth(searchQuery);
                fr.drawStringWithShadow("_", cursorX, searchTextY, TEXT_PRIMARY);
            }
        } else {
            fr.drawStringWithShadow("Search...", searchX + 8, searchTextY, TEXT_SECONDARY);
        }

        // ── Category tabs ──────────────────────────────────────────────
        int tabsY = getTabsY(py);
        drawTabs(fr, px, pw, tabsY, mouseX, mouseY);

        // Separator below tabs
        int listTopY = getListTopY(py);
        RenderUtil.drawRect(px + 8, listTopY - 1, right - 8, listTopY, SEPARATOR);

        // ── Module list ────────────────────────────────────────────────
        int listH = bottom - listTopY - 6;
        drawModuleList(fr, px, pw, listTopY, listH, right, mouseX, mouseY);
    }

    // ── Tabs ───────────────────────────────────────────────────────────

    private void drawTabs(FontRenderer fr, int px, int pw, int tabsY,
                          int mouseX, int mouseY) {
        Category[] cats = Category.values();
        int count = cats.length + 1; // +1 for "All"
        String[] names = new String[count];
        names[0] = "All";
        for (int i = 0; i < cats.length; i++) {
            names[i + 1] = cats[i].getDisplayName();
        }

        int[] widths = new int[count];
        int totalW = 0;
        for (int i = 0; i < count; i++) {
            widths[i] = fr.getStringWidth(names[i]) + TAB_PAD_X * 2;
            totalW += widths[i];
            if (i > 0) totalW += TAB_GAP;
        }

        int tabX = px + (pw - totalW) / 2;
        for (int i = 0; i < count; i++) {
            int tw = widths[i];
            boolean selected = (i == 0) ? selectedCategory == null
                                        : cats[i - 1] == selectedCategory;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tw
                           && mouseY >= tabsY && mouseY < tabsY + TAB_H;

            if (selected) {
                RenderUtil.drawRoundedRect(tabX, tabsY, tabX + tw,
                        tabsY + TAB_H, 3, TAB_ACTIVE_BG);
                // Accent underline
                RenderUtil.drawRect(tabX + 4, tabsY + TAB_H - 2,
                        tabX + tw - 4, tabsY + TAB_H, ACCENT);
            } else if (hovered) {
                RenderUtil.drawRoundedRect(tabX, tabsY, tabX + tw,
                        tabsY + TAB_H, 3, 0x18FFFFFF);
            }

            fr.drawStringWithShadow(names[i],
                    tabX + TAB_PAD_X,
                    tabsY + (TAB_H - fr.FONT_HEIGHT) / 2f,
                    selected ? TEXT_PRIMARY : TEXT_SECONDARY);

            tabX += tw + TAB_GAP;
        }
    }

    // ── Module list ────────────────────────────────────────────────────

    private void drawModuleList(FontRenderer fr, int px, int pw,
                                int listTopY, int listH, int panelRight,
                                int mouseX, int mouseY) {
        List<Module> mods = getFilteredModules();
        int cardX = px + 8;
        int cardW = pw - 16 - SCROLLBAR_W - 4;
        int cardRight = cardX + cardW;

        String scrollKey = getScrollKey();
        int scroll = scrollOffsets.containsKey(scrollKey)
                ? scrollOffsets.get(scrollKey) : 0;

        int totalH = 0;
        for (Module m : mods) {
            totalH += CARD_H + CARD_PAD;
            if (m == expandedModule && m.hasSettings()) {
                totalH += m.getSettings().size() * (SETTING_ROW_H + SETTING_PAD);
            }
        }
        int maxScroll = Math.max(0, totalH - listH);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
        scrollOffsets.put(scrollKey, scroll);

        int listBottom = listTopY + listH;
        int rowY = listTopY - scroll;

        for (Module mod : mods) {
            int cardTop = rowY;
            int cardBot = rowY + CARD_H;

            if (cardBot > listTopY && cardTop < listBottom) {
                boolean hovered = mouseX >= cardX && mouseX < cardRight
                               && mouseY >= Math.max(cardTop, listTopY)
                               && mouseY < Math.min(cardBot, listBottom);

                // Card bg
                RenderUtil.drawRoundedRect(cardX, cardTop, cardRight, cardBot, 3,
                        hovered ? CARD_HOVER : CARD_BG);
                // Bottom separator
                RenderUtil.drawRect(cardX + 4, cardBot - 1, cardRight - 4, cardBot, SEPARATOR);

                // Toggle switch (right side)
                int toggleX = cardRight - TOGGLE_W - 10;
                int toggleY = cardTop + (CARD_H - TOGGLE_H) / 2;

                float anim = toggleAnim.containsKey(mod) ? toggleAnim.get(mod)
                        : (mod.isEnabled() ? 1f : 0f);
                float target = mod.isEnabled() ? 1f : 0f;
                anim += (target - anim) * 0.3f;
                if (Math.abs(anim - target) < 0.01f) anim = target;
                toggleAnim.put(mod, anim);
                drawToggle(toggleX, toggleY, TOGGLE_W, TOGGLE_H, anim);

                // Settings indicator
                int textRightLimit = toggleX - 8;
                if (mod.hasSettings()) {
                    String gear = (mod == expandedModule) ? "v" : ">";
                    int gearW = fr.getStringWidth(gear);
                    fr.drawStringWithShadow(gear, toggleX - gearW - 6,
                            cardTop + (CARD_H - fr.FONT_HEIGHT) / 2f, TEXT_SECONDARY);
                    textRightLimit = toggleX - gearW - 14;
                }

                // Module name
                fr.drawStringWithShadow(mod.getName(), cardX + 10, cardTop + 7, TEXT_PRIMARY);

                // Description (truncated to fit)
                String desc = mod.getDescription();
                int descMaxW = textRightLimit - (cardX + 10);
                if (descMaxW > 20) {
                    if (fr.getStringWidth(desc) > descMaxW) {
                        while (desc.length() > 0 && fr.getStringWidth(desc + "...") > descMaxW) {
                            desc = desc.substring(0, desc.length() - 1);
                        }
                        desc = desc + "...";
                    }
                    fr.drawStringWithShadow(desc, cardX + 10, cardTop + 19, TEXT_SECONDARY);
                }
            }

            rowY += CARD_H + CARD_PAD;

            // Expanded settings
            if (mod == expandedModule && mod.hasSettings()) {
                for (Setting<?> setting : mod.getSettings()) {
                    int sTop = rowY;
                    int sBot = rowY + SETTING_ROW_H;
                    if (sBot > listTopY && sTop < listBottom) {
                        drawSettingRow(fr, setting, cardX + 16, cardW - 24, sTop);
                    }
                    rowY += SETTING_ROW_H + SETTING_PAD;
                }
            }
        }

        // Scrollbar
        if (maxScroll > 0) {
            int barTrackX = panelRight - SCROLLBAR_W - 4;
            int barH = Math.max(20, listH * listH / totalH);
            int barY = listTopY + (int) ((float) scroll / maxScroll * (listH - barH));
            RenderUtil.drawRoundedRect(barTrackX, barY, barTrackX + SCROLLBAR_W,
                    barY + barH, 1, 0x40FFFFFF);
        }
    }

    // ── Toggle switch ──────────────────────────────────────────────────

    private void drawToggle(int x, int y, int w, int h, float anim) {
        int trackCol = lerpColour(TOGGLE_OFF_COL, TOGGLE_ON_COL, anim);
        RenderUtil.drawRoundedRect(x, y, x + w, y + h, 3, trackCol);
        int knobSize = h - 4;
        float knobX = x + 2 + (w - knobSize - 4) * anim;
        RenderUtil.drawRoundedRect(knobX, y + 2, knobX + knobSize, y + h - 2, 2, TOGGLE_KNOB);
    }

    // ── Setting rows ───────────────────────────────────────────────────

    private void drawSettingRow(FontRenderer fr, Setting<?> setting,
                                int settX, int settW, int y) {
        int settRight = settX + settW;
        RenderUtil.drawRoundedRect(settX, y, settRight, y + SETTING_ROW_H, 2, SETTINGS_BG);
        RenderUtil.drawRect(settX, y, settX + 2, y + SETTING_ROW_H, ACCENT);

        float labelY = y + (SETTING_ROW_H - fr.FONT_HEIGHT) / 2f;
        fr.drawStringWithShadow(setting.getName(), settX + 8, labelY, TEXT_SECONDARY);

        if (setting instanceof BooleanSetting) {
            drawBooleanSetting((BooleanSetting) setting, settRight, y);
        } else if (setting instanceof NumberSetting) {
            drawNumberSetting(fr, (NumberSetting) setting, settX, settRight, y);
        } else if (setting instanceof ModeSetting) {
            drawModeSetting(fr, (ModeSetting) setting, settRight, y);
        } else if (setting instanceof ColorSetting) {
            drawColorSetting(fr, (ColorSetting) setting, settX, settRight, y);
        }
    }

    private void drawBooleanSetting(BooleanSetting setting, int right, int y) {
        float anim = setting.getValue() ? 1f : 0f;
        int tw = 20, th = 10;
        int tX = right - tw - 8;
        int tY = y + (SETTING_ROW_H - th) / 2;
        drawToggle(tX, tY, tw, th, anim);
    }

    private void drawNumberSetting(FontRenderer fr, NumberSetting setting,
                                    int settX, int right, int y) {
        int sliderX = settX + 66;
        int sliderRight = right - 40;
        int sliderY = y + SETTING_ROW_H / 2 - 2;
        int sliderH = 4;

        RenderUtil.drawRoundedRect(sliderX, sliderY, sliderRight, sliderY + sliderH,
                1, TOGGLE_OFF_COL);
        float pct = setting.getPercent();
        int fillEnd = sliderX + (int) ((sliderRight - sliderX) * pct);
        if (fillEnd > sliderX) {
            RenderUtil.drawRoundedRect(sliderX, sliderY, fillEnd, sliderY + sliderH, 1, ACCENT);
        }
        RenderUtil.drawRoundedRect(fillEnd - 3, sliderY - 2, fillEnd + 3,
                sliderY + sliderH + 2, 2, TOGGLE_KNOB);

        String valStr;
        if (setting.getStep() >= 1) {
            valStr = String.valueOf(setting.getIntValue());
        } else {
            valStr = String.format("%.1f", setting.getValue());
        }
        fr.drawStringWithShadow(valStr, right - 36,
                y + (SETTING_ROW_H - fr.FONT_HEIGHT) / 2f, TEXT_PRIMARY);
    }

    private void drawModeSetting(FontRenderer fr, ModeSetting setting,
                                  int right, int y) {
        String text = "> " + setting.getValue();
        int tw = fr.getStringWidth(text);
        fr.drawStringWithShadow(text, right - 8 - tw,
                y + (SETTING_ROW_H - fr.FONT_HEIGHT) / 2f, ACCENT);
    }

    private void drawColorSetting(FontRenderer fr, ColorSetting setting,
                                   int settX, int right, int y) {
        int swatchX = settX + 66;
        int swatchY = y + (SETTING_ROW_H - 10) / 2;
        RenderUtil.drawRect(swatchX, swatchY, swatchX + 10, swatchY + 10, setting.getValue());
        RenderUtil.drawOutline(swatchX, swatchY, swatchX + 10, swatchY + 10, 0x80FFFFFF);

        int dotX = swatchX + 16;
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            RenderUtil.drawRoundedRect(dotX, swatchY + 1, dotX + 8, swatchY + 9,
                    1, COLOR_PRESETS[i]);
            dotX += 12;
        }
    }

    // ── Input ──────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
        super.mouseClicked(mouseX, mouseY, btn);
        int[] b = getPanelBounds();
        int px = b[0], py = b[1], pw = b[2], ph = b[3];
        int right = px + pw;
        int bottom = py + ph;
        FontRenderer fr = mc.fontRendererObj;

        // ── Close button ───────────────────────────────────────────────
        if (btn == 0) {
            int closeBtnW = fr.getStringWidth("x");
            int closeBtnX = right - closeBtnW - 10;
            if (mouseX >= closeBtnX - 4 && mouseX <= right - 4
             && mouseY >= py && mouseY < py + HEADER_H) {
                mc.displayGuiScreen(null);
                return;
            }
        }

        // ── Tab click ──────────────────────────────────────────────────
        int tabsY = getTabsY(py);
        if (btn == 0 && mouseY >= tabsY && mouseY < tabsY + TAB_H) {
            Category[] cats = Category.values();
            int count = cats.length + 1;
            String[] names = new String[count];
            names[0] = "All";
            for (int i = 0; i < cats.length; i++) {
                names[i + 1] = cats[i].getDisplayName();
            }
            int[] widths = new int[count];
            int totalW = 0;
            for (int i = 0; i < count; i++) {
                widths[i] = fr.getStringWidth(names[i]) + TAB_PAD_X * 2;
                totalW += widths[i];
                if (i > 0) totalW += TAB_GAP;
            }
            int tabX = px + (pw - totalW) / 2;
            for (int i = 0; i < count; i++) {
                int tw = widths[i];
                if (mouseX >= tabX && mouseX < tabX + tw) {
                    selectedCategory = (i == 0) ? null : cats[i - 1];
                    expandedModule = null;
                    return;
                }
                tabX += tw + TAB_GAP;
            }
        }

        if (btn != 0 && btn != 1) return;

        // ── Module list click ──────────────────────────────────────────
        int listTopY = getListTopY(py);
        int listH = bottom - listTopY - 6;
        int listBottom = listTopY + listH;
        int cardX = px + 8;
        int cardW = pw - 16 - SCROLLBAR_W - 4;
        int cardRight = cardX + cardW;

        String scrollKey = getScrollKey();
        int scroll = scrollOffsets.containsKey(scrollKey)
                ? scrollOffsets.get(scrollKey) : 0;

        List<Module> mods = getFilteredModules();
        int rowY = listTopY - scroll;

        for (Module mod : mods) {
            int cardTop = rowY;
            int cardBot = rowY + CARD_H;

            if (cardBot > listTopY && cardTop < listBottom) {
                if (mouseX >= cardX && mouseX < cardRight
                 && mouseY >= Math.max(cardTop, listTopY)
                 && mouseY < Math.min(cardBot, listBottom)) {
                    if (btn == 0) {
                        mod.toggle();
                    } else if (mod.hasSettings()) {
                        expandedModule = (expandedModule == mod) ? null : mod;
                    }
                    return;
                }
            }
            rowY += CARD_H + CARD_PAD;

            // Settings sub-row hit-test
            if (mod == expandedModule && mod.hasSettings()) {
                int settX = cardX + 16;
                int settW = cardW - 24;
                int settRight = settX + settW;
                for (Setting<?> setting : mod.getSettings()) {
                    int sTop = rowY;
                    int sBot = rowY + SETTING_ROW_H;
                    if (sBot > listTopY && sTop < listBottom) {
                        if (mouseX >= settX && mouseX < settRight
                         && mouseY >= Math.max(sTop, listTopY)
                         && mouseY < Math.min(sBot, listBottom)) {
                            handleSettingClick(setting, mouseX, btn, settX, settRight);
                            return;
                        }
                    }
                    rowY += SETTING_ROW_H + SETTING_PAD;
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            String scrollKey = getScrollKey();
            int scroll = scrollOffsets.containsKey(scrollKey)
                    ? scrollOffsets.get(scrollKey) : 0;
            scroll -= (wheel > 0 ? 1 : -1) * (CARD_H + CARD_PAD) * 2;
            if (scroll < 0) scroll = 0;
            scrollOffsets.put(scrollKey, scroll);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int btn,
                                   long timeSinceLastClick) {
        if (draggingSlider != null && btn == 0) {
            int[] b = getPanelBounds();
            int cardX = b[0] + 8;
            int cardW = b[2] - 16 - SCROLLBAR_W - 4;
            int settX = cardX + 16;
            int settW = cardW - 24;
            int settRight = settX + settW;
            int sliderX = settX + 66;
            int sliderRight = settRight - 40;
            float pct = (float) (mouseX - sliderX) / (sliderRight - sliderX);
            pct = Math.max(0f, Math.min(1f, pct));
            draggingSlider.setPercent(pct);
        }
        super.mouseClickMove(mouseX, mouseY, btn, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingSlider != null) {
            draggingSlider = null;
            saveConfig();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // Escape
            if (searchQuery.length() > 0) {
                searchQuery = "";
            } else {
                mc.displayGuiScreen(null);
            }
            return;
        }
        if (keyCode == 14) { // Backspace
            if (searchQuery.length() > 0) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            }
            return;
        }
        if (typedChar >= ' ' && typedChar < 127) {
            searchQuery += typedChar;
        }
    }

    // ── Setting click dispatch ─────────────────────────────────────────

    private void handleSettingClick(Setting<?> setting, int mouseX, int btn,
                                     int settX, int settRight) {
        if (setting instanceof BooleanSetting) {
            if (btn == 0) {
                ((BooleanSetting) setting).toggle();
                saveConfig();
            }
        } else if (setting instanceof NumberSetting) {
            if (btn == 0) {
                int sliderX = settX + 66;
                int sliderRight = settRight - 40;
                if (mouseX >= sliderX && mouseX <= sliderRight) {
                    NumberSetting ns = (NumberSetting) setting;
                    float pct = (float) (mouseX - sliderX) / (sliderRight - sliderX);
                    pct = Math.max(0f, Math.min(1f, pct));
                    ns.setPercent(pct);
                    draggingSlider = ns;
                }
            }
        } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting) setting;
            if (btn == 0) {
                ms.cycle();
            } else if (btn == 1) {
                ms.cycleBack();
            }
            saveConfig();
        } else if (setting instanceof ColorSetting) {
            if (btn == 0) {
                int dotX = settX + 66 + 16;
                for (int i = 0; i < COLOR_PRESETS.length; i++) {
                    if (mouseX >= dotX && mouseX < dotX + 8) {
                        ((ColorSetting) setting).setValue(COLOR_PRESETS[i]);
                        saveConfig();
                        return;
                    }
                    dotX += 12;
                }
            }
        }
    }

    private void saveConfig() {
        if (AstroClient.INSTANCE.getConfigManager() != null) {
            AstroClient.INSTANCE.getConfigManager().save();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ── Colour math ────────────────────────────────────────────────────

    private static int lerpColour(int c1, int c2, float t) {
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >>  8) & 0xFF) + (((c2 >>  8) & 0xFF) - ((c1 >>  8) & 0xFF)) * t);
        int b = (int) (( c1        & 0xFF) + (( c2        & 0xFF) - ( c1        & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
