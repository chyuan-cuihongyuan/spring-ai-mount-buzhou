package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * spawn 选项——steal（抢占既有租约）/ listeners（随会话附加的事件监听者）。
 */
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
