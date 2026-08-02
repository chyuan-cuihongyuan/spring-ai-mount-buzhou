package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStateStore implements SessionStateStore {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, StateEntry>> bySession =
            new ConcurrentHashMap<>();

    @Override
    public void put(String sessionId, StateEntry entry) {
        bySession.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(entry.key(), entry);
    }

    @Override
    public Optional<StateEntry> get(String sessionId, String key) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        return session == null ? Optional.empty() : Optional.ofNullable(session.get(key));
    }

    @Override
    public Map<String, StateEntry> getAll(String sessionId) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        return session == null ? Map.of() : Map.copyOf(session);
    }

    @Override
    public void delete(String sessionId, String key) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        if (session != null) {
            session.remove(key);
        }
    }
}
