package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 观测内存实现（可再生集合 = volatile-lru 语义，采样近似）。
 *
 * <p>impl-36 / spec 13 §growth-8：有界化——<b>绝不抛配额异常</b>（观测流水可再生，缺失仅
 * 损失可观测性）：
 * <ul>
 *   <li><b>会话级采样逐出</b>：观测会话数（含仅写快照的会话）超 {@code maxObservabilitySessions}
 *       时随机采样 {@value #EVICTION_SAMPLE_SIZE} 个候选、逐出其中最久未活跃者
 *       （Redis volatile-lru 的采样近似思想——不维护全局有序结构，摊还 O(1)；
 *       采样未命中时退回全量精确扫描，容量不漂移）；</li>
 *   <li><b>per-session 记录 FIFO 丢最旧</b>：单会话 spans / events 各自超
 *       {@code maxObservabilityRecordsPerSession} 时丢最旧，丢弃计数
 *       {@link #droppedRecordsCount()} 可观测（丢弃不静默）。</li>
 * </ul>
 */
public class InMemoryObservabilityStore implements ObservabilityStore {

    /** 采样逐出的候选数（Redis volatile-lru 默认 sample 5 的同量级近似）。 */
    static final int EVICTION_SAMPLE_SIZE = 8;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SpanRecord>> spans =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<EventRecord>> events =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InjectionSnapshot> snapshots = new ConcurrentHashMap<>();
    /** impl-36：观测会话注册表（lastActivity 单调时钟——逐出裁决源）。 */
    private final ConcurrentHashMap<String, Long> sessionActivity = new ConcurrentHashMap<>();
    /** impl-36：FIFO 丢最旧的累计记录数（丢弃可见）。 */
    private final AtomicLong droppedRecords = new AtomicLong();

    private final int maxSessions;
    private final int maxRecordsPerSession;

    public InMemoryObservabilityStore() {
        this(InMemoryStoreConfig.defaults());
    }

    public InMemoryObservabilityStore(InMemoryStoreConfig config) {
        InMemoryStoreConfig effective = config == null ? InMemoryStoreConfig.defaults() : config;
        this.maxSessions = effective.maxObservabilitySessions();
        this.maxRecordsPerSession = effective.maxObservabilityRecordsPerSession();
    }

    @Override
    public void saveSpans(List<SpanRecord> spans) {
        spans.forEach(s -> {
            admitSession(s.sessionId());
            CopyOnWriteArrayList<SpanRecord> sessionSpans = this.spans.computeIfAbsent(
                    s.sessionId(), k -> new CopyOnWriteArrayList<>());
            sessionSpans.add(s);
            trimOldest(sessionSpans);
        });
    }

    @Override
    public void saveEvents(List<EventRecord> events) {
        events.forEach(e -> {
            admitSession(e.sessionId());
            CopyOnWriteArrayList<EventRecord> stream = this.events.computeIfAbsent(e.sessionId(),
                    k -> new CopyOnWriteArrayList<>());
            stream.add(e);
            trimOldest(stream);
        });
    }

    /** FIFO 丢最旧（可再生集合近似逐出）：超限部分从头部移除，计数可见。 */
    private void trimOldest(CopyOnWriteArrayList<?> records) {
        int overflow = records.size() - maxRecordsPerSession;
        if (overflow > 0) {
            records.subList(0, overflow).clear();
            droppedRecords.addAndGet(overflow);
        }
    }

    /** 新观测会话准入（含快照路径）：容量满则逐出最久未活跃的其他会话（绝不动刚写入的会话）。 */
    private void admitSession(String sessionId) {
        sessionActivity.put(sessionId, System.nanoTime());
        if (!spans.containsKey(sessionId) && !events.containsKey(sessionId)
                && sessionActivity.size() > maxSessions) {
            evictLeastActiveSampled(sessionId);
        }
    }

    private void evictLeastActiveSampled(String incomingSessionId) {
        String[] candidates = sessionActivity.keySet().toArray(new String[0]);
        // 小候选集（≤ 采样数）全量精确扫描——逐出行为确定（测试与运维可预期）；
        // 大候选集才采样近似（Redis volatile-lru 思想：不维护全局有序结构，摊还 O(1)）
        boolean exact = candidates.length <= EVICTION_SAMPLE_SIZE;
        int iterations = exact ? candidates.length : EVICTION_SAMPLE_SIZE;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String victim = null;
        long victimActivity = Long.MAX_VALUE;
        for (int i = 0; i < iterations; i++) {
            String candidate = exact ? candidates[i]
                    : candidates[random.nextInt(candidates.length)];
            if (candidate.equals(incomingSessionId)) {
                continue;
            }
            long activity = sessionActivity.getOrDefault(candidate, Long.MAX_VALUE);
            if (activity < victimActivity) {
                victim = candidate;
                victimActivity = activity;
            }
        }
        // 采样运气全打在 incoming 上（未命中）→ 全量精确兜底，容量不漂移
        if (victim == null) {
            for (String candidate : candidates) {
                if (candidate.equals(incomingSessionId)) {
                    continue;
                }
                long activity = sessionActivity.getOrDefault(candidate, Long.MAX_VALUE);
                if (activity < victimActivity) {
                    victim = candidate;
                    victimActivity = activity;
                }
            }
        }
        if (victim != null) {
            removeSessionData(victim);
        }
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
        // impl-36：快照路径同样过会话准入——仅写快照的会话计入容量、可被逐出（不留无界洞）
        admitSession(snapshot.sessionId());
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
        removeSessionData(sessionId);
    }

    private void removeSessionData(String sessionId) {
        spans.remove(sessionId);
        events.remove(sessionId);
        String prefix = sessionId + ":";
        snapshots.keySet().removeIf(k -> k.startsWith(prefix));
        sessionActivity.remove(sessionId);
    }

    /** impl-36：FIFO 丢最旧的累计记录数（丢弃不可静默——测试与运维可断言）。 */
    public long droppedRecordsCount() {
        return droppedRecords.get();
    }

    /** impl-36：在册观测会话数（测试与运维可观测）。 */
    int sessionCount() {
        return sessionActivity.size();
    }
}
