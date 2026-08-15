package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Webhook 持久化 outbox（spec 24 / T103 / impl-78）：待投递事件落
 * {@link SessionStateStore} 合成会话 {@link #SESSION_ID}，跨重启不丢。
 *
 * <p><b>键空间</b>：{@code outbox.<eventId>}（未决/退避中）与 {@code dead.<eventId>}
 * （死信，超 max-attempts 或 4xx 即死，不再自动重试）。value = {@link OutboxRecord} JSON
 * （时间用 epoch millis——core 不假定 jackson-jsr310 在 classpath）。
 *
 * <p><b>排序</b>：seq = 进程内 {@link AtomicLong}（启动从存量最大 seq 续起）；多实例共享
 * store 时 seq 可能交错，仅影响投递顺序不影响正确性（at-least-once 契约内，spec 24 §多实例）。
 *
 * <p><b>容量</b>：未决（outbox.*）记录数达 capacity 即拒入（软上限——并发 append 的计数
 * 竞差为 1 条级，spec 24 已记）。构造期即初始化合成会话（内存实现的 maxSessions 准入
 * 在启动期 fail-fast，而非首次事件时静默丢）。
 */
final class WebhookOutbox {

    /** 合成会话 Id：不进任何会话生命周期清理；fsck（T108）白名单成员。 */
    static final String SESSION_ID = "__buzhou.webhook__";
    static final String OUTBOX_PREFIX = "outbox.";
    static final String DEAD_PREFIX = "dead.";
    private static final String META_KEY = "meta.initialized";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(WebhookOutbox.class.getName());

    private final SessionStateStore store;
    private final int capacity;
    private final AtomicLong seq;

    /** 待投递记录（持久化形态）。body = 完整信封 JSON 字符串；时间 epoch millis。 */
    record OutboxRecord(String eventId, String type, String body, long seq,
                        int attempts, long nextAttemptAtEpochMs, long createdAtEpochMs) {

        boolean dueAt(Instant now) {
            return nextAttemptAtEpochMs <= now.toEpochMilli();
        }
    }

    /** 死信查询（遍历序，上限 limit）。 */
    List<WebhookDeadLetter> deadLetters(int limit) {
        return store.scanByPrefix(SESSION_ID, DEAD_PREFIX).entrySet().stream()
                .filter(e -> e.getKey().startsWith(DEAD_PREFIX))
                .map(e -> parse(e.getValue().value()))
                .filter(Objects::nonNull)
                .map(r -> new WebhookDeadLetter(r.eventId(), r.type(), r.attempts(),
                        Instant.ofEpochMilli(r.createdAtEpochMs())))
                .limit(limit)
                .toList();
    }

    WebhookOutbox(SessionStateStore store, int capacity) {
        this.store = Objects.requireNonNull(store, "store");
        this.capacity = capacity;
        long maxSeq = 0;
        for (String prefix : new String[]{OUTBOX_PREFIX, DEAD_PREFIX}) {
            for (StateEntry entry : store.scanByPrefix(SESSION_ID, prefix).values()) {
                OutboxRecord r = parse(entry.value());
                if (r != null) {
                    maxSeq = Math.max(maxSeq, r.seq());
                }
            }
        }
        this.seq = new AtomicLong(maxSeq);
        // 占位键：内存实现 maxSessions 准入在此 fail-fast（启动期配置错误，而非首事件静默丢）
        store.put(SESSION_ID, new StateEntry(META_KEY, "1", "webhook-outbox", 0, null, Instant.now()));
    }

    /** 入队（容量满返回 false，由调用方计 dropped；attempts=0、立即可投递）。 */
    synchronized boolean append(String eventId, String type, String body) {
        if (pendingCount() >= capacity) {
            return false;
        }
        long now = System.currentTimeMillis();
        OutboxRecord record = new OutboxRecord(eventId, type, body, seq.incrementAndGet(), 0, now, now);
        store.put(SESSION_ID, entry(record));
        return true;
    }

    /** 到期记录（nextAttemptAt <= now，按 seq 升序，limit 截断）。损坏记录就地隔离为死信。 */
    List<OutboxRecord> due(Instant now, int limit) {
        return store.scanByPrefix(SESSION_ID, OUTBOX_PREFIX).entrySet().stream()
                .map(e -> Map.entry(e.getKey(), parse(e.getValue().value())))
                .filter(e -> {
                    if (e.getValue() == null) {
                        quarantine(e.getKey());
                        return false;
                    }
                    return e.getValue().dueAt(now);
                })
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingLong(OutboxRecord::seq))
                .limit(limit)
                .toList();
    }

    /** 投递成功即删（幂等键头已让消费端可去重，端上不留窗口——spec 24 定案）。 */
    void delete(String eventId) {
        store.delete(SESSION_ID, OUTBOX_PREFIX + eventId);
    }

    /** 退避状态回写（attempts/nextAttemptAt 持久化，重启后自然续跑）。 */
    void update(OutboxRecord record) {
        store.put(SESSION_ID, entry(record));
    }

    /** 死信隔离：outbox 键迁移 dead 键，容量随之释放。 */
    void markDead(OutboxRecord record) {
        store.delete(SESSION_ID, OUTBOX_PREFIX + record.eventId());
        store.put(SESSION_ID, new StateEntry(DEAD_PREFIX + record.eventId(),
                toJson(record), "webhook-outbox", 0, null, Instant.now()));
    }

    int pendingCount() {
        return store.scanByPrefix(SESSION_ID, OUTBOX_PREFIX).size();
    }

    private StateEntry entry(OutboxRecord record) {
        return new StateEntry(OUTBOX_PREFIX + record.eventId(), toJson(record),
                "webhook-outbox", 0, null, Instant.now());
    }

    private static String toJson(OutboxRecord record) {
        try {
            return MAPPER.writeValueAsString(record);
        } catch (Exception e) {
            throw new IllegalStateException("outbox 记录序列化失败：" + record.eventId(), e);
        }
    }

    private static OutboxRecord parse(String json) {
        try {
            return MAPPER.readValue(json, OutboxRecord.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void quarantine(String key) {
        String eventId = key.substring(OUTBOX_PREFIX.length());
        LOGGER.log(System.Logger.Level.WARNING, "outbox 记录损坏，隔离为死信：" + eventId);
        store.delete(SESSION_ID, key);
        long now = System.currentTimeMillis();
        OutboxRecord poison = new OutboxRecord(eventId, "unknown", "", 0, -1, now, now);
        store.put(SESSION_ID, new StateEntry(DEAD_PREFIX + eventId, toJson(poison),
                "webhook-outbox", 0, null, Instant.now()));
    }
}
