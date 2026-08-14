package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-44 生产级加固面测试：运维计数器（ResilienceStats）经真实重试路径累积、
 * 配置矛盾 fail-fast（BuzhouConfigurationException 带修法）。
 */
class ResilienceHardeningTest {

    /** 一次 NETWORK 重试后成功：stats 记录 retryAttempts=1、最近分类 NETWORK，未记耗尽。 */
    @Test
    void statsRecordRetryJourney() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError());
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = fastBackoff();
        ResilienceStats stats = new ResilienceStats();

        AgentSession session = newRuntime(model, props, stats);
        assertThat(session.chat("hi")).isEqualTo("ok");
        session.close();

        assertThat(stats.retryAttempts()).isEqualTo(1);
        assertThat(stats.retryExhausted()).isZero();
        assertThat(stats.details()).containsEntry("lastErrorCategory", "NETWORK");
        assertThat(stats.status()).isEqualTo(BuzhouHealthStatusUp());
        assertThat(stats.mechanism()).isEqualTo("resilience");
    }

    /** 重试耗尽：stats 记录 exhausted，最近分类保持最后一次错误类别。 */
    @Test
    void statsRecordRetryExhaustion() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError());
        model.enqueueThrow(networkError());
        model.enqueueThrow(networkError());
        ResilienceProperties props = fastBackoff();
        ResilienceStats stats = new ResilienceStats();

        AgentSession session = newRuntime(model, props, stats);
        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(UncheckedIOException.class);
        session.close();

        assertThat(stats.retryExhausted()).isEqualTo(1);
        assertThat(stats.retryAttempts()).isEqualTo(2); // 3 次尝试 = 2 次重试
    }

    /** 健康委托语义：纯内存机制恒 UP；details 暴露计数快照。 */
    @Test
    void healthDelegateIsUpWithCounterDetails() {
        ResilienceStats stats = new ResilienceStats();
        assertThat(stats.status()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP);
        assertThat(stats.details()).containsEntry("retryAttempts", 0L).containsEntry("modelTimeouts", 0L);
    }

    /** 配置矛盾 fail-fast：deadline < maxBackoff 启动即失败（BuzhouConfigurationException 带修法）。 */
    @Test
    void deadlineSmallerThanMaxBackoffFailsFast() {
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofSeconds(10), 2.0, 0.0, null, Duration.ofSeconds(5), null);
        assertThatThrownBy(() -> ResilienceModule.configure(props))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("deadline")
                .hasMessageContaining("maxBackoff");
    }

    /** 非法数值 fail-fast：负 duration / 越界 jitter 构造即抛（带建议动作）。 */
    @Test
    void invalidValuesFailFastWithAction() {
        assertThatThrownBy(() -> new ResilienceProperties(true, 3,
                Duration.ofMillis(-1), null, null, null, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("initial-backoff");
        assertThatThrownBy(() -> new ResilienceProperties(true, 3,
                null, null, null, 1.5, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("jitter");
        assertThatThrownBy(() -> new ResilienceProperties(true, 0,
                null, null, null, null, null, null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("max-attempts");
    }

    /** 合法边界值不被误伤：jitter=0、deadline=0（关超时）、multiplier=1.0。 */
    @Test
    void legalBoundaryValuesAccepted() {
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofSeconds(10), 1.0, 0.0, null, Duration.ZERO, null);
        ResilienceStats stats = new ResilienceStats();
        // configure 不抛即通过（deadline=0 关闭超时合法）
        ResilienceModule.configure(props, "m", stats);
    }

    // ---- helpers ----

    private static io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status BuzhouHealthStatusUp() {
        return io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP;
    }

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props,
            ResilienceStats stats) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props, "test-model", stats));
        return runtime.spawn("app", "resilience-agent", "sess-" + System.nanoTime());
    }

    private static ResilienceProperties fastBackoff() {
        return new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);
    }

    private static UncheckedIOException networkError() {
        return new UncheckedIOException(new IOException("connection reset"));
    }
}
