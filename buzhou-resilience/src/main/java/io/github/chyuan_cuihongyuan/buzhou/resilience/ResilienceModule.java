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
        if (!properties.enabled()) {
            return RuntimeConfig.defaults();
        }
        validate(properties);
        ProviderErrorClassifier classifier = new DefaultErrorClassifier();
        // 熔断器（impl-56）：进程级单例——provider 健康是进程级事实，configure 每 context 一次、
        // 经 customizer 闭包注入全部会话（不能在 customize() 内建，否则退化为每会话分桶）。
        ModelCircuitBreaker circuit = properties.circuit().effectiveEnabled()
                ? new ModelCircuitBreaker(properties.circuit(), stats)
                : null;
        FallbackChain fallbackChain = fallbacks != null && !fallbacks.isEmpty()
                ? new FallbackChain(fallbacks, properties.fallback())
                : null;
        return RuntimeConfig.assemblyCustomizers(
                List.of(new ResilienceAssemblyCustomizer(properties, classifier, modelName, stats, circuit,
                        fallbackChain)));
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

        ResilienceAssemblyCustomizer(ResilienceProperties properties, ProviderErrorClassifier classifier,
                                     String modelName, ResilienceStats stats, ModelCircuitBreaker circuit,
                                     FallbackChain fallback) {
            this.properties = properties;
            this.classifier = classifier;
            this.modelName = modelName;
            this.stats = stats;
            this.circuit = circuit;
            this.fallback = fallback;
        }

        @Override
        public void customize(SessionAssemblyContext ctx) {
            // 限流 Advisor（spec 15「背压 · 维度③」）：先于 ResilienceAdvisor 注入（order +650 < +700）
            ResilienceProperties.RateLimit rl = properties.rateLimit();
            if (rl != null) {
                ModelRateLimiter limiter = new ModelRateLimiter(
                        rl.requestsPerMinute(),
                        rl.tokensPerMinute(),
                        rl.queueTimeout(),
                        properties.effectiveRateLimitOverloadPolicy(),
                        ctx::emitEvent);
                if (limiter.isEnabled()) {
                    ctx.addAdvisor(new RateLimitAdvisor(limiter, modelName, stats));
                }
            }
            // 虚拟线程执行器：deadline 兜底 + cancel 中断在途模型调用复用同一条路径。
            // 每会话一个，随会话关闭由 ResilienceSessionObserver.shutdownNow()。
            ExecutorService deadlineExecutor = Executors.newVirtualThreadPerTaskExecutor();
            ModelCallInFlight inFlight = new ModelCallInFlight();
            ResilienceAdvisor advisor = new ResilienceAdvisor(
                    properties, classifier, ctx::emitEvent, deadlineExecutor, inFlight, stats, circuit, modelName,
                    fallback);
            ctx.addAdvisor(advisor);
            // onCancel 中断在途模型调用（补 session.cancel() 漏网）；onClose 关执行器防泄漏。
            ctx.addObserver(new ResilienceSessionObserver(deadlineExecutor, inFlight));
        }
    }
}
