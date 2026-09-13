package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 预算用量分位推荐读数（spec 806 / T1113，k8s VPA 借鉴——按观测用量分位
 * 推荐 resource requests）：对用量样本（如会话累计 tokens / 单轮成本微美元）
 * 计算 P50/P95/P99 + 按目标水位（headroom%）推荐预算档——「预算该设多少」
 * 从拍脑袋变分位推导。
 *
 * <p>纯函数（{@link #recommend(List, int)}）：最近秩百分位（确定性）；
 * 推荐值 = ⌈P95 × (1 + headroom/100)⌉。样本 &lt; {@value #MIN_SAMPLES} 时
 * {@code sufficient=false}（推荐位为 -1 哨兵——样本不足不下结论，诚实折中）。
 * {@link Ring} 有界收集器（封顶 {@value Ring#CAPACITY}，FIFO 滑窗）供喂样。
 */
public final class BudgetRecommendation {

    /** 视为足够的最小样本数。 */
    public static final int MIN_SAMPLES = 5;

    private BudgetRecommendation() {
    }

    /** 推荐报告（不可变；sufficient=false 时 recommended=-1）。 */
    public record Report(int samples, long p50, long p95, long p99, long max,
                         long recommended, int headroomPercent, boolean sufficient) {
    }

    /**
     * 分位推荐：样本 null/负值忽略；headroom 百分比 0..500 合法域外 fail-fast。
     * 空样本 → 全 0 + insufficient。
     */
    public static Report recommend(List<Long> samples, int headroomPercent) {
        if (headroomPercent < 0 || headroomPercent > 500) {
            throw new IllegalArgumentException("headroomPercent 须在 0..500，实际 " + headroomPercent);
        }
        List<Long> clean = samples == null ? List.of()
                : samples.stream().filter(v -> v != null && v >= 0).sorted().toList();
        int n = clean.size();
        if (n < MIN_SAMPLES) {
            return new Report(n, 0, 0, 0, n == 0 ? 0 : clean.get(n - 1), -1, headroomPercent, false);
        }
        long p50 = nearestRank(clean, 50);
        long p95 = nearestRank(clean, 95);
        long p99 = nearestRank(clean, 99);
        long recommended = (long) Math.ceil(p95 * (1.0 + headroomPercent / 100.0));
        return new Report(n, p50, p95, p99, clean.get(n - 1), recommended, headroomPercent, true);
    }

    /** 最近秩百分位（sorted 升序；rank=⌈p/100·n⌉）。 */
    private static long nearestRank(List<Long> sorted, int p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(Math.max(rank, 1), sorted.size()) - 1);
    }

    /** 有界 FIFO 样本环（线程安全；满则挤最老）。 */
    public static final class Ring {
        /** 环容量。 */
        public static final int CAPACITY = 1024;

        private final long[] buffer = new long[CAPACITY];
        private int size;
        private int head;
        private final AtomicLong dropped = new AtomicLong();

        /** 记录一个样本（负值忽略）。 */
        public synchronized void record(long value) {
            if (value < 0) {
                return;
            }
            if (size < CAPACITY) {
                buffer[(head + size) % CAPACITY] = value;
                size++;
            } else {
                buffer[head] = value;
                head = (head + 1) % CAPACITY;
                dropped.incrementAndGet();
            }
        }

        /** 当前样本只读快照（旧→新）。 */
        public synchronized List<Long> snapshot() {
            List<Long> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                out.add(buffer[(head + i) % CAPACITY]);
            }
            return out;
        }

        public synchronized int size() {
            return size;
        }

        public long dropped() {
            return dropped.get();
        }
    }
}
