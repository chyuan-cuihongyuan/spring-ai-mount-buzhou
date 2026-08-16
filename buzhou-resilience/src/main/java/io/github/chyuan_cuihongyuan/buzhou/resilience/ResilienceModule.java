package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ModelCallInFlight;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceSessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.RateLimitAdvisor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模型韧性层模块入口（spec 15「模块与改动面」）。
 *
 * <p>独立可用、仅依赖 {@code buzhou-core}，遵守星形依赖白名单（不与 memory/spill/guard/tools/store-* 互依）。
 * {@link #configure(ResilienceProperties)} 返回一个只含「装配定制器」的 {@link RuntimeConfig}：
 * 定制器在会话装配期把 {@link ResilienceAdvisor} + {@link RateLimitAdvisor}（如配置了限流）注入 ChatClient advisor 链。
 *
 * <p>装配形态对齐 {@code ObservabilityModule}（贡献 advisor 而非 hook 的模块）：
 * core 收集所有 {@link RuntimeConfig} bean 并 merge，故自装配侧只需暴露一个 {@code RuntimeConfig} bean。
 */
public final class ResilienceModule {

    private ResilienceModule() {
    }

    /**
     * @param properties 韧性参数（null 字段取规范默认）
     * @return 含装配定制器的 {@link RuntimeConfig}；{@code enabled=false} 时返回 {@link RuntimeConfig#defaults()}（不注入 advisor）
     */
    public static RuntimeConfig configure(ResilienceProperties properties) {
        return configure(properties, null, null);
    }

    /**
     * 带模型名的配置入口（spec 15「背压 · 维度③」）。
     *
     * @param modelName 模型名（RPM/TPM 分桶键；null = "unknown"）
     */
    public static RuntimeConfig configure(ResilienceProperties properties, String modelName) {
        return configure(properties, modelName, null);
    }

    /**
     * 带模型名与运维面的完整入口（impl-44 / spec 14 §A）。
     *
     * @param stats 运维计数器（null = 编程式路径不统计）
     */
    public static RuntimeConfig configure(ResilienceProperties properties, String modelName, ResilienceStats stats) {
        return configure(properties, modelName, stats, null);
    }

    /**
     * 带备模型降级链的完整入口（impl-57 / spec 15「备模型降级链」）。
     *
     * @param fallbacks 备模型有序列表（null / 空 = 不降级）；触发类别读
     *                  {@code buzhou.resilience.fallback.trigger-categories}
     */
    public static RuntimeConfig configure(ResilienceProperties properties, String modelName, ResilienceStats stats,
                                          List<NamedFallbackModel> fallbacks) {
        return configure(properties, modelName, stats, fallbacks, null);
    }

    /**
     * 带备模型降级链 + shadow 探测的完整入口（spec 49 §A / T176）。
     *
     * @param fallbacks    备模型有序列表（null / 空 = 不降级）
     * @param shadowModels shadow 模型列表（null / 空 = 不探测；仅 shadow.effectiveEnabled 时生效）
     */
    public static RuntimeConfig configure(ResilienceProperties properties, String modelName, ResilienceStats stats,
                                          List<NamedFallbackModel> fallbacks,
                                          List<NamedFallbackModel> shadowModels) {
        return configure(properties, modelName, stats, fallbacks, shadowModels, null);
    }

    /**
     * 带共享限流后端的完整入口（spec 54 §A / T224 / effort#14）：backend 非 null 时
     * 限流额度存取走共享后端（如 Redis 固定窗——多实例共享额度）；null = 内存令牌桶
     * （默认零变化）。
     */
    public static RuntimeConfig configure(ResilienceProperties properties, String modelName, ResilienceStats stats,
                                          List<NamedFallbackModel> fallbacks,
                                          List<NamedFallbackModel> shadowModels,
                                          io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend rateLimitBackend) {
        if (!properties.enabled()) {
            return RuntimeConfig.defaults();
        }
        validate(properties);
        ProviderErrorClassifier classifier = new DefaultErrorClassifier();
        // 熔断器（impl-56）与限流器（impl-59 修正）：进程级单例——provider 健康与 RPM/TPM 容量都是
        // 进程级事实，configure 每 context 一次、经 customizer 闭包注入全部会话
        // （此前限流器在 customize() 内建，N 会话 = N 倍限额）。
        ModelCircuitBreaker circuit = properties.circuit().effectiveEnabled()
                ? new ModelCircuitBreaker(properties.circuit(), stats)
                : null;
        ModelRateLimiter limiter = null;
        ResilienceProperties.RateLimit rl = properties.rateLimit();
        if (rl != null) {
            limiter = new ModelRateLimiter(
                    rl.requestsPerMinute(), rl.tokensPerMinute(), rl.queueTimeout(),
                    properties.effectiveRateLimitOverloadPolicy(), null, rateLimitBackend);
            if (!limiter.isEnabled()) {
                limiter = null;
            }
        }
        // spec 49 §B / T177：remaining 水位 gauge（按已知模型名集合注册，避免基数无界）
        if (limiter != null) {
            final ModelRateLimiter gaugeLimiter = limiter;
            java.util.List<String> knownModels = new java.util.ArrayList<>();
            if (modelName != null && !modelName.isBlank()) {
                knownModels.add(modelName);
            }
            if (fallbacks != null) {
                fallbacks.forEach(f -> knownModels.add(f.name()));
            }
            if (shadowModels != null) {
                shadowModels.forEach(m -> knownModels.add(m.name()));
            }
            for (String m : knownModels) {
                for (String dim : List.of(ModelRateLimiter.DIMENSION_RPM, ModelRateLimiter.DIMENSION_TPM)) {
                    io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                            .gauge("buzhou.resilience.ratelimit.remaining",
                                    () -> gaugeLimiter.remainingRatio(m, dim),
                                    "model", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(m),
                                    "dimension", dim);
                }
            }
        }
        FallbackChain fallbackChain = fallbacks != null && !fallbacks.isEmpty()
                ? new FallbackChain(fallbacks, properties.fallback())
                : null;
        // spec 49 §A / T176：shadow 探测控制器（进程级共享——并发信号量与日预算都是进程级事实）
        io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow =
                properties.shadow() != null && properties.shadow().effectiveEnabled()
                        && shadowModels != null && !shadowModels.isEmpty()
                        ? new io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController(
                                shadowModels, properties.shadow())
                        : null;
        // spec 53 §E / T207：精确响应缓存（进程级共享 store；默认关 = null 零注入零开销）
        io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore responseCacheStore =
                properties.responseCache() != null && properties.responseCache().effectiveEnabled()
                        ? new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                                properties.responseCache().maxEntries(), properties.responseCache().ttl())
                        : null;
        RuntimeConfig assembly = RuntimeConfig.assemblyCustomizers(
                List.of(new ResilienceAssemblyCustomizer(properties, classifier, modelName, stats, circuit,
                        fallbackChain, limiter, shadow, responseCacheStore)));
        // per-session 日配额（impl-59）：有任一维度才挂 Hook（无配额零开销）。
        if (SessionQuotaHook.anyDimension(properties.sessionQuota())) {
            return RuntimeConfig.merge(assembly,
                    RuntimeConfig.hooks(List.of(new SessionQuotaHook(properties.sessionQuota(), stats))));
        }
        return assembly;
    }

    /**
     * 配置矛盾 fail-fast（impl-44 / spec 14 §A，对齐 core BuzhouConfigurationException 文化）：
     * 非法组合启动即失败并给修法，而非运行期静默错 behave。
     */
    private static void validate(ResilienceProperties p) {
        if (p.deadline() != null && !p.deadline().isZero() && p.deadline().compareTo(p.maxBackoff()) < 0) {
            throw new BuzhouConfigurationException(
                    "buzhou.resilience.deadline（" + p.deadline() + "）小于 maxBackoff（" + p.maxBackoff() + "）："
                            + "单次重试退避即可耗尽整个调用预算，重试形同虚设",
                    "把 deadline 调到 >= maxBackoff（或把 maxBackoff 调小），例如 deadline=60s、maxBackoff=10s");
        }
        ResilienceProperties.RateLimit rl = p.rateLimit();
        if (rl != null) {
            if (rl.requestsPerMinute() != null && rl.requestsPerMinute() < 1) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.rate-limit.requests-per-minute（" + rl.requestsPerMinute() + "）必须 >= 1",
                        "删除该键（不限）或设为正整数");
            }
            if (rl.tokensPerMinute() != null && rl.tokensPerMinute() < 1) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.rate-limit.tokens-per-minute（" + rl.tokensPerMinute() + "）必须 >= 1",
                        "删除该键（不限）或设为正整数");
            }
            if (rl.queueTimeout() != null && rl.queueTimeout().isNegative()) {
                throw new BuzhouConfigurationException(
                        "buzhou.resilience.rate-limit.queue-timeout（" + rl.queueTimeout() + "）不能为负",
                        "删除该键（默认 30s）或设为非负时长");
            }
            if (rl.overloadPolicy() != null && !rl.overloadPolicy().isBlank()) {
                try {
                    io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.valueOf(
                            rl.overloadPolicy().trim().toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new BuzhouConfigurationException(
                            "buzhou.resilience.rate-limit.overload-policy（" + rl.overloadPolicy() + "）非法",
                            "取值只能是 QUEUE 或 FAIL_FAST");
                }
            }
        }
    }

    static final class ResilienceAssemblyCustomizer implements SessionAssemblyCustomizer {
        private final ResilienceProperties properties;
        private final ProviderErrorClassifier classifier;
        private final String modelName;
        private final ResilienceStats stats;
        private final ModelCircuitBreaker circuit;
        private final FallbackChain fallback;
        private final ModelRateLimiter limiter;
        private final io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow;
        private final io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore responseCacheStore;

        ResilienceAssemblyCustomizer(ResilienceProperties properties, ProviderErrorClassifier classifier,
                                     String modelName, ResilienceStats stats, ModelCircuitBreaker circuit,
                                     FallbackChain fallback, ModelRateLimiter limiter,
                                     io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow,
                                     io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore responseCacheStore) {
            this.properties = properties;
            this.classifier = classifier;
            this.modelName = modelName;
            this.stats = stats;
            this.circuit = circuit;
            this.fallback = fallback;
            this.limiter = limiter;
            this.shadow = shadow;
            this.responseCacheStore = responseCacheStore;
        }

        @Override
        public void customize(SessionAssemblyContext ctx) {
            // 精确响应缓存（spec 53 §A / T203）：order +450 先于 observability(+500)/resilience(+700)
            // ——命中短路两者（无模型调用即无 span/熔断窗；命中可观测走 store 计数）。
            if (responseCacheStore != null) {
                ctx.addAdvisor(new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheAdvisor(
                        responseCacheStore, modelName));
            }
            // 限流 Advisor（spec 15「背压 · 维度③」）：先于 ResilienceAdvisor 注入（order +650 < +700）。
            // impl-59：limiter 为进程级共享（configure() 创建），本 advisor 每会话持有会话事件通道。
            if (limiter != null) {
                ctx.addAdvisor(new RateLimitAdvisor(limiter, modelName, stats, ctx::emitEvent));
            }
            // 虚拟线程执行器：deadline 兜底 + cancel 中断在途模型调用复用同一条路径。
            // 每会话一个，随会话关闭由 ResilienceSessionObserver.shutdownNow()。
            ExecutorService deadlineExecutor = Executors.newVirtualThreadPerTaskExecutor();
            ModelCallInFlight inFlight = new ModelCallInFlight();
            ResilienceAdvisor advisor = new ResilienceAdvisor(
                    properties, classifier, ctx::emitEvent, deadlineExecutor, inFlight, stats, circuit, modelName,
                    fallback, shadow, limiter);
            ctx.addAdvisor(advisor);
            // onCancel 中断在途模型调用（补 session.cancel() 漏网）；onClose 关执行器防泄漏。
            ctx.addObserver(new ResilienceSessionObserver(deadlineExecutor, inFlight));
        }
    }
}
