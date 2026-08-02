package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryObservabilityStore implements ObservabilityStore {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SpanRecord>> spans =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<EventRecord>> events =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InjectionSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void saveSpans(List<SpanRecord> spans) {
        spans.forEach(s -> this.spans.computeIfAbsent(s.sessionId(),
                k -> new CopyOnWriteArrayList<>()).add(s));
    }

    @Override
    public void saveEvents(List<EventRecord> events) {
        events.forEach(e -> this.events.computeIfAbsent(e.sessionId(),
                k -> new CopyOnWriteArrayList<>()).add(e));
    }

    @Override
    public List<SpanRecord> spansOfSession(String sessionId) {
        return List.copyOf(spans.getOrDefault(sessionId, new CopyOnWriteArrayList<>()));
    }

    @Override
    public List<EventRecord> eventsOfSession(String sessionId) {
        return List.copyOf(events.getOrDefault(sessionId, new CopyOnWriteArrayList<>()));
    }

    @Override
    public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
        snapshots.put(key(snapshot.sessionId(), snapshot.turnSeq()), snapshot);
    }

    private String key(String sessionId, int turnSeq) {
        return sessionId + ":" + turnSeq;
    }

    @Override
    public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
        return Optional.ofNullable(snapshots.get(key(sessionId, turnSeq)));
    }
}
