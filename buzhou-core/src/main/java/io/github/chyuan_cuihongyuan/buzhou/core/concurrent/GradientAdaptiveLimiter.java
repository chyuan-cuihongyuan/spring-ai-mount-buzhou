package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 梯度式自适应并发闸（spec 1617 / T2385，Netflix concurrency-limits Gradient2 /
 * Envoy adaptive_concurrency 思想）：以<b>延迟梯度</b>驱动并发上限——近期延迟 EMA
 * 相对基线 EMA 劣化即乘性下调（过载前兆先于失败），明显变快即加性上调（探测余量）。
 * 与 {@link AdaptiveBulkhead}（失败驱动 AIMD，spec 145）正交并存：那只看成败、
 * 这只看延迟——失败前规避。
 *
 * <p><b>调整语义（钉住）</b>：gradient = baselineEma / recentEma（长窗慢 EMA 为基线、
 * 短窗快 EMA 为近期；warmup {@value #WARMUP_SAMPLES} 样本只学习不调整）：
 * <ul>
 *   <li>gradient ≥ 1 + tolerance（明显变快）→ limit + 1（加性上涨，封顶 maxLimit）</li>
 *   <li>gradient ≤ 1 − tolerance（明显劣化）→ limit × gradient 积性下调（地板 minLimit）</li>
 *   <li>容错带内不动（防抖——tolerance 默认 {@value #DEFAULT_TOLERANCE_PCT}%）</li>
 * </ul>
 * 无等待语义：tryAcquire 超当前上限 fail-fast（信号质量优先，与 AdaptiveBulkhead 同词汇）。
 */
public final class GradientAdaptiveLimiter {

    /** 默认容错带（20% = gradient 偏离 1 超两成才调整）。 */
    public static final double DEFAULT_TOLERANCE_PCT = 0.20;
    /** 学习期样本数（只喂 EMA 不调整）。 */
    public static final int WARMUP_SAMPLES = 8;
    /** 短窗 EMA 系数（快响应）。 */
    static final double RECENT_ALPHA = 0.4;
    /** 长窗 EMA 系数（慢基线）。 */
    static final double BASELINE_ALPHA = 0.05;

    /** 配置：下限/上限/容错带。 */
    public record Config(int minLimit, int maxLimit, double tolerance) {
        public Config {
            if (minLimit < 1 || maxLimit < minLimit) {
                throw new IllegalArgumentException("梯度配置非法（minLimit>=1、maxLimit>=minLimit）");
            }
            if (!(tolerance > 0 && tolerance < 1)) {
                throw new IllegalArgumentException("tolerance 必须在 (0,1)（当前 " + tolerance + "）");
            }
        }

        public static Config defaults() {
            return new Config(4, 64, DEFAULT_TOLERANCE_PCT);
        }
    }

    /** 观测行：动态上限 / 梯度 / 两窗 EMA / 调整次数。 */
    public record View(int limit, int inFlight, double recentEmaMillis, double baselineEmaMillis,
                       double gradient, long adjustmentsUp, long adjustmentsDown) {
    }

    private final Config config;
    private final AtomicInteger limit;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong samples = new AtomicLong();
    private final AtomicLong adjustmentsUp = new AtomicLong();
    private final AtomicLong adjustmentsDown = new AtomicLong();
    private volatile double recentEma;
    private volatile double baselineEma;

    public GradientAdaptiveLimiter(Config config) {
        this.config = config;
        this.limit = new AtomicInteger(config.minLimit());
    }

    /** 当前动态上限。 */
    public int limit() {
        return limit.get();
    }

    /** 超上限 fail-fast（true = 放行；配对 {@link #release()}）。 */
    public boolean tryAcquire() {
        while (true) {
            int current = inFlight.get();
            if (current >= limit.get()) {
                return false;
            }
            if (inFlight.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /** 归还占用（与 tryAcquire 配对）。 */
    public void release() {
        inFlight.decrementAndGet();
    }

    /**
     * 喂一次完成延迟（毫秒）并调整上限（warmup 后每样本评估）。
     * latency ≤ 0 忽略（无意义样本不污染 EMA）。
     */
    public void record(long latencyMillis) {
        if (latencyMillis <= 0) {
            return;
        }
        long n = samples.incrementAndGet();
        synchronized (this) {
            if (n == 1) {
                recentEma = latencyMillis;
                baselineEma = latencyMillis;
            } else {
                recentEma += RECENT_ALPHA * (latencyMillis - recentEma);
                baselineEma += BASELINE_ALPHA * (latencyMillis - baselineEma);
            }
            if (n <= WARMUP_SAMPLES) {
                return; // 学习期不调整
            }
            double gradient = recentEma <= 0 ? 1.0 : baselineEma / recentEma;
            if (gradient >= 1 + config.tolerance()) {
                limit.updateAndGet(v -> Math.min(config.maxLimit(), v + 1));
                adjustmentsUp.incrementAndGet();
            } else if (gradient <= 1 - config.tolerance()) {
                int scaled = (int) Math.floor(limit.get() * gradient);
                limit.updateAndGet(v -> Math.max(config.minLimit(), Math.max(scaled, v - 1)));
                adjustmentsDown.incrementAndGet();
            }
        }
    }

    /** 观测快照。 */
    public View view() {
        synchronized (this) {
            double gradient = recentEma <= 0 ? 1.0 : baselineEma / recentEma;
            return new View(limit.get(), inFlight.get(), recentEma, baselineEma, gradient,
                    adjustmentsUp.get(), adjustmentsDown.get());
        }
    }
}
