package dev.astro.module;

import dev.astro.AstroClient;
import dev.astro.event.EventTarget;
import dev.astro.event.events.KeyEvent;
import dev.astro.event.events.TickEvent;
import dev.astro.module.misc.AutoGG;
import dev.astro.module.misc.ChatMod;
import dev.astro.module.movement.Freelook;
import dev.astro.module.movement.ToggleSneak;
import dev.astro.module.movement.ToggleSprint;
import dev.astro.module.player.*;
import dev.astro.module.render.Cooldowns;
import dev.astro.module.render.CPSCounter;
import dev.astro.module.render.Keystrokes;
import dev.astro.module.render.TNTTimer;
import dev.astro.module.render.FPSDisplay;
import dev.astro.module.render.CoordinatesDisplay;
import dev.astro.module.render.ClockDisplay;
import dev.astro.module.render.PingDisplay;
import dev.astro.module.render.MemoryDisplay;
import dev.astro.module.render.ServerAddressDisplay;
import dev.astro.module.render.DayCounterDisplay;
import dev.astro.module.render.DirectionDisplay;
import dev.astro.module.render.ComboCounter;
import dev.astro.module.render.ReachDisplay;
import dev.astro.module.render.ItemCounterDisplay;
import dev.astro.module.render.PackDisplay;
import dev.astro.module.render.ArmorStatusDisplay;
import dev.astro.module.render.PotionEffectsDisplay;
import dev.astro.module.render.BedwarsStats;
import dev.astro.module.render.BedwarsTimers;
import dev.astro.module.render.Zoom;
import dev.astro.module.render.Fullbright;
import dev.astro.module.render.LevelHead;
import dev.astro.module.world.*;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for every {@link Module}.
 * Handles initialisation and key-bind toggling.
 */
public final class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void init() {
        register(new ToggleSprint());
        register(new ToggleSneak());
        register(new Keystrokes());
        register(new CPSCounter());
        register(new AutoGG());
        register(new ChatMod());
        register(new TNTTimer());
        register(new Cooldowns());
        register(new FPSDisplay());
        register(new CoordinatesDisplay());
        register(new ClockDisplay());
        register(new PingDisplay());
        register(new MemoryDisplay());
        register(new ServerAddressDisplay());
        register(new DayCounterDisplay());
        register(new DirectionDisplay());
        register(new ComboCounter());
        register(new ReachDisplay());
        register(new ItemCounterDisplay());
        register(new PackDisplay());
        register(new ArmorStatusDisplay());
        register(new PotionEffectsDisplay());
        register(new BedwarsStats());
        register(new BedwarsTimers());

        // Render
        register(new Zoom());
        register(new Fullbright());
        register(new LevelHead());

        // World
        register(new MotionBlur());
        register(new MenuBlur());
        register(new TimeChanger());
        register(new WeatherChanger());
        register(new BlockOutline());

        // Player
        register(new Hitboxes());
        register(new FOVChanger());
        register(new CustomCrosshair());
        register(new HurtCam());
        register(new NametagMod());
        register(new ScoreboardMod());

        // Movement
        register(new Freelook());

        AstroClient.INSTANCE.getEventBus().register(this);
    }

    private void register(Module module) { modules.add(module); }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKeyCode() == 0) return;
        for (Module m : modules) {
            if (m.getActivationMode() != Module.ActivationMode.TOGGLE) continue;
            if (m.getKeyBind() == event.getKeyCode()) m.toggle();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        for (Module m : modules) {
            if (m.getActivationMode() != Module.ActivationMode.HOLD) continue;
            int keyBind = m.getKeyBind();
            boolean shouldBeEnabled = keyBind != Keyboard.KEY_NONE && Keyboard.isKeyDown(keyBind);
            if (m.isEnabled() != shouldBeEnabled) {
                m.setEnabled(shouldBeEnabled);
            }
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getByClass(Class<T> clazz) {
        return (T) modules.stream()
                .filter(clazz::isInstance)
                .findFirst().orElse(null);
    }

    public List<Module> getEnabled() {
        return modules.stream()
                .filter(Module::isEnabled)
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
    }
}
