package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;

import java.time.Instant;
import java.util.Comparator;
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

    @Override
    public List<SessionSummary> listSessionSummaries(String cursor, int size) {
        List<SessionSummary> all = spans.entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(SessionSummary::lastActivityAt).reversed()
                        .thenComparing(SessionSummary::sessionId))
                .toList();
        int from = cursor == null || cursor.isBlank() ? 0 : Integer.parseInt(cursor);
        if (from >= all.size()) {
            return List.of();
        }
        return all.subList(from, Math.min(from + size, all.size()));
    }

    private static SessionSummary summarize(String sessionId, List<SpanRecord> sessionSpans) {
        Instant first = null;
        Instant last = null;
        int turns = 0;
        Map<String, Object> sessionAttributes = Map.of();
        for (SpanRecord s : sessionSpans) {
            if (s.startedAt() != null && (first == null || s.startedAt().isBefore(first))) {
                first = s.startedAt();
            }
            Instant activity = s.activityAt();
            if (activity != null && (last == null || activity.isAfter(last))) {
                last = activity;
            }
            if (SpanKind.TURN.equals(s.kind())) {
                turns++;
            }
            if (SpanKind.SESSION.equals(s.kind())) {
                sessionAttributes = s.attributes();
            }
        }
        // startedAt 全空（内存实现无 NOT NULL 约束）时兜底 EPOCH，保排序键非空
        Instant epoch = Instant.EPOCH;
        return new SessionSummary(sessionId, first == null ? epoch : first,
                last == null ? epoch : last, turns, sessionSpans.size(), sessionAttributes);
    }

    @Override
    public List<EventRecord> eventsOfSpan(String spanId) {
        return events.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.spanId().equals(spanId))
                .sorted(Comparator.comparing(EventRecord::occurredAt))
                .toList();
    }

    /** impl-35 / spec 13 §stores-6：移除该会话全部 spans/events/注入快照（幂等；快照按键前缀清除）。 */
    @Override
    public void deleteSession(String sessionId) {
        spans.remove(sessionId);
        events.remove(sessionId);
        String prefix = sessionId + ":";
        snapshots.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
