package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 空闲会话后台压缩（spec 310 / T611，RocksDB/LSM compaction 借鉴——空闲即
 * 后台维护窗口）：会话索引 lastActiveAt 事实 → 空闲超阈 {@code ACTIVE} 会话 →
 * 压缩动作（默认 {@link ManualCompactor}，注入 {@code compact}——与 compact_now
 * 同管线）。每轮批上限防压缩风暴；逐会话隔离失败；只动 ACTIVE（CLOSED 归
 * RetentionSweeper 保留策略族——不越界）。
 */
public final class IdleCompactionHousekeeper implements SmartLifecycle {

    /** 分页枚举上限（200/页——大舰队不全扫，空闲者多在活跃序尾）。 */
    static final int MAX_PAGES = 10;
    private static final int PAGE_SIZE = 200;

    private final SessionIndexStore index;
    private final Function<String, ManualCompactor.CompactResult> action;
    private final Duration idleThreshold;
    private final Duration interval;
    private final int maxPerSweep;
    /** spec 341 / T674：可选选主门（331 扩散——null = 零变化）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector elector;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong compacted = new AtomicLong();
    private final AtomicLong skipped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong foldedMessages = new AtomicLong();
    private volatile ScheduledExecutorService scheduler;

    /**
     * @param index         会话索引（事实源——lastActiveAt）
     * @param action        压缩动作（sessionId → CompactResult；装配传 {@code compactor::compact}）
     * @param idleThreshold 空闲阈值（正）
     * @param interval      sweep 周期（正）
     * @param maxPerSweep   每轮压缩上限（≥1）
     */
    public IdleCompactionHousekeeper(SessionIndexStore index,
                                     Function<String, ManualCompactor.CompactResult> action,
                                     Duration idleThreshold, Duration interval, int maxPerSweep) {
        this(index, action, idleThreshold, interval, maxPerSweep, null);
    }

    /**
     * spec 341 / T674：带选主门构造（331 扩散——调度周期先取续，非 leader
     * 跳周期留计数；手动 sweepOnce 不设门；stop 让位）。elector null =
     * 既有每实例各跑语义零变化。
     */
    public IdleCompactionHousekeeper(SessionIndexStore index,
                                     Function<String, ManualCompactor.CompactResult> action,
                                     Duration idleThreshold, Duration interval, int maxPerSweep,
                                     io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector elector) {
        if (index == null || action == null) {
            throw new IllegalArgumentException("index/action 必须非空");
        }
        if (idleThreshold == null || idleThreshold.isZero() || idleThreshold.isNegative()) {
            throw new IllegalArgumentException("idleThreshold 为正");
        }
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval 为正");
        }
        if (maxPerSweep < 1) {
            throw new IllegalArgumentException("maxPerSweep >= 1（当前 " + maxPerSweep + "）");
        }
        this.index = index;
        this.action = action;
        this.idleThreshold = idleThreshold;
        this.interval = interval;
        this.maxPerSweep = maxPerSweep;
        this.elector = elector;
    }

    /** 单轮：枚举 ACTIVE（分页上限内）→ 空闲候选（最久优先）→ 限批压缩。返回本轮压缩数。 */
    public int sweepOnce(Instant now) {
        List<SessionInfo> candidates = idleCandidates(now);
        int done = 0;
        for (SessionInfo candidate : candidates) {
            if (done >= maxPerSweep) {
                break;
            }
            try {
                ManualCompactor.CompactResult result = action.apply(candidate.sessionId());
                if (result != null && result.skipped()) {
                    skipped.incrementAndGet();
                    BuzhouMetricsHolder.metrics().counter("buzhou.idle-compaction.skipped", 1);
                } else {
                    compacted.incrementAndGet();
                    BuzhouMetricsHolder.metrics().counter("buzhou.idle-compaction.compacted", 1);
                    if (result != null) {
                        foldedMessages.addAndGet(result.foldedMessages());
                        BuzhouMetricsHolder.metrics()
                                .counter("buzhou.idle-compaction.folded-messages", result.foldedMessages());
                    }
                }
                done++;
            } catch (RuntimeException e) {
                failed.incrementAndGet(); // 逐会话隔离——一个失败不停轮
                BuzhouMetricsHolder.metrics().counter("buzhou.idle-compaction.failed", 1);
            }
        }
        return done;
    }

    /** 空闲候选（lastActiveAt 倒序枚举翻页，收集最久空闲优先——list 序最旧在尾，倒收）。 */
    private List<SessionInfo> idleCandidates(Instant now) {
        long cutoffEpochMs = now.toEpochMilli() - idleThreshold.toMillis();
        List<SessionInfo> idle = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            List<SessionInfo> batch = index.list(new SessionIndexQuery(null, null,
                    SessionInfo.STATUS_ACTIVE, null, null, page * PAGE_SIZE, PAGE_SIZE));
            batch.stream()
                    .filter(info -> info.lastActiveAtEpochMs() <= cutoffEpochMs)
                    .forEach(idle::add);
            if (batch.size() < PAGE_SIZE || idle.size() >= maxPerSweep * 2L) {
                break; // 枚举尽或候选已足（×2 余量——排序后仍取最久的 maxPerSweep 个）
            }
        }
        idle.sort(java.util.Comparator.comparingLong(SessionInfo::lastActiveAtEpochMs));
        // lastActive 升序 = 最久空闲在前——直接取前 maxPerSweep 个（最久优先）
        return idle.size() <= maxPerSweep ? idle : List.copyOf(idle.subList(0, maxPerSweep));
    }

    // ---- SmartLifecycle：周期调度 ----

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    BuzhouThreadFactory.platform("buzhou-idle-compaction"));
            scheduler.scheduleAtFixedRate(this::gatedSweep,
                    interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** spec 341 / T674：调度周期先取续——非 leader 跳周期留计数（手动 sweepOnce 不设门）。 */
    private void gatedSweep() {
        try {
            if (elector != null && !elector.tryAcquireOrRenew().leader()) {
                BuzhouMetricsHolder.metrics()
                        .counter("buzhou.idle-compaction.skipped-not-leader", 1);
                return;
            }
            sweepOnce(Instant.now());
        } catch (RuntimeException e) {
            // 逐会话失败已隔离；此处只防调度线程被意外异常杀死（331 同法）
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ScheduledExecutorService current = scheduler;
            if (current != null) {
                current.shutdownNow();
                scheduler = null;
            }
            if (elector != null) {
                try {
                    elector.resign(); // spec 341：停机让位——快速故障转移
                } catch (RuntimeException ignored) {
                    // TTL 自然过期兜底
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    // ---- 观测面 ----

    public long compactedCount() {
        return compacted.get();
    }

    public long skippedCount() {
        return skipped.get();
    }

    public long failedCount() {
        return failed.get();
    }

    public long foldedMessagesTotal() {
        return foldedMessages.get();
    }
}
