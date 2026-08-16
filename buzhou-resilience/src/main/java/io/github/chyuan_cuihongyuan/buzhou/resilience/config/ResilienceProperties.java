package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 模型韧性层装配属性（spec 15 / 09，前缀 {@code buzhou.resilience}；impl-44 补 fail-fast）。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置」→ 取规范默认（对齐 {@code SpillProperties} 模板），
 * 这样 yml 只覆盖个别参数时其余仍取默认，而非被零值抹掉。<b>显式非法值（负时长 / 越界数值）
 * 启动即失败</b>（BuzhouConfigurationException 带修法），不静默纠偏。
 *
 * @param enabled             整个韧性机制开关（默认开，safe-by-default；关则回退底座原生行为）
 * @param maxAttempts         最大尝试次数（含首次；默认 3，即首调 + 最多 2 次重试）
 * @param initialBackoff      首次重试退避基数（默认 500ms）
 * @param maxBackoff          退避上限（默认 10s；Retry-After 与指数增长都钳制到本值）
 * @param multiplier          指数退避乘子（默认 2.0；1.0 = 恒定退避）
 * @param jitter              抖动因子 {@code [0,1]}（默认 0.5；多实例同时重试时打散惊群，0 = 关闭抖动）
 * @param retryableCategories 可重试类别集合（大小写无关；默认 {@code [RATE_LIMIT, NETWORK]}，
 *                            覆盖默认可重试表，如把 {@code UNKNOWN} 配为重试）
 * @param deadline            模型调用统一超时（默认 60s；超时取消在途调用并作为终态失败。
 *                            显式设为 0 关闭超时——不推荐生产关闭）
 * @param circuit             熔断器参数组（默认启用、保守阈值；null = 全默认）
 */
@ConfigurationProperties(prefix = "buzhou.resilience")
@Validated
public record ResilienceProperties(
        Boolean enabled,
        @Min(value = 1, message = "max-attempts 必须 >= 1（首调 + 重试数）") Integer maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        @DecimalMin(value = "1.0", message = "multiplier 必须 >= 1.0（1.0 = 恒定退避）") Double multiplier,
        @DecimalMin(value = "0.0") @DecimalMax(value = "1.0", message = "jitter 取值 [0,1]") Double jitter,
        List<String> retryableCategories,
        Duration deadline,
        @Valid RateLimit rateLimit,
        @Valid Circuit circuit,
        @Valid Fallback fallback,
        @Valid SessionQuota sessionQuota,
        @Valid Shadow shadow,
        @Valid ResponseCache responseCache) {

    /** 13 参兼容构造（spec 53 之前调用方；response-cache = 未配置）。 */
    public ResilienceProperties(
            Boolean enabled, Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
            Double multiplier, Double jitter, List<String> retryableCategories, Duration deadline,
            RateLimit rateLimit, Circuit circuit, Fallback fallback, SessionQuota sessionQuota,
            Shadow shadow) {
        this(enabled, maxAttempts, initialBackoff, maxBackoff, multiplier, jitter,
                retryableCategories, deadline, rateLimit, circuit, fallback, sessionQuota,
                shadow, null);
    }

    /** 12 参兼容构造（spec 49 之前调用方；shadow = 未配置）。 */
    public ResilienceProperties(
            Boolean enabled, Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
            Double multiplier, Double jitter, List<String> retryableCategories, Duration deadline,
            RateLimit rateLimit, Circuit circuit, Fallback fallback, SessionQuota sessionQuota) {
        this(enabled, maxAttempts, initialBackoff, maxBackoff, multiplier, jitter,
                retryableCategories, deadline, rateLimit, circuit, fallback, sessionQuota, null);
    }

    /** 11 参兼容构造（impl-59 之前调用方；session-quota = 未配置）。 */
    public ResilienceProperties(
            Boolean enabled, Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
            Double multiplier, Double jitter, List<String> retryableCategories, Duration deadline,
            RateLimit rateLimit, Circuit circuit, Fallback fallback) {
        this(enabled, maxAttempts, initialBackoff, maxBackoff, multiplier, jitter,
                retryableCategories, deadline, rateLimit, circuit, fallback, null, null);
    }

    /** 9 参兼容构造（impl-56 之前调用方；circuit = 全默认、fallback = 未配置）。 */
    public ResilienceProperties(
            Boolean enabled, Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
            Double multiplier, Double jitter, List<String> retryableCategories, Duration deadline,
            RateLimit rateLimit) {
        this(enabled, maxAttempts, initialBackoff, maxBackoff, multiplier, jitter,
                retryableCategories, deadline, rateLimit, null, null);
    }

    /**
     * 模型 RPM+TPM 双桶限流参数组（spec 15「背压 · 维度③」）。
     *
     * <p>前缀 {@code buzhou.resilience.rate-limit}。boxed null = 不限（safe-by-default）。
     *
     * @param requestsPerMinute 每分钟请求数上限（null = 不限）
     * @param tokensPerMinute   每分钟 token 数上限（null = 不限；TPM 事后记账+下次预检=平均速率保护）
     * @param queueTimeout      QUEUE 档排队超时（null = 取保守默认 30s）
     * @param overloadPolicy    过载策略（null = QUEUE；FAIL_FAST = 不排队直接拒）
     */
    public record RateLimit(
            @Min(value = 1, message = "rate-limit.requests-per-minute 必须 >= 1") Integer requestsPerMinute,
            @Min(value = 1, message = "rate-limit.tokens-per-minute 必须 >= 1") Integer tokensPerMinute,
            Duration queueTimeout,
            String overloadPolicy) {
    }

    /**
     * 熔断器参数组（spec 15「熔断器」，T81 / impl-56）。前缀 {@code buzhou.resilience.circuit}。
     * 默认启用、保守阈值（真实 provider 故障才跳闸）。
     *
     * @param enabled              熔断开关（null = 开）
     * @param windowSize           计数窗口样本数（默认 20；ring buffer，满窗移出最老）
     * @param minCalls             跳闸最小样本数（默认 5；不足样本不判失败率，防小样本误跳）
     * @param failureRateThreshold 跳闸失败率阈值（默认 0.5；≥ 该值且样本 ≥ min-calls 即跳闸）
     * @param openCooldown         OPEN 冷却时长（默认 30s；期满放行单探测进 HALF_OPEN）
     * @param failureCategories    计入失败的类别（默认 {@code [NETWORK, SERVER, TIMEOUT]}；
     *                             RATE_LIMIT/CONTENT/AUTH/UNKNOWN 为 IGNORED 不进窗口）
     * @param backoffCap           连续跳闸冷却退避倍数上限（默认 8；冷却 = open-cooldown ×
     *                             min(2^(trips-1), cap)，探测成功即复位——spec 25 / T104）
     * @param halfOpenSuccessThreshold 半开连续成功探测数（默认 1 = 单探测即恢复的既有行为；
     *                             >1 时抖动 provider 需连续 N 次探测成功才 CLOSE——spec 35 §A / T118）
     */
    public record Circuit(
            Boolean enabled,
            Integer windowSize,
            Integer minCalls,
            Double failureRateThreshold,
            Duration openCooldown,
            List<String> failureCategories,
            Integer backoffCap,
            Integer halfOpenSuccessThreshold) {

        /** 既有 6 参便捷构造（backoffCap/halfOpen 默认，二进制/源码兼容既有调用点）。 */
        public Circuit(Boolean enabled, Integer windowSize, Integer minCalls,
                Double failureRateThreshold, Duration openCooldown, List<String> failureCategories) {
            this(enabled, windowSize, minCalls, failureRateThreshold, openCooldown, failureCategories,
                    null, null);
        }

        /** 7 参便捷构造（halfOpenSuccessThreshold 默认）。 */
        public Circuit(Boolean enabled, Integer windowSize, Integer minCalls,
                Double failureRateThreshold, Duration openCooldown, List<String> failureCategories,
                Integer backoffCap) {
            this(enabled, windowSize, minCalls, failureRateThreshold, openCooldown, failureCategories,
                    backoffCap, null);
        }

        /** 多构造器场景：显式指定规范构造器为绑定构造器（T187 勘察修复——缺注解时 yml 键静默不生效）。 */
        @org.springframework.boot.context.properties.bind.ConstructorBinding
        public Circuit {
            enabled = enabled == null ? true : enabled;
            windowSize = windowSize == null ? 20 : windowSize;
            minCalls = minCalls == null ? 5 : minCalls;
            failureRateThreshold = failureRateThreshold == null ? 0.5 : failureRateThreshold;
            openCooldown = openCooldown == null ? Duration.ofSeconds(30) : openCooldown;
            failureCategories = failureCategories == null || failureCategories.isEmpty()
                    ? List.of("NETWORK", "SERVER", "TIMEOUT")
                    : failureCategories.stream().map(c -> c.toUpperCase(java.util.Locale.ROOT)).toList();
            if (windowSize < 2) {
                throw configError("circuit.window-size", String.valueOf(windowSize), "设为 >= 2 的整数");
            }
            if (minCalls < 1 || minCalls > windowSize) {
                throw configError("circuit.min-calls", String.valueOf(minCalls),
                        "设为 [1, window-size] 内的整数（不足样本不判失败率）");
            }
            if (failureRateThreshold <= 0 || failureRateThreshold > 1) {
                throw configError("circuit.failure-rate-threshold", String.valueOf(failureRateThreshold),
                        "设为 (0, 1]（默认 0.5）");
            }
            if (openCooldown.isZero() || openCooldown.isNegative()) {
                throw configError("circuit.open-cooldown", openCooldown.toString(), "设为正时长，如 30s");
            }
            backoffCap = backoffCap == null ? 8 : backoffCap;
            if (backoffCap < 1) {
                throw configError("circuit.backoff-cap", String.valueOf(backoffCap), "设为 >= 1 的整数");
            }
            halfOpenSuccessThreshold = halfOpenSuccessThreshold == null ? 1 : halfOpenSuccessThreshold;
            if (halfOpenSuccessThreshold < 1) {
                throw configError("circuit.half-open-success-threshold",
                        String.valueOf(halfOpenSuccessThreshold), "设为 >= 1 的整数（1 = 单探测即恢复）");
            }
        }

        /** 连续跳闸自适应冷却倍数（trips 含本次跳闸；cap 封顶）。 */
        public long backoffMultiplier(int consecutiveTrips) {
            long shift = Math.max(0, Math.min(consecutiveTrips - 1, 30));
            return Math.min(1L << shift, backoffCap);
        }

        /** 生效开关（compact ctor 已归一，恒非 null）。 */
        public boolean effectiveEnabled() {
            return enabled;
        }
    }

    /**
     * 备模型降级链参数组（spec 15「备模型降级链」，T82 / impl-57；spec 48 §B / T175 金丝雀）。
     * 前缀 {@code buzhou.resilience.fallback}。未配置 models = 不降级。
     *
     * @param models            备模型 bean 名有序列表（Spring 路径按名解析，未命中启动失败；空 = 不降级）
     * @param triggerCategories 触发降级的主模型终态失败类别（默认 {@code [NETWORK, SERVER, TIMEOUT, AUTH]}；
     *                          CONTENT 不触发防策略跳舱；熔断 OPEN 恒触发不受此表控制）
     * @param canaryEnabled     spec 48 §B：金丝雀开关（默认 false）——启用时首次模型调用按
     *                          会话稳定哈希在候选池（主+备）加权抽取初始目标
     * @param weights           spec 48 §B：候选权重（modelName → 权重；未列名默认 1；
     *                          LiteLLM Router simple-shuffle 语义收窄为会话稳定）
     */
    public record Fallback(
            List<String> models,
            List<String> triggerCategories,
            Boolean canaryEnabled,
            Map<String, Integer> weights) {

        /** 多构造器场景：显式指定规范构造器为绑定构造器（T187 勘察修复——缺注解时 yml 键静默不生效）。 */
        @org.springframework.boot.context.properties.bind.ConstructorBinding
        public Fallback {
            triggerCategories = triggerCategories == null || triggerCategories.isEmpty()
                    ? List.of("NETWORK", "SERVER", "TIMEOUT", "AUTH")
                    : triggerCategories.stream().map(c -> c.toUpperCase(java.util.Locale.ROOT)).toList();
            if (models != null && models.isEmpty()) {
                models = null; // 空列表视同未配置
            }
            weights = weights == null ? Map.of() : Map.copyOf(weights);
        }

        /** 既有 2 参构造兼容（金丝雀关、无权重）。 */
        public Fallback(List<String> models, List<String> triggerCategories) {
            this(models, triggerCategories, null, null);
        }

        /** 是否配置了备模型。 */
        public boolean enabled() {
            return models != null && !models.isEmpty();
        }

        /** 类别是否触发降级。 */
        public boolean triggers(String category) {
            return category != null && triggerCategories.contains(category.toUpperCase(java.util.Locale.ROOT));
        }
    }

    /**
     * shadow 探测参数组（spec 49 §A / T176）。前缀 {@code buzhou.resilience.shadow}。
     *
     * @param enabled       开关（默认 false——未启用零提交零事件零计数）
     * @param models        shadow ChatModel bean 名列表（未命中启动失败；空 = 不探测）
     * @param maxConcurrent 进程级并发上限（默认 2；超限提交计 skipped-concurrency）
     * @param dailyBudget   进程级 UTC 日预算（提交次数口径；默认 1000；池尽计 skipped-budget）
     */
    public record Shadow(
            Boolean enabled,
            List<String> models,
            Integer maxConcurrent,
            Long dailyBudget) {

        public Shadow {
            models = models == null || models.isEmpty() ? null : List.copyOf(models);
            maxConcurrent = maxConcurrent == null || maxConcurrent <= 0 ? 2 : maxConcurrent;
            dailyBudget = dailyBudget == null || dailyBudget < 0 ? 1000L : dailyBudget;
        }

        /** 生效开关：显式开启（模型来源由装配面校验——Spring 路径看 models 名单，编程式路径看注入列表）。 */
        public boolean effectiveEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /**
     * per-session 日配额组（spec 16「per-session 配额」，T84 / impl-59）。
     * 前缀 {@code buzhou.resilience.session-quota}。UTC 自然日固定窗口；null = 不限。
     *
     * @param turnsPerDay     每会话每日轮次上限
     * @param toolCallsPerDay 每会话每日工具调用上限
     * @param tokensPerDay    每会话每日 token 上限（prompt+completion）
     */
    public record SessionQuota(
            Integer turnsPerDay,
            Integer toolCallsPerDay,
            Long tokensPerDay) {

        public SessionQuota {
            if (turnsPerDay != null && turnsPerDay < 1) {
                throw configError("session-quota.turns-per-day", String.valueOf(turnsPerDay),
                        "设为正整数，或删除该键（不限）");
            }
            if (toolCallsPerDay != null && toolCallsPerDay < 1) {
                throw configError("session-quota.tool-calls-per-day", String.valueOf(toolCallsPerDay),
                        "设为正整数，或删除该键（不限）");
            }
            if (tokensPerDay != null && tokensPerDay < 1) {
                throw configError("session-quota.tokens-per-day", String.valueOf(tokensPerDay),
                        "设为正整数，或删除该键（不限）");
            }
        }
    }

    /**
     * 精确响应缓存参数组（spec 53 §E / T207）。前缀 {@code buzhou.resilience.response-cache}。
     * 默认关（零行为回归）；开启后同请求（model+messages+options 采样）二次调用命中短路。
     *
     * @param enabled    开关（默认 false）
     * @param maxEntries LRU 容量（默认 256）
     * @param ttl        条目 TTL（默认 1h；惰性过期——命中路径检查）
     */
    public record ResponseCache(
            Boolean enabled,
            Integer maxEntries,
            Duration ttl) {

        public ResponseCache {
            maxEntries = maxEntries == null ? 256 : maxEntries;
            ttl = ttl == null ? Duration.ofHours(1) : ttl;
        }

        /** 生效开关（显式开启）。 */
        public boolean effectiveEnabled() {
            return Boolean.TRUE.equals(enabled);
        }
    }

    /** 多构造器场景：显式指定规范构造器为绑定构造器（兼容构造不参与绑定）。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public ResilienceProperties {
        enabled = enabled == null ? true : enabled;
        // Duration 无标准 JSR-303 约束（HV 不支持），fail-fast 在此显式做：非法值抛 BuzhouConfigurationException
        if (maxAttempts != null && maxAttempts < 1) {
            throw configError("max-attempts", String.valueOf(maxAttempts), "设为 >= 1 的整数");
        }
        if (initialBackoff != null && (initialBackoff.isNegative() || initialBackoff.isZero())) {
            throw configError("initial-backoff", initialBackoff.toString(), "设为正时长，如 500ms");
        }
        if (maxBackoff != null && (maxBackoff.isNegative() || maxBackoff.isZero())) {
            throw configError("max-backoff", maxBackoff.toString(), "设为正时长，如 10s");
        }
        if (multiplier != null && multiplier < 1.0) {
            throw configError("multiplier", String.valueOf(multiplier), "设为 >= 1.0（1.0 = 恒定退避）");
        }
        if (jitter != null && (jitter < 0 || jitter > 1)) {
            throw configError("jitter", String.valueOf(jitter), "设为 [0,1]");
        }
        if (deadline != null && deadline.isNegative()) {
            throw configError("deadline", deadline.toString(), "设为正时长，或显式 0 关闭超时");
        }
        if (rateLimit != null) {
            if (rateLimit.queueTimeout() != null && rateLimit.queueTimeout().isNegative()) {
                throw configError("rate-limit.queue-timeout", rateLimit.queueTimeout().toString(), "设为非负时长");
            }
        }
        maxAttempts = maxAttempts == null ? 3 : maxAttempts;
        initialBackoff = initialBackoff == null ? Duration.ofMillis(500) : initialBackoff;
        maxBackoff = maxBackoff == null ? Duration.ofSeconds(10) : maxBackoff;
        multiplier = multiplier == null ? 2.0 : multiplier;
        jitter = jitter == null ? 0.5 : jitter;
        retryableCategories = retryableCategories == null || retryableCategories.isEmpty()
                ? List.of("RATE_LIMIT", "NETWORK") : List.copyOf(retryableCategories);
        deadline = deadline == null ? Duration.ofSeconds(60) : deadline;
        // rateLimit 保持 null = 未配置（不限），由模块层判定
        circuit = circuit == null ? new Circuit(null, null, null, null, null, null) : circuit;
        shadow = shadow == null ? new Shadow(null, null, null, null) : shadow;
        responseCache = responseCache == null ? new ResponseCache(null, null, null) : responseCache;
        if (responseCache.maxEntries() < 1) {
            throw configError("response-cache.max-entries",
                    String.valueOf(responseCache.maxEntries()), "设为 >= 1 的整数");
        }
        if (responseCache.ttl().isZero() || responseCache.ttl().isNegative()) {
            throw configError("response-cache.ttl", responseCache.ttl().toString(), "设为正时长，如 1h");
        }
    }

    private static BuzhouConfigurationException configError(String key, String value, String action) {
        return new BuzhouConfigurationException(
                "buzhou.resilience." + key + "（" + value + "）非法", action);
    }

    /** 全默认（装配测试 / 兜底用）。 */
    public static ResilienceProperties defaults() {
        return new ResilienceProperties(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** rate-limit 过载策略生效值（null/空白 = QUEUE；非法值启动即失败——fail-fast，不静默回退）。 */
    public io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy effectiveRateLimitOverloadPolicy() {
        if (rateLimit == null || rateLimit.overloadPolicy() == null || rateLimit.overloadPolicy().isBlank()) {
            return io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.QUEUE;
        }
        try {
            return io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.valueOf(
                    rateLimit.overloadPolicy().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BuzhouConfigurationException(
                    "buzhou.resilience.rate-limit.overload-policy（" + rateLimit.overloadPolicy() + "）非法",
                    "取值只能是 QUEUE 或 FAIL_FAST");
        }
    }
}
