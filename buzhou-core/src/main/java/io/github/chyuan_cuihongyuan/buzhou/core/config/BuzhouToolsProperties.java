package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.Map;

/**
 * 工具面装配属性（spec 31 / T110 / impl-85，前缀 {@code buzhou.tools}；
 * spec 305 / 306 扩 health / circuit 组）。
 *
 * @param resultLimitChars      工具结果入模型上下文的字符上限（默认 20_000；-1 = 不限）
 * @param resultLimitOverrides  per-tool 覆盖（glob 通配键，值 = 字符数或 -1 禁用；
 *                              追加式覆盖默认豁免 read_range——同键改值，新键叠加）
 * @param health                工具健康探测装配组（spec 305；默认关——探针注册归宿主）
 * @param circuit               工具熔断 yml 装配组（spec 306；默认关——165/131 原语装配面）
 */
@ConfigurationProperties(prefix = "buzhou.tools")
public record BuzhouToolsProperties(
        Integer resultLimitChars,
        Map<String, Integer> resultLimitOverrides,
        Health health,
        Circuit circuit,
        Map<String, String> baggage) {

    /** 2 参兼容构造（spec 305 之前调用方；health/circuit/baggage = 未配置）。 */
    public BuzhouToolsProperties(Integer resultLimitChars, Map<String, Integer> resultLimitOverrides) {
        this(resultLimitChars, resultLimitOverrides, null, null, null);
    }

    /** 4 参兼容构造（spec 337 之前调用方；baggage = 未配置）。 */
    public BuzhouToolsProperties(Integer resultLimitChars, Map<String, Integer> resultLimitOverrides,
            Health health, Circuit circuit) {
        this(resultLimitChars, resultLimitOverrides, health, circuit, null);
    }

    /** 多构造器场景：显式指定规范构造器为绑定构造器（T187 勘察同款）。 */
    @ConstructorBinding
    public BuzhouToolsProperties {
        if (resultLimitChars == null) {
            resultLimitChars = io.github.chyuan_cuihongyuan.buzhou.core.exec
                    .ToolResultLimiter.DEFAULT_LIMIT_CHARS;
        }
        if (resultLimitChars < -1) {
            throw new BuzhouConfigurationException(
                    "buzhou.tools.result-limit-chars（" + resultLimitChars + "）非法",
                    "设为 >= 0 的整数或 -1（不限）");
        }
        if (resultLimitOverrides != null) {
            resultLimitOverrides.values().stream()
                    .filter(v -> v != null && v < -1)
                    .findAny()
                    .ifPresent(v -> {
                        throw new BuzhouConfigurationException(
                                "buzhou.tools.result-limit-overrides 值（" + v + "）非法",
                                "每项设为 >= 0 的整数或 -1（该工具不限）");
                    });
        }
        baggage = baggage == null ? java.util.Map.of() : java.util.Map.copyOf(baggage);
    }

    /**
     * 工具健康探测装配组（spec 305 / T601，Consul health check）。前缀
     * {@code buzhou.tools.health}。默认关；探针注册归宿主（框架不知道怎么探——分层诚实）。
     *
     * @param enabled  开关（开启即装配 ToolHealthProber bean + 周期自调度 + 健康面）
     * @param interval 探测周期（默认 30s）
     */
    public record Health(Boolean enabled, Duration interval) {

        public Health {
            interval = interval == null ? Duration.ofSeconds(30) : interval;
            if (interval.isZero() || interval.isNegative()) {
                throw new BuzhouConfigurationException(
                        "buzhou.tools.health.interval（" + interval + "）非法", "正时长，如 30s");
            }
        }

        /** 生效开关（显式开启）。 */
        public boolean effectiveEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /**
     * 工具熔断 yml 装配组（spec 306 / T603，resilience4j——spec 131/165 原语装配面；
     * fog 227「新 hook 配置面族」首项）。前缀 {@code buzhou.tools.circuit}。默认关。
     *
     * @param enabled                   开关（开启即装配 ToolCircuitBreakerHook——BuzhouHook 自动收集）
     * @param windowSize                计数窗口样本数（默认 20）
     * @param failureRateThresholdPercent 跳闸失败率阈值百分比（默认 50；∈(0,100]）
     * @param cooldown                  OPEN 冷却时长（默认 60s；期满进半开探测）
     * @param halfOpenTrials            半开探测名额（默认 3）
     */
    public record Circuit(Boolean enabled, Integer windowSize,
                          Double failureRateThresholdPercent, Duration cooldown,
                          Integer halfOpenTrials) {

        public Circuit {
            windowSize = windowSize == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker
                            .Config.defaults().windowSize() : windowSize;
            failureRateThresholdPercent = failureRateThresholdPercent == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker
                            .Config.defaults().failureRateThresholdPercent() : failureRateThresholdPercent;
            cooldown = cooldown == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker
                            .Config.defaults().cooldown() : cooldown;
            halfOpenTrials = halfOpenTrials == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker
                            .Config.defaults().halfOpenTrials() : halfOpenTrials;
            if (windowSize < 2 || !(failureRateThresholdPercent > 0 && failureRateThresholdPercent <= 100)
                    || cooldown.isZero() || cooldown.isNegative() || halfOpenTrials < 1) {
                throw new BuzhouConfigurationException(
                        "buzhou.tools.circuit 参数非法（window-size>=2、failure-rate-threshold-percent∈(0,100]、"
                                + "cooldown 正时长、half-open-trials>=1）",
                        "例：window-size=20, failure-rate-threshold-percent=50, cooldown=60s, half-open-trials=3");
            }
        }

        /** 生效开关（显式开启）。 */
        public boolean effectiveEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        /** 映射为熔断器 Config（装配用）。 */
        public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker.Config toConfig() {
            return new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.ToolCircuitBreaker.Config(
                    windowSize, failureRateThresholdPercent, cooldown, halfOpenTrials);
        }
    }
}
