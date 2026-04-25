package dev.astro.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight, reflection-based event bus for AstroClient's internal events.
 * Method look-ups happen once at registration; dispatch is a plain invoke().
 */
public final class EventBus {

    private static final class Subscription {
        final Object listener;
        final Method method;
        Subscription(Object listener, Method method) {
            this.listener = listener;
            this.method   = method;
        }
    }

    private final Map<Class<? extends Event>, CopyOnWriteArrayList<Subscription>>
            registry = new ConcurrentHashMap<>();

    public void register(Object listener) {
        // Walk the entire class hierarchy to find @EventTarget methods
        // (e.g. HUDModule.onRender2D inherited by FPSDisplay, PingDisplay, etc.)
        Class<?> clazz = listener.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(EventTarget.class)) continue;
                if (m.getParameterTypes().length != 1)         continue;

                Class<?> param = m.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(param))      continue;

                m.setAccessible(true);

                @SuppressWarnings("unchecked")
                Class<? extends Event> eventType = (Class<? extends Event>) param;
                registry.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<Subscription>())
                        .add(new Subscription(listener, m));
            }
            clazz = clazz.getSuperclass();
        }
    }

    public void unregister(Object listener) {
        registry.values().forEach(subs ->
                subs.removeIf(s -> s.listener == listener));
    }

    public <T extends Event> T post(T event) {
        CopyOnWriteArrayList<Subscription> subs = registry.get(event.getClass());
        if (subs == null) return event;

        for (Subscription sub : subs) {
            try {
                sub.method.invoke(sub.listener, event);
            } catch (Exception e) {
                System.err.println("[AstroClient] Event dispatch error: " +
                        event.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
        return event;
    }
}
