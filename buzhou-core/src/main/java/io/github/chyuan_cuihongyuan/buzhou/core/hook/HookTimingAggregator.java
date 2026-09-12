package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

/**
 * hook 计时进程级聚合器（spec 647 / T944–T945）：跨会话（跨 HookChain 实例）
 * 按 hook 名聚合 count / totalNanos / maxNanos——「全进程哪个 hook 最慢、吃掉
 * 多少 Turn 内联预算」单一读面可答。HookChain 在聚合开启时镜像累计（链内
 * 私有 {@link HookChain#stats()} 口径不变）。
 *
 * <p>Holder 模式（RetryBudgetHolder / ToolTimeoutOverrides.Holder 同款）：
 * Spring 装配开启（{@code HookTimingHolder.enable()}）；编程式 / 未装配 =
 * 关（{@link HookChain} 纯私有计时，零变化）。
 */
public final class HookTimingAggregator {

    private final ConcurrentHashMap<String, Timing> timings = new ConcurrentHashMap<>();

    /** 累计一条（hook 名 → 纳秒）。 */
    public void record(String hookName, long nanos) {
        timings.computeIfAbsent(hookName, k -> new Timing()).record(nanos);
    }

    /** 聚合快照（hook 名 → count/total/max；不可变）。 */
    public Map<String, HookChain.HookTiming> stats() {
        Map<String, HookChain.HookTiming> out = new LinkedHashMap<>();
        timings.forEach((name, t) -> out.put(name,
                new HookChain.HookTiming(name, t.count.sum(), t.totalNanos.sum(), t.maxNanos)));
        return Map.copyOf(out);
    }

    /**
     * spec 708 / T967：滚动窗 max 快照（hook 名 → 纳秒；窗口内无样本 = 0——
     * 生命周期 max 永不衰减的误导面修正：「现在还慢不慢」可答）。不可变。
     */
    public Map<String, Long> windowedMax() {
        Map<String, Long> out = new LinkedHashMap<>();
        timings.forEach((name, t) -> out.put(name, t.windowedMax.max()));
        return Map.copyOf(out);
    }

    /** 与 HookChain.Timing 同构（无锁累计 + CAS max + 滚动窗 max）。 */
    private static final class Timing {
        final LongAdder count = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        final io.github.chyuan_cuihongyuan.buzhou.core.metrics.RollingMaxCounter windowedMax =
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.RollingMaxCounter();
        volatile long maxNanos;

        void record(long nanos) {
            count.increment();
            totalNanos.add(nanos);
            windowedMax.record(nanos);
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

    /** 进程级 Holder（装配开启；未开启 null——HookChain 纯私有）。 */
    public static final class Holder {
        private static volatile HookTimingAggregator current;

        private Holder() {
        }

        /** 开启进程级聚合（Spring 装配调用一次）。 */
        public static void enable() {
            if (current == null) {
                current = new HookTimingAggregator();
            }
        }

        /** 当前聚合器（未开启 = null——调用方判空跳过镜像，零开销）。 */
        public static HookTimingAggregator current() {
            return current;
        }

        /** 关闭（测试隔离用）。 */
        static void reset() {
            current = null;
        }
    }
}
