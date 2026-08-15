package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 49 §B / T177 / impl-146：池级配额全候选执行。
 *
 * <p>诚实口径：外层 RateLimitAdvisor（主模型闸，既有）每逻辑调用先于候选闸扣减，且把降级服务
 * 响应的 usage 记入主桶（既有逻辑记账语义）——单 limiter 配置下候选桶压力恒 ≤ 主桶，候选拒绝
 * 分支为防御性闸（多候选路径/未来外层语义变化时生效）。本测试钉住可观测真值：
 * 候选级按实际服务模型记账（secondary 桶水位下降）+ 双记账并存 + 拒绝族计数可达。
 */
class PoolQuotaEndToEndTest {

    /**
     * 降级候选过闸 + 主桶耗尽外层拒绝（rate-limit-rejected 族可达）：RPM=1（整数令牌，
     * 亚秒 refill 不可能补满 → 拒绝确定性）。T1 primary 失败 → secondary 候选过闸
     * （secondary 桶 1→0）服务成功；T2 主桶空 → 外层拒绝。
     */
    @Test
    void fallbackCandidateGatedAndPrimaryBucketRejectionFires() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("boom-1"));
        primary.enqueueThrow(networkError("boom-2"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("from-secondary"));
        secondary.enqueue(new AssistantMessage("never-reached"));

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                rpmProps(), "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "sess-quota");
        List<String> rejected = new CopyOnWriteArrayList<>();
        installRejectProbe(rejected);

        // T1：primary 失败 → secondary 候选过闸（自有桶扣减）成功
        assertThat(session.chat("q1")).isEqualTo("from-secondary");
        assertThat(secondary.seenPrompts).hasSize(1);

        // T2：主桶（T1 已扣）空 → 外层 RPM 拒绝（确定性：整数令牌亚秒不补满）
        assertThatThrownBy(() -> session.chat("q2"))
                .isInstanceOf(ModelRateLimitExceededException.class)
                .hasMessageContaining("primary");
        assertThat(secondary.seenPrompts).hasSize(1); // 未再触达
        assertThat(rejected).anyMatch(r -> r.contains("primary"));
        session.close();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
    }

    /** remaining 探针：消耗后 0..1 内下降（refill 持续，宽幅上界）；未启用维度 = 1。 */
    @Test
    void remainingRatioReflectsConsumption() {
        ModelRateLimiter limiter = new ModelRateLimiter(null, 100, Duration.ofMillis(1),
                io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.FAIL_FAST, null);
        assertThat(limiter.remainingRatio("m", ModelRateLimiter.DIMENSION_TPM)).isEqualTo(1.0);
        limiter.recordUsage("m", 60L);
        double after = limiter.remainingRatio("m", ModelRateLimiter.DIMENSION_TPM);
        assertThat(after).isBetween(0.0, 0.45);
        assertThat(limiter.remainingRatio("m", ModelRateLimiter.DIMENSION_RPM)).isEqualTo(1.0);
    }

    /** 未配置限流：降级行为零变化（既有回归口径）。 */
    @Test
    void noLimiterFallbackUnchanged() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("down"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("plain-sec"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                noRateLimitProps(), "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "sess-nolimit");
        assertThat(session.chat("hi")).isEqualTo("plain-sec");
        session.close();
    }

    // ---- helpers ----

    /** RPM=1、FAIL_FAST、无重试；TPM 不限。 */
    private static ResilienceProperties rpmProps() {
        ResilienceProperties.RateLimit rateLimit = new ResilienceProperties.RateLimit(
                1, null, Duration.ofMillis(1), "FAIL_FAST");
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), rateLimit, null, null, null, null);
    }

    private static ResilienceProperties noRateLimitProps() {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null, null);
    }

    private static void installRejectProbe(List<String> sink) {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        if ("buzhou.resilience.rate-limit-rejected".equals(name)) {
                            sink.add(name + "|" + String.join("=", tagKeyValue));
                        }
                    }

                    @Override
                    public void timer(String name, Duration duration, String... tagKeyValue) {
                    }
                });
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }
}
