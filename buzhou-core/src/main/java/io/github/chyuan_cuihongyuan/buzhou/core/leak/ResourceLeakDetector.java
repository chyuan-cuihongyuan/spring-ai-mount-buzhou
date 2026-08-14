package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;
import java.time.Duration;

/**
 * 资源泄漏检测器（impl-41 / spec 13 §T66，源 Netty ResourceLeakDetector 四级采样）：
 * 挂在<b>会话资源 / spill 句柄 / 租约</b>三处——出租（acquire）时
 * {@link #track(String)} 领取 {@link LeakHandle}，释放时 {@link LeakHandle#close()}；
 * 句柄在<b>未关闭情况下被 GC</b> 即判定泄漏嫌疑，经 {@link LeakListener} 上报
 * （WARN 日志 + {@link #leaksReported()} 计数）。
 *
 * <ul>
 *   <li><b>DISABLED</b>：不追踪（零开销）；</li>
 *   <li><b>SIMPLE</b>（默认）：1/128 采样，无栈捕获；</li>
 *   <li><b>ADVANCED</b>：1/128 采样 + 出租时栈捕获（报告可定位出租点）；</li>
 *   <li><b>PARANOID</b>：全量采样 + 栈捕获（压测/排障用，开销最大）。</li>
 * </ul>
 *
 * <p><b>出租时长阈值</b>（{@code leaseAgeThreshold}，默认 PT5M）：GC 上报时年龄低于阈值的
 * 跳过（抑制「构造后立刻丢弃的测试临时对象」噪声）。Cleaner 单守护线程全局共享；
 * 检测器本身不持任何资源强引用（句柄即检测对象，close 即解除登记）。
 */
public final class ResourceLeakDetector {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceLeakDetector.class);

    /** 采样分母（Netty 同款 1/128：泄漏发生时有 ~63% 至少一次命中采样）。 */
    static final int SAMPLING_INTERVAL = 128;

    private static final Cleaner CLEANER = Cleaner.create();

    public enum LeakLevel {
        DISABLED, SIMPLE, ADVANCED, PARANOID
    }

    /** 泄漏上报（WARN 日志之外的业务钩子：指标/事件面）。 */
    @FunctionalInterface
    public interface LeakListener {
        void onLeak(LeakReport report);
    }

    /** 泄漏报告：描述、GC 时年龄（毫秒）、出租点栈（ADVANCED/PARANOID 且采样命中）。 */
    public record LeakReport(String description, long ageMillis, String acquisitionStack) {
    }

    /** 出租登记句柄：释放时必须 close；被 GC 而未 close = 泄漏嫌疑（采样命中即上报）。 */
    public static final class LeakHandle implements AutoCloseable {

        static final LeakHandle NOOP = new LeakHandle(null);

        private final Registration registration;

        private LeakHandle(Registration registration) {
            this.registration = registration;
        }

        @Override
        public void close() {
            if (registration == null) {
                return;
            }
            // closed 先置（可见性）：close 执行中句柄必可达，Cleaner 不可能并发结算
            registration.closed = true;
            if (registration.settle()) {
                registration.activeCounter.decrementAndGet();
            }
        }
    }

    /** Cleaner 侧登记项（句柄可达性消失后由 Cleaner 线程读取）。 */
    private static final class Registration {

        final String description;
        final boolean sampled;
        final long leasedAtNanos;
        final String acquisitionStack;
        final java.util.concurrent.atomic.AtomicLong activeCounter;
        /** close（显式释放）与 Cleaner（GC 回收）双路径一次性结算。 */
        final java.util.concurrent.atomic.AtomicBoolean settled =
                new java.util.concurrent.atomic.AtomicBoolean();
        volatile boolean closed;

        Registration(String description, boolean sampled, long leasedAtNanos,
                String acquisitionStack,
                java.util.concurrent.atomic.AtomicLong activeCounter) {
            this.description = description;
            this.sampled = sampled;
            this.leasedAtNanos = leasedAtNanos;
            this.acquisitionStack = acquisitionStack;
            this.activeCounter = activeCounter;
        }

        /** 首次结算返回 true（close 或 Cleaner 恰一扣减 activeHandles）。 */
        boolean settle() {
            return settled.compareAndSet(false, true);
        }
    }

    /** Cleaner 回调（静态类：不持检测器强引用，仅经计数器/listener 引用回传）。 */
    private static final class Cleanup implements Runnable {

        private final Registration registration;
        private final LeakLevel level;
        private final long thresholdMillis;
        private final LeakListener listener;
        private final java.util.concurrent.atomic.AtomicLong reportedCounter;
        private final java.util.concurrent.atomic.AtomicLong activeCounter;

        Cleanup(Registration registration, LeakLevel level, long thresholdMillis,
                LeakListener listener,
                java.util.concurrent.atomic.AtomicLong reportedCounter,
                java.util.concurrent.atomic.AtomicLong activeCounter) {
            this.registration = registration;
            this.level = level;
            this.thresholdMillis = thresholdMillis;
            this.listener = listener;
            this.reportedCounter = reportedCounter;
            this.activeCounter = activeCounter;
        }

        @Override
        public void run() {
            if (registration.settle()) {
                activeCounter.decrementAndGet();
            }
            if (registration.closed || !registration.sampled
                    || level == LeakLevel.DISABLED) {
                return; // 已显式 close 的不算泄漏
            }
            long ageMillis = (System.nanoTime() - registration.leasedAtNanos) / 1_000_000;
            if (ageMillis < thresholdMillis) {
                return; // 出租时长阈值内的 GC 视为噪声
            }
            LeakReport report = new LeakReport(registration.description, ageMillis,
                    registration.acquisitionStack);
            LOG.warn("buzhou 资源泄漏嫌疑（未释放即被 GC）：{}，出租时长 {}ms{}",
                    report.description(), ageMillis,
                    report.acquisitionStack() == null ? ""
                            : "\n出租点栈：\n" + report.acquisitionStack());
            reportedCounter.incrementAndGet();
            if (listener != null) {
                try {
                    listener.onLeak(report);
                } catch (RuntimeException e) {
                    LOG.error("LeakListener 异常（忽略，不影响 Cleaner 线程）", e);
                }
            }
        }
    }

    private final LeakLevel level;
    private final long leaseAgeThresholdMillis;
    private final LeakListener listener;
    private final java.util.concurrent.atomic.AtomicLong leaksReported =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong trackCounter =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong activeHandles =
            new java.util.concurrent.atomic.AtomicLong();

    public ResourceLeakDetector() {
        this(LeakLevel.SIMPLE, Duration.ofMinutes(5), null);
    }

    public ResourceLeakDetector(LeakLevel level, Duration leaseAgeThreshold,
            LeakListener listener) {
        this.level = level == null ? LeakLevel.SIMPLE : level;
        this.leaseAgeThresholdMillis = leaseAgeThreshold == null || leaseAgeThreshold.isNegative()
                ? Duration.ofMinutes(5).toMillis() : leaseAgeThreshold.toMillis();
        this.listener = listener;
    }

    /** 出租登记（description 建议 {@code <类型>:<id>} 形式，如 {@code lease:s1}）。 */
    public LeakHandle track(String description) {
        if (level == LeakLevel.DISABLED) {
            return LeakHandle.NOOP;
        }
        trackCounter.incrementAndGet();
        activeHandles.incrementAndGet();
        boolean sampled;
        if (level == LeakLevel.PARANOID) {
            sampled = true;
        } else {
            // 1/128 采样（SIMPLE/ADVANCED）
            sampled = trackCounter.get() % SAMPLING_INTERVAL == 0;
        }
        String stack = level == LeakLevel.ADVANCED || level == LeakLevel.PARANOID
                ? captureStack() : null;
        Registration registration = new Registration(description, sampled,
                System.nanoTime(), stack, activeHandles);
        LeakHandle handle = new LeakHandle(registration);
        CLEANER.register(handle, new Cleanup(registration, level,
                leaseAgeThresholdMillis, listener, leaksReported, activeHandles));
        return handle;
    }

    private static String captureStack() {
        StringBuilder out = new StringBuilder();
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        // 跳过 VM/本类帧，取前 8 帧
        int printed = 0;
        for (StackTraceElement element : elements) {
            String cls = element.getClassName();
            if (cls.equals(ResourceLeakDetector.class.getName())
                    || cls.startsWith("java.lang.Thread")) {
                continue;
            }
            out.append("\tat ").append(element).append('\n');
            if (++printed >= 8) {
                break;
            }
        }
        return out.toString();
    }

    public LeakLevel level() {
        return level;
    }

    /** 累计上报的泄漏嫌疑数（健康/指标面）。 */
    public long leaksReported() {
        return leaksReported.get();
    }

    /** 当前活跃（出租未释放）句柄数（近似——Cleaner 异步扣减）。 */
    public long activeHandles() {
        return activeHandles.get();
    }

    /** 累计出租登记数。 */
    public long tracked() {
        return trackCounter.get();
    }
}
