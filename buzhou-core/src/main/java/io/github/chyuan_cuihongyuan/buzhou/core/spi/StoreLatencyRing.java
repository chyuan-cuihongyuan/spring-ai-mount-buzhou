package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储提交延迟环形读数（spec 810 / T1121，etcd backend commit latency 与
 * pg_stat_statements 慢查询聚合思想）：按操作名记录最近 N 次耗时毫秒
 * （FIFO 环），读数 count/total/max + 最近秩 P50/P95——存储写慢从「感觉」
 * 变样本环。喂点：{@link TimedMessageStore} 装饰器或装配侧手动
 * {@link #record(String, long)}；操作名封顶 {@value #MAX_OPS}（truncated 如实），
 * 单环容量 {@value #RING_CAPACITY}。纯内存读数面——不改任何存储行为。
 */
public final class StoreLatencyRing {

    /** 单操作环容量。 */
    public static final int RING_CAPACITY = 128;
    /** 操作名封顶。 */
    public static final int MAX_OPS = 16;

    /** 单操作读数（不可变）。 */
    public record OpStats(String op, long count, long totalMillis, long maxMillis,
                          long p50Millis, long p95Millis, boolean ringFull) {
    }

    private static final class Ring {
        final long[] buffer = new long[RING_CAPACITY];
        int size;
        int head;
        long total;
        long max;
    }

    private final Map<String, Ring> rings = new ConcurrentHashMap<>();
    private volatile boolean truncated;
    private final AtomicLong recorded = new AtomicLong();

    /**
     * 记录一次操作耗时（毫秒；负值忽略；操作名 null/空白忽略；操作名超封顶
     * 置 truncated 不再记新名）。
     */
    public void record(String op, long millis) {
        if (op == null || op.isBlank() || millis < 0) {
            return;
        }
        recorded.incrementAndGet();
        Ring ring = rings.get(op);
        if (ring == null) {
            synchronized (rings) {
                if (rings.size() >= MAX_OPS && !rings.containsKey(op)) {
                    truncated = true;
                    return;
                }
                ring = rings.computeIfAbsent(op, k -> new Ring());
            }
        }
        synchronized (ring) {
            if (ring.size < RING_CAPACITY) {
                ring.buffer[(ring.head + ring.size) % RING_CAPACITY] = millis;
                ring.size++;
            } else {
                ring.buffer[ring.head] = millis;
                ring.head = (ring.head + 1) % RING_CAPACITY;
            }
            ring.total += millis;
            ring.max = Math.max(ring.max, millis);
        }
    }

    /** 单操作读数（未知操作返回 null）。 */
    public OpStats stats(String op) {
        Ring ring = rings.get(op);
        if (ring == null) {
            return null;
        }
        List<Long> samples = new ArrayList<>(ring.size);
        synchronized (ring) {
            for (int i = 0; i < ring.size; i++) {
                samples.add(ring.buffer[(ring.head + i) % RING_CAPACITY]);
            }
        }
        samples.sort(Long::compare);
        return new OpStats(op, samples.size(), ring.total, ring.max,
                nearestRank(samples, 50), nearestRank(samples, 95), ring.size >= RING_CAPACITY);
    }

    /** 全操作读数（按操作名典序）。 */
    public List<OpStats> all() {
        return rings.keySet().stream().sorted().map(this::stats).toList();
    }

    /** 累计记录条数（含被截断丢弃的操作名首笔）。 */
    public long recorded() {
        return recorded.get();
    }

    public boolean truncated() {
        return truncated;
    }

    private static long nearestRank(List<Long> sorted, int p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(Math.max(rank, 1), sorted.size()) - 1);
    }

    /** 参数校验辅助（防误用 null 环构造装饰器）。 */
    static void requireNonNull(Object o, String name) {
        Objects.requireNonNull(o, name);
    }
}
