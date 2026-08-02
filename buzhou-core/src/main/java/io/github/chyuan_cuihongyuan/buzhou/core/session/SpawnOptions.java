package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

public record SpawnOptions(boolean steal, List<SessionEventListener> listeners) {

    public SpawnOptions {
        listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    public static SpawnOptions defaults() {
        return new SpawnOptions(false, List.of());
    }

    public static SpawnOptions withSteal() {
        return new SpawnOptions(true, List.of());
    }

    public SpawnOptions withListeners(SessionEventListener... toAdd) {
        return new SpawnOptions(steal, List.of(toAdd));
    }
}
