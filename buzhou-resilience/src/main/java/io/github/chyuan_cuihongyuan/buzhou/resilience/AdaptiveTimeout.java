package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * EWMA 自适应超时推荐器（spec 1403 / T2107 / impl 1056）——Envoy timeout
 * budget / Finagle 自适应超时思想：固定超时要么太松（故障探测迟钝）要么太紧
 * （正常慢请求被误杀），超时应从<b>实测时延指数加权滑动平均</b>推导——
 * recent 观测权重高、历史逐渐遗忘，推荐值 = clamp(⌈EWMA×multiplier⌉, floor, ceiling)。
 *
 * <p>纯推导器：只从调用方喂入的观测样本计算推荐值，<b>不接线任何执行路径</b>
 * （超时执行归调用方既有机制——turnBudget/HookAdvisor 族；接线留装配轮）。
 * 预热哨兵：样本数不足 {@link #MIN_SAMPLES} 不下结论（BudgetRecommendation
 * 「小样本不推荐」先例）。无锁：ewma 用 CAS 参与式更新，样本计数原子。
 */
public final class AdaptiveTimeout {

    /** 默认平滑系数（0&lt;α≤1；0.3 = 新样本权重 30%，兼顾响应与抗噪）。 */
    public static final double DEFAULT_ALPHA = 0.3;
    /** 默认放大倍数（EWMA 是均值语义，超时须覆盖长尾——3× 起步）。 */
    public static final int DEFAULT_MULTIPLIER = 3;
    /** 预热最少样本数（不足不下结论）。 */
    public static final int MIN_SAMPLES = 3;

    private final double alpha;
    private final int multiplier;
    private final long floorMillis;
    private final long ceilingMillis;

    private final AtomicReference<double[]> ewmaHolder = new AtomicReference<>(null); // [ewma, samples]
    private final AtomicLong recommendations = new AtomicLong();

    /** 全参数构造：α、放大倍数、下限/上限夹取。 */
    public AdaptiveTimeout(double alpha, int multiplier, Duration floor, Duration ceiling) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha 须在 (0,1]：" + alpha);
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("multiplier 须 ≥1：" + multiplier);
        }
        this.alpha = alpha;
        this.multiplier = multiplier;
        this.floorMillis = floor.toMillis();
        this.ceilingMillis = ceiling.toMillis();
        if (floorMillis > ceilingMillis) {
            throw new IllegalArgumentException("floor 不得超过 ceiling：" + floor + " > " + ceiling);
        }
    }

    /** 默认参数构造：α=0.3、3×、下限 50ms、上限 60s。 */
    public AdaptiveTimeout() {
        this(DEFAULT_ALPHA, DEFAULT_MULTIPLIER, Duration.ofMillis(50), Duration.ofSeconds(60));
    }

    /** 喂入一次实测时延样本（毫秒，非负）；EWMA：e′ = α·x + (1−α)·e，首样本直接播种。 */
    public void record(long observedMillis) {
        if (observedMillis < 0) {
            throw new IllegalArgumentException("观测时延须非负：" + observedMillis);
        }
        ewmaHolder.updateAndGet(prev -> {
            if (prev == null) {
                return new double[]{observedMillis, 1};
            }
            double ewma = alpha * observedMillis + (1 - alpha) * prev[0];
            return new double[]{ewma, prev[1] + 1};
        });
    }

    /**
     * 当前推荐超时：样本不足返回 empty（预热哨兵）；否则
     * clamp(⌈EWMA×multiplier⌉, floor, ceiling)。
     */
    public Optional<Duration> recommended() {
        double[] state = ewmaHolder.get();
        if (state == null || state[1] < MIN_SAMPLES) {
            return Optional.empty();
        }
        long millis = clamp((long) Math.ceil(state[0] * multiplier));
        recommendations.incrementAndGet();
        return Optional.of(Duration.ofMillis(millis));
    }

    private long clamp(long millis) {
        return Math.max(floorMillis, Math.min(ceilingMillis, millis));
    }

    /** 只读快照：EWMA/样本数/当前推荐/参数与夹取边界（无副作用，不推进推荐计数）。 */
    public Snapshot stats() {
        double[] state = ewmaHolder.get();
        long ewmaMillis = state == null ? -1 : (long) state[0];
        long samples = state == null ? 0 : (long) state[1];
        long recMillis = -1;
        if (state != null && state[1] >= MIN_SAMPLES) {
            recMillis = clamp((long) Math.ceil(state[0] * multiplier));
        }
        return new Snapshot(ewmaMillis, samples, recMillis,
                floorMillis, ceilingMillis, multiplier, recommendations.get());
    }

    /** 测试归零口（EWMA/样本/推荐计数）。 */
    public void resetForTest() {
        ewmaHolder.set(null);
        recommendations.set(0);
    }

    /**
     * @param ewmaMillis      当前 EWMA（毫秒；无样本 = -1 哨兵）
     * @param samples         累计样本数
     * @param recommendedMillis 当前推荐超时（毫秒；预热期 = -1 哨兵）
     * @param floorMillis     夹取下限
     * @param ceilingMillis   夹取上限
     * @param multiplier      放大倍数
     * @param recommendationCount 累计产生推荐次数（预热期不计数）
     */
    public record Snapshot(long ewmaMillis, long samples, long recommendedMillis,
                           long floorMillis, long ceilingMillis, int multiplier,
                           long recommendationCount) {
    }
}
