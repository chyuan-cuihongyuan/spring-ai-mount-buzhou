package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SLO 错误预算燃尽率（spec 321 / T633，Google SRE workbook 错误预算借鉴）：
 * 桶环时间窗记 scope 成败，burnRate = 窗错误率/(1−SLO)——「额度正以几倍速
 * 烧」的 SRE 语言。breaching = burn ≥ 阈值<b>且</b>样本 ≥ min-samples（一败
 * 100% 的噪声不触发）。惰性旋转（访问路径顺带扫过期桶——无定时线程，
 * 310 事实驱动同哲学）；scope 上限 256 折 {@code __overflow__}（舱拒绝计数
 * 同款诚实边界）。
 */
public final class ErrorBudget {

    /** 配置：SLO 百分比 / burn 阈值 / 桶数 / 窗长 / 最小样本。 */
    public record Config(double sloPercent, double burnRateThreshold, int buckets,
            Duration window, int minSamples) {

        public static final double DEFAULT_BURN_THRESHOLD = 2.0;
        public static final int DEFAULT_BUCKETS = 60;
        public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(10);
        public static final int DEFAULT_MIN_SAMPLES = 20;

        public Config {
            if (sloPercent <= 0 || sloPercent >= 100) {
                throw new IllegalArgumentException("sloPercent ∈ (0,100)（当前 " + sloPercent + "）");
            }
            if (burnRateThreshold <= 0) {
                throw new IllegalArgumentException("burnRateThreshold > 0（当前 " + burnRateThreshold + "）");
            }
            if (buckets < 2) {
                throw new IllegalArgumentException("buckets >= 2（当前 " + buckets + "）");
            }
            if (window == null || window.isZero() || window.isNegative()
                    || window.toMillis() < buckets) {
                throw new IllegalArgumentException("window >= buckets 毫秒（当前 " + window + "×" + buckets + " 桶）");
            }
            if (minSamples < 1) {
                throw new IllegalArgumentException("minSamples >= 1（当前 " + minSamples + "）");
            }
        }
    }

    private static final int MAX_TRACKED_SCOPES = 256;
    private static final String OVERFLOW_MARKER = "__overflow__";

    private final Config config;
    private final double sloFraction; // 1 − SLO/100
    private final long bucketMillis;
    private final Clock clock;
    private final Map<String, Tallies> scopes = new ConcurrentHashMap<>();
    private long lastTickBucket;

    private static final class Tallies {
        final long[] errors;
        final long[] totals;

        Tallies(int buckets) {
            this.errors = new long[buckets];
            this.totals = new long[buckets];
        }
    }

    public ErrorBudget(Config config, Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock 必须非空");
        }
        this.config = config;
        this.sloFraction = 1.0 - config.sloPercent() / 100.0;
        this.bucketMillis = Math.max(1, config.window().toMillis() / config.buckets());
        this.clock = clock;
        this.lastTickBucket = clock.millis() / bucketMillis;
    }

    /** 记一次结局（scope = 工具名等；success=false 计错）。 */
    public synchronized void record(String scope, boolean success) {
        if (scope == null) {
            return;
        }
        rotate();
        Tallies tallies = talliesOf(scope);
        int idx = (int) (currentBucket() % config.buckets());
        tallies.totals[idx]++;
        if (!success) {
            tallies.errors[idx]++;
        }
    }

    /** 窗内错误率（0..1；无样本 0）。 */
    public synchronized double errorRate(String scope) {
        rotate();
        Tallies tallies = scopes.get(scope);
        if (tallies == null) {
            return 0.0;
        }
        long errors = 0;
        long totals = 0;
        for (int i = 0; i < config.buckets(); i++) {
            errors += tallies.errors[i];
            totals += tallies.totals[i];
        }
        return totals == 0 ? 0.0 : (double) errors / totals;
    }

    /** 燃尽率 = 错误率/(1−SLO)——burn 1 = 按计划烧，&gt;1 超计划。 */
    public double burnRate(String scope) {
        return errorRate(scope) / sloFraction;
    }

    /** 燃尽超阈<b>且</b>样本充足（min-samples 防「一败 100%」噪声）。 */
    public synchronized boolean breaching(String scope) {
        rotate();
        Tallies tallies = scopes.get(scope);
        if (tallies == null) {
            return false;
        }
        long errors = 0;
        long totals = 0;
        for (int i = 0; i < config.buckets(); i++) {
            errors += tallies.errors[i];
            totals += tallies.totals[i];
        }
        return totals >= config.minSamples()
                && (double) errors / totals / sloFraction >= config.burnRateThreshold();
    }

    /** 任一 scope 燃尽超阈（健康面 DOWN 判定——不截断 top）。 */
    public synchronized boolean anyBreaching() {
        for (String scope : scopes.keySet()) {
            if (breaching(scope)) {
                return true;
            }
        }
        return false;
    }

    /** top 燃尽 scope（burn 降序、同值字典序——输出稳定；观测面有界用）。 */
    public List<Map.Entry<String, Double>> topBreaching(int n) {
        Map<String, Double> breaching = new LinkedHashMap<>();
        for (String scope : scopes.keySet()) {
            if (breaching(scope)) {
                breaching.put(scope, burnRate(scope));
            }
        }
        return breaching.entrySet().stream()
                .sorted((a, b) -> {
                    int byBurn = Double.compare(b.getValue(), a.getValue());
                    return byBurn != 0 ? byBurn : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }

    /** 窗内样本总量（scope；观测面）。 */
    public synchronized long samples(String scope) {
        rotate();
        Tallies tallies = scopes.get(scope);
        if (tallies == null) {
            return 0;
        }
        long totals = 0;
        for (int i = 0; i < config.buckets(); i++) {
            totals += tallies.totals[i];
        }
        return totals;
    }

    /** 是否已有任何样本（健康面 UNKNOWN 判定——未启用 ≠ DOWN）。 */
    public synchronized boolean hasSamples() {
        rotate();
        for (Tallies tallies : scopes.values()) {
            for (int i = 0; i < config.buckets(); i++) {
                if (tallies.totals[i] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 跟踪 scope 数（含 overflow 折叠行——观测面/测试）。 */
    public int trackedScopes() {
        return scopes.size();
    }

    /** 配置（观测面）。 */
    public Config config() {
        return config;
    }

    private Tallies talliesOf(String scope) {
        Tallies existing = scopes.get(scope);
        if (existing != null) {
            return existing;
        }
        if (scopes.size() >= MAX_TRACKED_SCOPES) {
            return scopes.computeIfAbsent(OVERFLOW_MARKER, k -> new Tallies(config.buckets()));
        }
        return scopes.computeIfAbsent(scope, k -> new Tallies(config.buckets()));
    }

    private long currentBucket() {
        return clock.millis() / bucketMillis;
    }

    /** 惰性旋转：清过期桶（delta ≥ 桶数 = 全清——窗整体滑出）。 */
    private void rotate() {
        long current = currentBucket();
        long delta = current - lastTickBucket;
        if (delta <= 0) {
            return;
        }
        if (delta >= config.buckets()) {
            for (Tallies tallies : scopes.values()) {
                Arrays.fill(tallies.errors, 0L);
                Arrays.fill(tallies.totals, 0L);
            }
        } else {
            for (long b = lastTickBucket + 1; b <= current; b++) {
                int idx = (int) (b % config.buckets());
                for (Tallies tallies : scopes.values()) {
                    tallies.errors[idx] = 0;
                    tallies.totals[idx] = 0;
                }
            }
        }
        lastTickBucket = current;
    }
}
