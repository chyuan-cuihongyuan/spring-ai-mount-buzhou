package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

/**
 * 工具执行耗时进程级聚合器（spec 700 / T951–T952）：跨会话按工具名聚合
 * count / totalNanos / maxNanos / failed——「全进程哪个工具吃掉最多工具耗时、
 * 换掉哪个工具能救回延迟」单一读面可答。HookedToolCallback 统一执行点在
 * 聚合开启时镜像累计（复用既有 spec 108 计时窗口，零额外开销）。
 *
 * <p>借鉴 PostgreSQL {@code pg_stat_statements}（语句级耗时聚合 top 读面）/
 * ClickHouse query log 思想；与 hook 侧 {@code HookTimingAggregator}（spec 647）
 * 完全同构。不进 micrometer——per-tool tag 违反 tag 基数守卫（spec 111）。
 *
 * <p>Holder 模式（HookTimingAggregator.Holder / RetryBudgetHolder 同款）：
 * Spring 装配开启（{@code Holder.enable()}）；编程式 / 未装配 = 关
 * （HookedToolCallback 判空跳过镜像，零行为变化）。
 */
public final class ToolTimingAggregator {

    private final ConcurrentHashMap<String, Timing> timings = new ConcurrentHashMap<>();

    /** 累计一条（工具名 → 纳秒；failed = 错误即反馈路径）。 */
    public void record(String toolName, long nanos, boolean failed) {
        timings.computeIfAbsent(toolName, k -> new Timing()).record(nanos, failed);
    }

    /** 聚合快照（工具名 → count/total/max/failed；不可变，保持首见序——读面稳定）。 */
    public Map<String, ToolTiming> stats() {
        Map<String, ToolTiming> out = new LinkedHashMap<>();
        timings.forEach((name, t) -> out.put(name,
                new ToolTiming(name, t.count.sum(), t.totalNanos.sum(), t.maxNanos, t.failed.sum())));
        return java.util.Collections.unmodifiableMap(out);
    }

    /** 单工具聚合行（不可变）。 */
    public record ToolTiming(String toolName, long count, long totalNanos,
                             long maxNanos, long failed) {

        /** 平均纳秒（count=0 防除零 → 0）。 */
        public long avgNanos() {
            return count == 0 ? 0 : totalNanos / count;
        }
    }

    /** 无锁累计 + CAS max（HookTimingAggregator.Timing 同构）。 */
    private static final class Timing {
        final LongAdder count = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        final LongAdder failed = new LongAdder();
        volatile long maxNanos;

        void record(long nanos, boolean isFailed) {
            count.increment();
            totalNanos.add(nanos);
            if (isFailed) {
                failed.increment();
            }
            long observed = maxNanos;
            long currentMax;
            do {
                currentMax = observed;
                if (nanos <= currentMax) {
                    break;
                }
            } while (!MAX_UPDATER.compareAndSet(this, currentMax, nanos));
        }

        private static final AtomicLongFieldUpdater<Timing> MAX_UPDATER =
                AtomicLongFieldUpdater.newUpdater(Timing.class, "maxNanos");
    }

    /** 进程级 Holder（装配开启；未开启 null——HookedToolCallback 判空跳过镜像）。 */
    public static final class Holder {
        private static volatile ToolTimingAggregator current;

        private Holder() {
        }

        /** 开启进程级聚合（Spring 装配调用一次）。 */
        public static void enable() {
            if (current == null) {
                current = new ToolTimingAggregator();
            }
        }

        /** 当前聚合器（未开启 = null——调用方判空跳过镜像，零开销）。 */
        public static ToolTimingAggregator current() {
            return current;
        }

        /** 关闭（测试隔离用）。 */
        static void reset() {
            current = null;
        }
    }
}
