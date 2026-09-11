package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeSet;

/**
 * 模型端点离群驱逐（spec 149 / T505，Envoy outlier detection 借鉴）：连续错误
 * 达阈值 → 驱逐出备选池一个窗口（过期自动复池）；{@link #filter} 从降级链候选
 * 剔除在逐成员（保序）。与熔断（spec 15 单模型开关）/延迟排序（spec 64 快者优先）
 * 正交——本类管「池成员资格」。
 *
 * <p>spec 224 / T592：Envoy panic threshold 借鉴——{@link #filter} 剔除后健康候选数
 * 跌破恐慌阈值（占候选百分比，向上取整）时<b>忽略驱逐返回全量候选</b>：全逐比
 * 试坏端点更糟，可用性优先于隔离；触发时计数 {@code buzhou.outlier.panic} + WARN
 * 留痕。默认 0 = 关闭（零行为变化）；Envoy 路由侧常用 50。
 */
public final class ModelOutlierEjection {

    /** 配置：连错阈值 / 驱逐窗口 / 恐慌阈值百分比（均正；percent ∈ [0,100]，0 = 关）。 */
    public record Config(int consecutiveErrors, Duration ejectionWindow, int panicThresholdPercent) {
        public Config {
            if (consecutiveErrors < 1 || ejectionWindow == null
                    || ejectionWindow.isZero() || ejectionWindow.isNegative()) {
                throw new IllegalArgumentException(
                        "驱逐配置非法（consecutiveErrors>=1、ejectionWindow 为正）");
            }
            if (panicThresholdPercent < 0 || panicThresholdPercent > PERCENT_MAX) {
                throw new IllegalArgumentException(
                        "panicThresholdPercent 必须在 [0,100]（当前 " + panicThresholdPercent + "）");
            }
        }

        /** 两参兼容构造（panic 关闭——既有调用零行为变化）。 */
        public Config(int consecutiveErrors, Duration ejectionWindow) {
            this(consecutiveErrors, ejectionWindow, 0);
        }

        public static Config defaults() {
            return new Config(5, Duration.ofSeconds(30));
        }

        /** panic=100 便捷预设（任何驱逐即恐慌——健康数占满才不触发）。 */
        public static Config withPanicAll(int consecutiveErrors, Duration ejectionWindow) {
            return new Config(consecutiveErrors, ejectionWindow, PERCENT_MAX);
        }
    }

    private static final String EJECTED_COUNTER = "buzhou.outlier.ejected";
    private static final String PANIC_COUNTER = "buzhou.outlier.panic";
    private static final int PERCENT_MAX = 100;
    private static final int PERCENT_SCALE = 100;
    private static final System.Logger LOGGER = System.getLogger(ModelOutlierEjection.class.getName());

    private static final class ModelState {
        int consecutiveErrors;
        long ejectedUntilMillis;

        ModelState() {
            this.ejectedUntilMillis = 0L; // 0 = 从未/已复池
        }
    }

    private final Config config;
    private final Clock clock;
    private final Map<String, ModelState> models = new ConcurrentHashMap<>();

    public ModelOutlierEjection() {
        this(Config.defaults(), Clock.systemUTC());
    }

    public ModelOutlierEjection(Config config, Clock clock) {
        this.config = config == null ? Config.defaults() : config;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /** 记一次模型调用错误：连错达阈值即驱逐一个窗口。 */
    public void recordError(String modelName) {
        ModelState state = models.computeIfAbsent(modelName, k -> new ModelState());
        synchronized (state) {
            state.consecutiveErrors++;
            if (state.consecutiveErrors >= config.consecutiveErrors()) {
                state.ejectedUntilMillis = clock.millis() + config.ejectionWindow().toMillis();
                state.consecutiveErrors = 0; // 驱逐后重新计数（复池后再观察新窗）
                BuzhouMetricsHolder.metrics().counter(EJECTED_COUNTER, 1);
            }
        }
    }

    /** 记一次成功：复位连错（健康调用抵销劣化轨迹）。 */
    public void recordSuccess(String modelName) {
        ModelState state = models.get(modelName);
        if (state != null) {
            synchronized (state) {
                state.consecutiveErrors = 0;
            }
        }
    }

    /** 是否在逐（窗口过期即视为复池——下次调用生效）。 */
    public boolean isEjected(String modelName) {
        ModelState state = models.get(modelName);
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return clock.millis() < state.ejectedUntilMillis;
        }
    }

    /**
     * 健康池视图：剔除在逐成员（保序）——挂在 FallbackChain.models() 之后。
     * spec 224 / T592：健康数跌破恐慌阈值时忽略驱逐返回全量（可用性优先 + 留痕）。
     */
    public List<NamedFallbackModel> filter(List<NamedFallbackModel> candidates) {
        List<NamedFallbackModel> healthy = new ArrayList<>();
        for (NamedFallbackModel candidate : candidates) {
            if (!isEjected(candidate.name())) {
                healthy.add(candidate);
            }
        }
        int minHealthy = minHealthyCount(candidates.size());
        if (minHealthy > 0 && healthy.size() < minHealthy) {
            BuzhouMetricsHolder.metrics().counter(PANIC_COUNTER, 1);
            panicActivations.incrementAndGet();
            LOGGER.log(System.Logger.Level.WARNING,
                    "离群驱逐恐慌模式：健康候选 " + healthy.size() + "/" + candidates.size()
                            + " 低于阈值 " + minHealthy + "（panicThresholdPercent="
                            + config.panicThresholdPercent() + "）——忽略驱逐保可用性");
            return candidates;
        }
        return healthy;
    }

    /** 恐慌下限 = ceil(候选数 × percent / 100)；percent=0（关）或空候选 = 0 不触发。 */
    private int minHealthyCount(int candidateCount) {
        if (config.panicThresholdPercent() <= 0 || candidateCount <= 0) {
            return 0;
        }
        return (candidateCount * config.panicThresholdPercent() + PERCENT_SCALE - 1) / PERCENT_SCALE;
    }

    /** spec 633 / T916：恐慌激活累计（观测面——非零持续增长 = 备选池常年低于恐慌线）。 */
    private final java.util.concurrent.atomic.AtomicLong panicActivations =
            new java.util.concurrent.atomic.AtomicLong();

    /** 恐慌激活累计（观测面）。 */
    public long panicActivations() {
        return panicActivations.get();
    }

    /** 当前被逐名单（观测面，稳定序）。 */
    public Set<String> ejectedModels() {
        Set<String> out = new TreeSet<>();
        models.forEach((name, state) -> {
            if (isEjected(name)) {
                out.add(name);
            }
        });
        return out;
    }
}
