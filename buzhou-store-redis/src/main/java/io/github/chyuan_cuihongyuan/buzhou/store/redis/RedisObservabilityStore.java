package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis {@link ObservabilityStore}（spec 03 Redis 结构推演）：
 * <ul>
 *   <li>span：per-span HASH（HSET 实现 upsert：RUNNING→终态覆盖同 spanId）+ 会话内 span ZSET 索引（按 startedAt）
 *       + 全局会话活跃 ZSET（listSessionSummaries 数据源，按 lastActivity）；</li>
 *   <li>event：per-event STRING（全局按 eventId 索引）+ 会话内 event ZSET（按 occurredAt）+ per-span event ZSET（eventsOfSpan）；</li>
 *   <li>snapshot：STRING（JSON）+ 可配 PEXPIRE（snapshot.ttl）。</li>
 * </ul>
 *
 * <p>listSessionSummaries 经全局会话 ZSET 逆序分页后，逐会话读 spans 内存聚合
 * （first/last activity、turnCount、spanCount、SESSION 属性袋）——与 JDBC 同口径的页内 N+1（spec 03 推演 #3）。
 */
public class RedisObservabilityStore implements ObservabilityStore {

    private final RedisSync sync;
    private final RedisKeys keys;
    private final Duration snapshotTtl;

    public RedisObservabilityStore(RedisSync sync, RedisKeys keys, Duration snapshotTtl) {
        this.sync = sync;
        this.keys = keys;
        this.snapshotTtl = snapshotTtl == null ? Duration.ZERO : snapshotTtl;
    }

    // ---- 写 ----

    @Override
    public void saveSpans(List<SpanRecord> spans) {
        if (spans == null || spans.isEmpty()) {
            return;
        }
        var c = sync.commands();
        for (SpanRecord s : spans) {
            String spanKey = keys.span(s.sessionId(), s.spanId());
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("spanId", s.spanId());
            fields.put("parentSpanId", s.parentSpanId() == null ? "" : s.parentSpanId());
            fields.put("sessionId", s.sessionId());
            fields.put("turnSeq", Integer.toString(s.turnSeq()));
            fields.put("kind", s.kind());
            fields.put("name", s.name());
            fields.put("startedAt", s.startedAt().toString());
            fields.put("endedAt", s.endedAt() == null ? "" : s.endedAt().toString());
            fields.put("status", s.status());
            fields.put("attributes", RedisJson.write(s.attributes()));
            c.hset(spanKey, fields);
            c.zadd(keys.spansOfSession(s.sessionId()), s.startedAt().toEpochMilli(), s.spanId());
            long activityMs = (s.endedAt() != null ? s.endedAt() : s.startedAt()).toEpochMilli();
            c.zadd(keys.sessionsIndex(), activityMs, s.sessionId());
        }
    }

    @Override
    public void saveEvents(List<EventRecord> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        var c = sync.commands();
        for (EventRecord e : events) {
            c.set(keys.event(e.eventId()), RedisJson.write(e));
            c.zadd(keys.eventsOfSession(e.sessionId()), e.occurredAt().toEpochMilli(), e.eventId());
            if (e.spanId() != null) {
                c.zadd(keys.eventsOfSpan(e.spanId()), e.occurredAt().toEpochMilli(), e.eventId());
            }
        }
    }

    @Override
    public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
        var c = sync.commands();
        String key = keys.snapshot(snapshot.sessionId(), snapshot.turnSeq());
        c.set(key, RedisJson.write(snapshot));
        if (!snapshotTtl.isZero() && !snapshotTtl.isNegative()) {
            c.pexpire(key, snapshotTtl.toMillis());
        }
    }

    // ---- 读 ----

    @Override
    public List<SpanRecord> spansOfSession(String sessionId) {
        List<String> spanIds = sync.commands().zrange(keys.spansOfSession(sessionId), 0, -1);
        List<SpanRecord> out = new ArrayList<>();
        if (spanIds != null) {
            for (String spanId : spanIds) {
                SpanRecord s = readSpan(sessionId, spanId);
                if (s != null) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    @Override
    public List<EventRecord> eventsOfSession(String sessionId) {
        return readEvents(keys.eventsOfSession(sessionId));
    }

    @Override
    public List<EventRecord> eventsOfSpan(String spanId) {
        return readEvents(keys.eventsOfSpan(spanId));
    }

    private List<EventRecord> readEvents(String indexKey) {
        List<String> eventIds = sync.commands().zrange(indexKey, 0, -1);
        List<EventRecord> out = new ArrayList<>();
        if (eventIds != null) {
            for (String eventId : eventIds) {
                EventRecord e = RedisJson.read(sync.commands().get(keys.event(eventId)), EventRecord.class);
                if (e != null) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    @Override
    public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
        return Optional.ofNullable(RedisJson.read(
                sync.commands().get(keys.snapshot(sessionId, turnSeq)), InjectionSnapshot.class));
    }

    @Override
    public List<SessionSummary> listSessionSummaries(String cursor, int size) {
        int offset = (cursor == null || cursor.isBlank()) ? 0 : Integer.parseInt(cursor);
        var c = sync.commands();
        List<String> page = c.zrevrange(keys.sessionsIndex(), offset, offset + Math.max(0, size - 1));
        List<SessionSummary> out = new ArrayList<>();
        if (page != null) {
            for (String sessionId : page) {
                SessionSummary summary = aggregate(sessionId);
                if (summary != null) {
                    out.add(summary);
                }
            }
        }
        return out;
    }

    private SessionSummary aggregate(String sessionId) {
        List<SpanRecord> spans = spansOfSession(sessionId);
        if (spans.isEmpty()) {
            return null;
        }
        Instant first = null;
        Instant last = null;
        int turnCount = 0;
        Map<String, Object> sessionAttrs = Map.of();
        Instant earliestSessionStartedAt = null;
        for (SpanRecord s : spans) {
            Instant started = s.startedAt();
            Instant activity = s.endedAt() != null ? s.endedAt() : started;
            if (first == null || started.isBefore(first)) {
                first = started;
            }
            if (last == null || activity.isAfter(last)) {
                last = activity;
            }
            if (SpanKind.TURN.equals(s.kind())) {
                turnCount++;
            }
            if (SpanKind.SESSION.equals(s.kind())) {
                if (earliestSessionStartedAt == null || started.isBefore(earliestSessionStartedAt)) {
                    earliestSessionStartedAt = started;
                    sessionAttrs = s.attributes();
                }
            }
        }
        return new SessionSummary(sessionId, first, last, turnCount, spans.size(), sessionAttrs);
    }

    private SpanRecord readSpan(String sessionId, String spanId) {
        Map<String, String> f = sync.commands().hgetall(keys.span(sessionId, spanId));
        if (f == null || f.isEmpty()) {
            return null;
        }
        String endedAt = f.get("endedAt");
        String parent = f.get("parentSpanId");
        return new SpanRecord(
                f.get("spanId"),
                (parent == null || parent.isEmpty()) ? null : parent,
                f.get("sessionId"),
                Integer.parseInt(f.get("turnSeq")),
                f.get("kind"),
                f.get("name"),
                Instant.parse(f.get("startedAt")),
                (endedAt == null || endedAt.isEmpty()) ? null : Instant.parse(endedAt),
                f.get("status"),
                RedisJson.readMap(f.get("attributes")));
    }
}
