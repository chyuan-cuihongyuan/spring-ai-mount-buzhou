package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HookEnvironment {

    private final String sessionId;
    private final SessionStateStore stateStore;
    private final AtomicInteger turn = new AtomicInteger();
    private volatile Consumer<SessionEvent> eventPublisher = event -> {
    };
    private final SessionStateHandle stateHandle = new SessionStateHandle() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            return stateStore.get(sessionId, key)
                    .map(StateEntry::value)
                    .filter(type::isInstance)
                    .map(v -> (T) v);
        }

        @Override
        public void put(String key, Object value) {
            stateStore.put(sessionId, new StateEntry(key, String.valueOf(value),
                    "hook", turn.get(), null, Instant.now()));
        }

        @Override
        public void delete(String key) {
            stateStore.delete(sessionId, key);
        }
    };

    public HookEnvironment(String sessionId, SessionStateStore stateStore) {
        this.sessionId = sessionId;
        this.stateStore = stateStore;
    }

    public String sessionId() {
        return sessionId;
    }

    public int nextTurn() {
        return turn.incrementAndGet();
    }

    public int currentTurn() {
        return Math.max(turn.get(), 1);
    }

    public SessionStateHandle stateHandle() {
        return stateHandle;
    }

    public void bindEventPublisher(Consumer<SessionEvent> publisher) {
        this.eventPublisher = publisher;
    }

    public void emit(SessionEvent event) {
        eventPublisher.accept(event);
    }
}
