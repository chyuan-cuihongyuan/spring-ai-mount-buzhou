package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 事件去重抑制器（spec 203 / T575，发射方防线）：按
 * sha256(type + 键排序 payload JSON) 指纹环形（默认 1024）拦截<b>完全相同</b>
 * 的重复事件——重复丢弃计 {@code buzhou.event.deduped}，首见透传入环；
 * 环形滚出后同事件可再过（「最近 N 个」窗）。与 fanout 组合 = 全站入站去重。
 */
public final class EventDeduplicator implements SessionEventListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEDUPED_COUNTER = "buzhou.event.deduped";

    private final SessionEventListener delegate;
    private final int capacity;
    private final java.util.ArrayDeque<String> ring = new java.util.ArrayDeque<>();
    private final java.util.HashSet<String> members = new java.util.HashSet<>();

    public EventDeduplicator(SessionEventListener delegate) {
        this(delegate, 1024);
    }

    public EventDeduplicator(SessionEventListener delegate, int capacity) {
        if (delegate == null || capacity < 1) {
            throw new IllegalArgumentException("delegate 非空、capacity>=1");
        }
        this.delegate = delegate;
        this.capacity = capacity;
    }

    // spec 1448 / T2195：去重聚合读面（metrics counter 之外的进程内累计——
    // passed/deduped 比率是重复事件注入压力的直接信号）
    private final java.util.concurrent.atomic.AtomicLong passedCount =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong dedupedCount =
            new java.util.concurrent.atomic.AtomicLong();

    @Override
    public synchronized void onEvent(SessionEvent event) {
        String fingerprint = fingerprint(event);
        if (members.contains(fingerprint)) {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter(DEDUPED_COUNTER, 1);
            dedupedCount.incrementAndGet();
            return; // 重复不出门
        }
        ring.addLast(fingerprint);
        members.add(fingerprint);
        if (ring.size() > capacity) {
            members.remove(ring.removeFirst()); // 最老滚出
        }
        passedCount.incrementAndGet();
        delegate.onEvent(event);
    }

    /** 只读快照：放行/去重计数 + 环占用（守恒 seen = passed + deduped）。 */
    public DeduplicationStats deduplicationStats() {
        return new DeduplicationStats(passedCount.get(), dedupedCount.get(),
                ring.size(), capacity);
    }

    /** 测试归零口（环/成员状态随语义保留，仅计数清零）。 */
    public void resetStatsForTest() {
        passedCount.set(0);
        dedupedCount.set(0);
    }

    /**
     * @param passed   放行（转发 delegate）事件数
     * @param deduped  被去重拦截的重复事件数
     * @param ringSize 当前指纹环占用（≤ capacity）
     * @param capacity 指纹环容量
     */
    public record DeduplicationStats(long passed, long deduped, int ringSize, int capacity) {

        /** 去重率 = deduped/(passed+deduped)（0 总量 -1 哨兵）。 */
        public double deduplicationRatio() {
            long total = passed + deduped;
            return total == 0 ? -1d : (double) deduped / total;
        }
    }

    /** 指纹：type + 键排序 payload JSON 的 sha256（Map 序不定等价同指纹）。 */
    private static String fingerprint(SessionEvent event) {
        Map<String, Object> sorted = new TreeMap<>(event.payload());
        try {
            String canonical = event.type() + ":" + MAPPER.writeValueAsString(sorted);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unhashed:" + event.type() + ":" + sorted.hashCode();
        }
    }
}
