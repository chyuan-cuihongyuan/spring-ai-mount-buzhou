package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 指标新鲜度跟踪装饰器（spec 802 / T1105，Prometheus staleness 借鉴）：
 * 包装任意 {@link BuzhouMetrics}——counter/timer 每次写入刷新指标名级
 * 「最后写入时刻」；{@link #audit(long, long)} 报告超过 staleAfterMillis 未
 * 写入的指标（按年龄降序）——「某机制为何不再有数」从猜变查（序列静默 =
 * 写路径死亡或装配丢失的信号）。
 *
 * <p><b>gauge 不追踪</b>（连续量无「写入」语义——注册即持续采样）；名字级
 * 而非 series 级（tag 组合爆炸——诚实折中）；名字封顶 {@value #MAX_NAMES}
 * （超限不再记、truncated 如实——基数有界纪律）。装饰器零委托变更，纯旁路。
 */
public final class MetricFreshnessTracker implements BuzhouMetrics {

    /** 追踪指标名封顶。 */
    public static final int MAX_NAMES = 512;
    /** 陈旧清单封顶（audit 输出侧）。 */
    public static final int STALE_LIST_LIMIT = 64;

    /** 单条陈旧读数。 */
    public record StaleMetric(String name, long lastWriteMillis, long ageMillis) {
    }

    /** 不可变新鲜度报告。 */
    public record FreshnessReport(List<StaleMetric> stale, int trackedNames, int limit,
                                  long staleAfterMillis, boolean truncated) {
    }

    private final BuzhouMetrics delegate;
    private final Clock clock;
    private final Map<String, Long> lastWrites = new ConcurrentHashMap<>();
    private final AtomicBoolean truncated = new AtomicBoolean(false);

    public MetricFreshnessTracker(BuzhouMetrics delegate) {
        this(delegate, Clock.systemUTC());
    }

    public MetricFreshnessTracker(BuzhouMetrics delegate, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void counter(String name, long delta, String... tagKeyValue) {
        touch(name);
        delegate.counter(name, delta, tagKeyValue);
    }

    @Override
    public void timer(String name, java.time.Duration duration, String... tagKeyValue) {
        touch(name);
        delegate.timer(name, duration, tagKeyValue);
    }

    /** gauge 不追踪（连续量无写入语义——见类注）。 */
    @Override
    public void gauge(String name, java.util.function.Supplier<Number> value, String... tagKeyValue) {
        delegate.gauge(name, value, tagKeyValue);
    }

    private void touch(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (!lastWrites.containsKey(name) && lastWrites.size() >= MAX_NAMES) {
            truncated.set(true);
            return; // 名字封顶：不再追踪新名
        }
        lastWrites.put(name, clock.millis());
    }

    /**
     * 新鲜度审计：距 now 未写入超 staleAfterMillis 的指标按年龄降序
     * （封顶 {@value #STALE_LIST_LIMIT}）；staleAfterMillis &lt; 1 fail-fast。
     */
    public FreshnessReport audit(long nowMillis, long staleAfterMillis) {
        if (staleAfterMillis < 1) {
            throw new IllegalArgumentException("staleAfterMillis 必须 >= 1，实际 " + staleAfterMillis);
        }
        List<StaleMetric> stale = new ArrayList<>();
        for (Map.Entry<String, Long> e : lastWrites.entrySet()) {
            long age = nowMillis - e.getValue();
            if (age > staleAfterMillis) {
                stale.add(new StaleMetric(e.getKey(), e.getValue(), age));
            }
        }
        stale.sort(Comparator.comparingLong(StaleMetric::ageMillis).reversed());
        List<StaleMetric> limited = stale.size() > STALE_LIST_LIMIT
                ? List.copyOf(stale.subList(0, STALE_LIST_LIMIT)) : List.copyOf(stale);
        return new FreshnessReport(limited, lastWrites.size(), STALE_LIST_LIMIT,
                staleAfterMillis, truncated.get());
    }

    /** 已追踪的最后写入时刻只读视图（诊断辅助）。 */
    public Map<String, Long> lastWrites() {
        return Map.copyOf(lastWrites);
    }
}
