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
    /** spec 79 §A / T311：due-time 索引前缀（键 = due.<16 位零垫 nextAttemptAt>.<eventId>）。 */
    static final String DUE_PREFIX = "due.";
    private static final String META_KEY = "meta.initialized";
    /** spec 303 / T597：持久投递纪元键（发送方每次启动递增——重启显式化，接收方 fence 据此 RESET）。 */
    static final String META_EPOCH_KEY = "meta.epoch";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(WebhookOutbox.class.getName());

    private final SessionStateStore store;
    private final int capacity;
    private final AtomicLong seq;
    /** spec 303 / T597：本进程投递纪元（启动期持久递增；恒正）。 */
    private final long epoch;

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
        for (StateEntry entry : store.scanByPrefix(SESSION_ID, OUTBOX_PREFIX).values()) {
            OutboxRecord r = parse(entry.value());
            if (r != null) {
                maxSeq = Math.max(maxSeq, r.seq());
                // spec 79 §A / T311：存量记录 due 索引回填（幂等——旧版无索引数据迁移）
                store.put(SESSION_ID, indexEntry(dueKey(r), r.eventId()));
            }
        }
        for (StateEntry entry : store.scanByPrefix(SESSION_ID, DEAD_PREFIX).values()) {
            OutboxRecord r = parse(entry.value());
            if (r != null) {
                maxSeq = Math.max(maxSeq, r.seq());
            }
        }
        this.seq = new AtomicLong(maxSeq);
        // spec 303 / T597：持久纪元递增——max(持久值+1, 启动墙钟毫秒) 防快启同毫秒撞号；
        // 回写失败降级墙钟值（不持久但实际不撞），纪元恒正。
        long persisted = 0;
        try {
            StateEntry epochEntry = store.get(SESSION_ID, META_EPOCH_KEY).orElse(null);
            if (epochEntry != null) {
                persisted = Long.parseLong(epochEntry.value());
            }
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "投递纪元读取失败，降级墙钟纪元", e);
        }
        this.epoch = Math.max(Math.max(persisted + 1, 1), System.currentTimeMillis());
        try {
            store.put(SESSION_ID, new StateEntry(META_EPOCH_KEY, String.valueOf(epoch),
                    "webhook-outbox", 0, null, Instant.now()));
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "投递纪元回写失败（本进程纪元仍生效，下次启动可能复用墙钟）", e);
        }
        // 占位键：内存实现 maxSessions 准入在此 fail-fast（启动期配置错误，而非首事件静默丢）
        store.put(SESSION_ID, new StateEntry(META_KEY, "1", "webhook-outbox", 0, null, Instant.now()));
    }

    /** spec 533 / T817：设置载荷上限（0 = 不限；forwarder 装配透传）。 */
    void setMaxPayloadChars(int chars) {
        this.maxPayloadChars = chars;
    }

    /** 本进程投递纪元（恒正；信封 epoch 字段来源——spec 303）。 */
    long epoch() {
        return epoch;
    }

    /** spec 533 / T817：载荷上限（0 = 不限——默认零变化；Kafka max message size 思想）。 */
    private volatile int maxPayloadChars;

    /** 入队（容量满/超载荷上限返回 false，由调用方计 dropped；attempts=0、立即可投递）。 */
    synchronized boolean append(String eventId, String type, String body) {
        if (pendingCount() >= capacity) {
            return false;
        }
        if (maxPayloadChars > 0 && body != null && body.length() > maxPayloadChars) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.webhook.payload-oversized");
            return false;
        }
        long now = System.currentTimeMillis();
        OutboxRecord record = new OutboxRecord(eventId, type, body, seq.incrementAndGet(), 0, now, now);
        store.put(SESSION_ID, entry(record));
        store.put(SESSION_ID, indexEntry(dueKey(record), eventId));
        return true;
    }

    /**
     * 到期记录（nextAttemptAt <= now，按 seq 升序，limit 截断）。spec 79 §A / T311：
     * 经 due-time 索引键序区间读（scanByKeyRange）取最早到期者——退避积压不再全量
     * 读值；索引自愈：孤儿（记录已删）与陈旧（记录已后移）就地清键。损坏记录隔离
     * 死信。limit 按到期序先取（重退避者不再被挤饿——与旧全量扫的 seq 优先差异
     * 显性化于此，spec 79 §A 定案）。
     */
    List<OutboxRecord> due(Instant now, int limit) {
        List<OutboxRecord> out = new java.util.ArrayList<>();
        store.scanByKeyRange(SESSION_ID, DUE_PREFIX, null,
                DUE_PREFIX + pad(now.toEpochMilli() + 1), limit)
                .forEach((indexKey, indexEntry) -> {
                    String eventId = eventIdOf(indexKey);
                    java.util.Optional<StateEntry> recordEntry =
                            store.get(SESSION_ID, OUTBOX_PREFIX + eventId);
                    if (recordEntry.isEmpty()) {
                        store.delete(SESSION_ID, indexKey); // 孤儿索引：自愈清键
                        return;
                    }
                    OutboxRecord r = parse(recordEntry.get().value());
                    if (r == null) {
                        quarantine(OUTBOX_PREFIX + eventId);
                        store.delete(SESSION_ID, indexKey);
                        return;
                    }
                    if (!r.dueAt(now)) {
                        store.delete(SESSION_ID, indexKey); // 陈旧索引：记录已后移，新键在位
                        return;
                    }
                    out.add(r);
                });
        out.sort(Comparator.comparingLong(OutboxRecord::seq));
        return out;
    }

    /** 投递成功即删（幂等键头已让消费端可去重，端上不留窗口——spec 24 定案；索引键同删）。 */
    void delete(OutboxRecord record) {
        store.delete(SESSION_ID, OUTBOX_PREFIX + record.eventId());
        store.delete(SESSION_ID, dueKey(record));
    }

    /** 退避状态回写（attempts/nextAttemptAt 持久化，重启后自然续跑；索引键随迁）。 */
    void update(OutboxRecord previous, OutboxRecord updated) {
        store.put(SESSION_ID, entry(updated));
        store.delete(SESSION_ID, dueKey(previous));
        store.put(SESSION_ID, indexEntry(dueKey(updated), updated.eventId()));
    }

    /** 死信隔离：outbox 键迁移 dead 键，容量随之释放（索引键同删）。 */
    void markDead(OutboxRecord record) {
        store.delete(SESSION_ID, OUTBOX_PREFIX + record.eventId());
        store.delete(SESSION_ID, dueKey(record));
        store.put(SESSION_ID, new StateEntry(DEAD_PREFIX + record.eventId(),
                toJson(record), "webhook-outbox", 0, null, Instant.now()));
    }

    /** spec 58 §A / T259：容量计数走 countByPrefix 下推（append 热路径不再全量读值）。 */
    int pendingCount() {
        return store.countByPrefix(SESSION_ID, OUTBOX_PREFIX);
    }

    /**
     * spec 135 / T483：最老待投记录（全量扫含<b>退避中</b>——due() 只见到期者；
     * 取 createdAt 最早）。损坏记录跳过（隔离归 due() 路径既有语义）；无积压 = empty。
     */
    java.util.Optional<OutboxRecord> pendingOldest(int scanLimit) {
        OutboxRecord oldest = null;
        for (Map.Entry<String, StateEntry> e
                : store.scanByPrefix(SESSION_ID, OUTBOX_PREFIX).entrySet()) {
            if (scanLimit-- <= 0) {
                break;
            }
            OutboxRecord r = parse(e.getValue().value());
            if (r == null) {
                continue;
            }
            if (oldest == null || r.createdAtEpochMs() < oldest.createdAtEpochMs()
                    || (r.createdAtEpochMs() == oldest.createdAtEpochMs()
                            && r.seq() < oldest.seq())) {
                oldest = r;
            }
        }
        return java.util.Optional.ofNullable(oldest);
    }

    /** 死信计数（spec 135 lag 面）。 */
    int deadCount() {
        return store.countByPrefix(SESSION_ID, DEAD_PREFIX);
    }

    /** spec 37 §B / T133 / impl-106：死信迁回 outbox（attempts=0、立即可投递）；容量满则停。 */
    synchronized int requeueDead(int limit) {
        int requeued = 0;
        for (Map.Entry<String, StateEntry> e : store.scanByPrefix(SESSION_ID, DEAD_PREFIX).entrySet()) {
            if (requeued >= limit || pendingCount() >= capacity) {
                break;
            }
            OutboxRecord dead = parse(e.getValue().value());
            store.delete(SESSION_ID, e.getKey());
            if (dead == null) {
                continue; // 损坏死信：丢弃（已在隔离期暴露过）
            }
            long now = System.currentTimeMillis();
            OutboxRecord revived = new OutboxRecord(dead.eventId(), dead.type(), dead.body(),
                    seq.incrementAndGet(), 0, now, dead.createdAtEpochMs());
            store.put(SESSION_ID, new StateEntry(OUTBOX_PREFIX + dead.eventId(),
                    toJson(revived), "webhook-outbox", 0, null, Instant.now()));
            store.put(SESSION_ID, indexEntry(dueKey(revived), dead.eventId()));
            requeued++;
        }
        return requeued;
    }

    // ---- spec 79 §A / T311：due-time 索引键工具 ----

    private static String dueKey(OutboxRecord record) {
        return DUE_PREFIX + pad(record.nextAttemptAtEpochMs()) + "." + record.eventId();
    }

    /** 16 位零垫十进制（字典序 = 数值序；epoch millis 13 位，3 位余量到 ~2286 年）。 */
    private static String pad(long epochMs) {
        return String.format("%016d", epochMs);
    }

    private static String eventIdOf(String indexKey) {
        String rest = indexKey.substring(DUE_PREFIX.length());
        return rest.substring(rest.indexOf('.') + 1);
    }

    private static StateEntry indexEntry(String dueKey, String eventId) {
        return new StateEntry(dueKey, eventId, "webhook-outbox", 0, null, Instant.now());
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
