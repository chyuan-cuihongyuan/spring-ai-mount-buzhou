package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 模型 RPM+TPM 双桶限流端到端测试（spec「背压 · 维度③」）。
 *
 * <p>复用 {@code ResilienceEndToEndTest} 的装配形态（{@code Buzhou.runtime} + {@code ScriptedChatModel}
 * + 调用计数断言），断言 RPM 排队/拒绝、TPM 按 usage 记账、自限流不触发重试、事件留痕。
 */
class RateLimitEndToEndTest {

    /** 低 RPM 下第 N 次调用被拒绝（FAIL_FAST 档）。 */
    @Test
    void rpmFailFastRejectsThirdCall() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("first");
        model.enqueueText("second");
        // rpm=2, FAIL_FAST：前 2 次放行，第 3 次拒绝
        ResilienceProperties props = rateLimitProps(2, null, "FAIL_FAST");
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("first");
        assertThat(session.chat("hi")).isEqualTo("second");
        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(ModelRateLimitExceededException.class)
                .hasMessageContaining("RPM");
        // 自限流拒绝不触发模型调用
        assertThat(model.seenPrompts).hasSize(2);
        session.close();
    }

    /** 自限流拒绝不触发重试（调用计数不放大）。 */
    @Test
    void selfRateLimitRejectionDoesNotTriggerRetry() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        // rpm=1, FAIL_FAST, maxAttempts=5（即使配了重试，自限流拒绝也不重试）
        ResilienceProperties props = new ResilienceProperties(true, 5,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                new ResilienceProperties.RateLimit(1, null, Duration.ofMillis(100), "FAIL_FAST"));
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("ok");
        // 第 2 次被 RPM 拒绝——不触发模型调用、不重试
        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(ModelRateLimitExceededException.class);
        assertThat(model.seenPrompts).hasSize(1);  // 恰好 1 次（无重试放大）
        session.close();
    }

    /** 预排 usage 后 TPM 记账生效——第 N 次调用被 TPM 桶拦（RPM 桶未满而 TPM 桶空）。 */
    @Test
    void tpmRecordedFromUsageBlocksNextCall() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 前两次调用各返回 80 tokens usage：tpm=100 → 第一次桶余 20，第二次桶 = 20-80 = -60
        // 第三次预检 -60 <= 0 → 拒绝
        model.script.add(usageResponse("first", 40, 40));
        model.script.add(usageResponse("second", 40, 40));
        model.enqueueText("third");
        ResilienceProperties props = rateLimitProps(1000, 100, "FAIL_FAST");
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("first");
        assertThat(session.chat("hi")).isEqualTo("second");
        // 第三次被 TPM 拦（RPM 桶远未满）
        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(ModelRateLimitExceededException.class)
                .hasMessageContaining("TPM");
        session.close();
    }

    /** RPM QUEUE 档排队超时后抛异常。 */
    @Test
    void rpmQueueTimeoutThrowsException() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("first");
        // rpm=1, QUEUE, queueTimeout=100ms：第 2 次排队等令牌补充，100ms 后超时
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                new ResilienceProperties.RateLimit(1, null, Duration.ofMillis(100), "QUEUE"));
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("first");
        long start = System.nanoTime();
        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(ModelRateLimitExceededException.class)
                .hasMessageContaining("RPM");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        // 排队了约 100ms
        assertThat(elapsedMs).isGreaterThanOrEqualTo(50);
        session.close();
    }

    /** backpressure.model-rejected 事件带 modelName、桶维度。 */
    @Test
    void modelRejectedEventHasModelNameAndDimension() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("first");
        ResilienceProperties props = rateLimitProps(1, null, "FAIL_FAST");
        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        session.chat("hi");
        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(ModelRateLimitExceededException.class);

        assertThat(events).anyMatch(e ->
                ModelRateLimiter.EVENT_MODEL_REJECTED.equals(e.type())
                        && "RPM".equals(e.payload().get("dimension"))
                        && "test-model".equals(e.payload().get("modelName")));
        session.close();
    }

    /** 不配置限流时行为与现状一致（回归断言）。 */
    @Test
    void noRateLimitByDefaultBehaviorUnchanged() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a");
        model.enqueueText("b");
        model.enqueueText("c");
        // 全默认：无限流
        ResilienceProperties props = ResilienceProperties.defaults();
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("a");
        assertThat(session.chat("hi")).isEqualTo("b");
        assertThat(session.chat("hi")).isEqualTo("c");
        session.close();
    }

    // ---- helpers ----

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props, "test-model"));
        return runtime.spawn("app", "ratelimit-agent", "sess-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    private static ResilienceProperties rateLimitProps(Integer rpm, Integer tpm, String policy) {
        return new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                new ResilienceProperties.RateLimit(rpm, tpm, Duration.ofMillis(100), policy));
    }

    /** 构造带 usage 的 ChatResponse（prompt + completion tokens）。 */
    private static ChatResponse usageResponse(String content, int promptTokens, int completionTokens) {
        DefaultUsage usage = new DefaultUsage(promptTokens, completionTokens);
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))), metadata);
    }
}
