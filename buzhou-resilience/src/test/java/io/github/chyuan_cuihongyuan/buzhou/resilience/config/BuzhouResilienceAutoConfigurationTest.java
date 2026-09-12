package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 韧性层自装配测试：默认开、yml 参数绑定、enabled=false 回退（对齐各模块 {@code *AutoConfigurationTest}）。
 */
class BuzhouResilienceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouResilienceAutoConfiguration.class));

    @Test
    void enabledByDefaultRegistersCustomizer() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RuntimeConfig.class);
            assertThat(context.getBean(RuntimeConfig.class).assemblyCustomizers()).hasSize(1);
        });
    }

    @Test
    void disabledDropsCustomizer() {
        // enabled=false 命中 @ConditionalOnProperty 的负向匹配：整个 AutoConfiguration 不装载，
        // 不贡献 RuntimeConfig bean——等同于「不引入本模块」，回退底座原生行为。
        runner.withPropertyValues("buzhou.resilience.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RuntimeConfig.class);
        });
    }

    @Test
    void ymlOverridesBindToProperties() {
        runner.withPropertyValues(
                "buzhou.resilience.max-attempts=7",
                "buzhou.resilience.initial-backoff=2s",
                "buzhou.resilience.jitter=0").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.maxAttempts()).isEqualTo(7);
            assertThat(props.initialBackoff()).isEqualTo(Duration.ofSeconds(2));
            assertThat(props.jitter()).isEqualTo(0.0);
        });
    }

    // ---- 限流配置绑定（spec「背压 · 维度③」） ----

    @Test
    void rateLimitDefaultsToNull() {
        runner.run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.rateLimit()).isNull();  // null = 不限（safe-by-default）
        });
    }

    @Test
    void rateLimitYmlOverridesBindToProperties() {
        runner.withPropertyValues(
                "buzhou.resilience.rate-limit.requests-per-minute=100",
                "buzhou.resilience.rate-limit.tokens-per-minute=50000",
                "buzhou.resilience.rate-limit.queue-timeout=10s",
                "buzhou.resilience.rate-limit.overload-policy=FAIL_FAST"
        ).run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.rateLimit()).isNotNull();
            assertThat(props.rateLimit().requestsPerMinute()).isEqualTo(100);
            assertThat(props.rateLimit().tokensPerMinute()).isEqualTo(50000);
            assertThat(props.rateLimit().queueTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(props.effectiveRateLimitOverloadPolicy())
                    .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.FAIL_FAST);
        });
    }

    /** spec 620 / T916 补验：circuit.time-window yml 绑定（多构造 record canonical 已标注）。 */
    @Test
    void circuitTimeWindowYmlBinds() {
        runner.withPropertyValues(
                "buzhou.resilience.circuit.time-window=90s"
        ).run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.circuit()).isNotNull();
            assertThat(props.circuit().timeWindow()).isEqualTo(Duration.ofMinutes(1).plusSeconds(30));
        });
    }

    /** spec 614 / T878：smoothing 与 gcra-burst-tolerance yml 绑定（多构造 record @ConstructorBinding）。 */
    @Test
    void rateLimitSmoothingYmlBinds() {
        runner.withPropertyValues(
                "buzhou.resilience.rate-limit.requests-per-minute=60",
                "buzhou.resilience.rate-limit.smoothing=gcra",
                "buzhou.resilience.rate-limit.gcra-burst-tolerance=5s"
        ).run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.rateLimit()).isNotNull();
            assertThat(props.rateLimit().isGcraSmoothing()).isTrue();
            assertThat(props.rateLimit().gcraBurstTolerance()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    // ---- 金丝雀 / shadow 配置绑定（spec 48 §B / 49 §A / T187 元数据入档） ----

    @Test
    void canaryAndShadowDefaultToOff() {
        runner.run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            // fallback 未配置 = null（金丝雀天然关）；shadow 未配置归一为关 = 零提交零事件零计数
            assertThat(props.fallback()).isNull();
            assertThat(props.shadow().effectiveEnabled()).isFalse();
            assertThat(props.shadow().maxConcurrent()).isEqualTo(2);
            assertThat(props.shadow().dailyBudget()).isEqualTo(1000L);
        });
    }

    @Test
    void canaryAndShadowYmlOverridesBindToProperties() {
        // shadow.models 按名解析 fail-fast（spec 49 §A）：提供同名 bean 供装配校验命中
        runner.withBean("shadowModel",
                        io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel.class,
                        io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel::new)
                .withPropertyValues(
                "buzhou.resilience.fallback.canary-enabled=true",
                "buzhou.resilience.fallback.weights.primary=9",
                "buzhou.resilience.fallback.weights.candidate=1",
                "buzhou.resilience.shadow.enabled=true",
                "buzhou.resilience.shadow.models=shadowModel",
                "buzhou.resilience.shadow.max-concurrent=4",
                "buzhou.resilience.shadow.daily-budget=77"
        ).run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.fallback().canaryEnabled()).isTrue();
            assertThat(props.fallback().weights())
                    .containsEntry("primary", 9)
                    .containsEntry("candidate", 1);
            assertThat(props.shadow().effectiveEnabled()).isTrue();
            assertThat(props.shadow().models()).containsExactly("shadowModel");
            assertThat(props.shadow().maxConcurrent()).isEqualTo(4);
            assertThat(props.shadow().dailyBudget()).isEqualTo(77L);
        });
    }

    /** T187 勘察修复回归：circuit 多构造器 record 补 @ConstructorBinding 后 yml 键恢复生效。 */
    @Test
    void circuitYmlOverridesBindToProperties() {
        runner.withPropertyValues(
                "buzhou.resilience.circuit.window-size=40",
                "buzhou.resilience.circuit.half-open-success-threshold=3").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.circuit()).isNotNull();
            assertThat(props.circuit().windowSize()).isEqualTo(40);
            assertThat(props.circuit().halfOpenSuccessThreshold()).isEqualTo(3);
        });
    }

    // ---- 精确响应缓存（spec 53 §E / T207） ----

    @Test
    void responseCacheDefaultsToOff() {
        runner.run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.responseCache().effectiveEnabled()).isFalse();
            assertThat(props.responseCache().maxEntries()).isEqualTo(256);
            assertThat(props.responseCache().ttl()).isEqualTo(Duration.ofHours(1));
        });
    }

    @Test
    void responseCacheYmlOverridesBindAndFailFastOnInvalid() {
        runner.withPropertyValues(
                "buzhou.resilience.response-cache.enabled=true",
                "buzhou.resilience.response-cache.max-entries=128",
                "buzhou.resilience.response-cache.ttl=30m").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.responseCache().effectiveEnabled()).isTrue();
            assertThat(props.responseCache().maxEntries()).isEqualTo(128);
            assertThat(props.responseCache().ttl()).isEqualTo(Duration.ofMinutes(30));
        });
        // 非法值 fail-fast（max-entries < 1 / ttl 非正）
        runner.withPropertyValues(
                "buzhou.resilience.response-cache.enabled=true",
                "buzhou.resilience.response-cache.max-entries=0")
                .run(ctx -> assertThat(ctx).hasFailed());
        runner.withPropertyValues(
                "buzhou.resilience.response-cache.enabled=true",
                "buzhou.resilience.response-cache.ttl=0s")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    /** spec 641：coalescing yml 声明绑定（多构造 record canonical @ConstructorBinding 生效）；缺省关。 */
    @Test
    void responseCacheCoalescingBindsOptInAndDefaultsOff() {
        runner.run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.responseCache().effectiveCoalescing()).isFalse();
        });
        runner.withPropertyValues(
                "buzhou.resilience.response-cache.enabled=true",
                "buzhou.resilience.response-cache.coalescing=true").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.responseCache().effectiveEnabled()).isTrue();
            assertThat(props.responseCache().effectiveCoalescing()).isTrue();
        });
    }

    /**
     * spec 643 / T936：shadow 明细 JSONL 轮转档 yml 绑定——键缺席 = 默认 64MB×3；
     * 显式 0 = 关（Shadow record 5→7 参多构造 canonical 绑定钉住）。
     */
    @Test
    void shadowRollingKeysBindWithDefaultAndExplicitOff() {
        runner.run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.shadow().effectiveDetailMaxBytes()).isEqualTo(
                    io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_BYTES);
            assertThat(props.shadow().effectiveDetailMaxHistory()).isEqualTo(
                    io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_HISTORY);
        });
        runner.withPropertyValues(
                "buzhou.resilience.shadow.detail-max-bytes=0",
                "buzhou.resilience.shadow.detail-max-history=0").run(context -> {
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.shadow().effectiveDetailMaxBytes()).isZero();
            assertThat(props.shadow().effectiveDetailMaxHistory()).isZero();
        });
    }
}
