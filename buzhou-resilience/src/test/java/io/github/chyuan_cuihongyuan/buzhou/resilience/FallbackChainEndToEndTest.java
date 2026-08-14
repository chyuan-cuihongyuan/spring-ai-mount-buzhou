package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
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
 * 备模型降级链端到端（spec 15「备模型降级链」，impl-57）：主模型终态失败后同一逻辑调用内
 * 切换备模型；熔断 OPEN 恒触发（主模型零重试直达备模型）；全败上抛主因；CONTENT 不触发。
 */
class FallbackChainEndToEndTest {

    /** 主模型 NETWORK 终态失败（重试耗尽）→ 备模型接管，用户拿到备模型回复 + switched 事件。 */
    @Test
    void primaryFailureFallsBackToSecondaryModel() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("primary down"));
        primary.enqueueThrow(networkError("primary down"));
        primary.enqueueThrow(networkError("primary down"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("from-secondary"));

        AgentSession session = newRuntime(primary, secondary, "primary", 3);
        List<SessionEvent> events = listen(session);

        String reply = session.chat("hi");
        assertThat(reply).as("events=%s", events).isEqualTo("from-secondary");
        assertThat(primary.seenPrompts).hasSize(3); // 首调 + 2 重试（重试预算全给主模型）
        assertThat(secondary.seenPrompts).hasSize(1); // 备模型单次尝试
        assertThat(events).anyMatch(e -> FallbackChain.EVENT_SWITCHED.equals(e.type())
                && "primary".equals(e.payload().get("from"))
                && "secondary".equals(e.payload().get("to"))
                && "NETWORK".equals(e.payload().get("category")));
        session.close();
    }

    /** CB+降级组合拳：主模型熔断跳闸后（两轮失败），后续请求零重试直达备模型（primary 调用数不再增长）。 */
    @Test
    void circuitOpenRoutesStraightToFallback() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("boom-1"));
        primary.enqueueThrow(networkError("boom-2"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("sec-1"));
        secondary.enqueue(new AssistantMessage("sec-2"));
        secondary.enqueue(new AssistantMessage("sec-3"));

        AgentSession session = newRuntime(primary, secondary, "primary", 1);
        List<SessionEvent> events = listen(session);

        // 前两轮：主模型失败（重试耗尽后失败样本入熔断窗口）→ 降级成功，用户拿到备模型回复
        assertThat(session.chat("hi-1")).isEqualTo("sec-1");
        assertThat(session.chat("hi-2")).isEqualTo("sec-2"); // 第二个失败样本 → 熔断跳闸
        assertThat(primary.seenPrompts).hasSize(2);

        // 第三轮：主模型熔断 OPEN → 恒触发降级（CIRCUIT_OPEN），primary 零调用
        assertThat(session.chat("hi-3")).isEqualTo("sec-3");
        assertThat(primary.seenPrompts).hasSize(2); // 未增长——熔断 OPEN 后不再触达主模型
        assertThat(events).anyMatch(e -> FallbackChain.EVENT_SWITCHED.equals(e.type())
                && "CIRCUIT_OPEN".equals(e.payload().get("category")));
        session.close();
    }

    /** 备模型链全败：上抛主模型原始错误（根因不遮蔽）+ exhausted 事件。 */
    @Test
    void exhaustedChainRethrowsPrimaryError() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("primary root cause"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueThrow(networkError("secondary also down"));

        AgentSession session = newRuntime(primary, secondary, "primary", 1);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("primary root cause");
        assertThat(events).anyMatch(e -> FallbackChain.EVENT_EXHAUSTED.equals(e.type()));
        assertThat(secondary.seenPrompts).hasSize(1);
        session.close();
    }

    /** CONTENT 静默拒绝不触发降级（防策略跳舱）：返回原响应、备模型零调用。 */
    @Test
    void contentRefusalDoesNotTriggerFallback() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.script.add(refusalResponse());
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("never-used"));

        AgentSession session = newRuntime(primary, secondary, "primary", 3);
        List<SessionEvent> events = listen(session);

        session.chat("hi"); // 静默拒绝：响应原样返回
        assertThat(secondary.seenPrompts).isEmpty(); // 未降级
        assertThat(events).noneMatch(e -> FallbackChain.EVENT_SWITCHED.equals(e.type()));
        session.close();
    }

    /** 无备模型时行为与 impl-56 一致：熔断 OPEN 拒绝直上（无降级路径）。 */
    @Test
    void withoutFallbackCircuitOpenStillFailsFast() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("boom-1"));
        primary.enqueueThrow(networkError("boom-2"));

        BuzhouStores stores = Buzhou.inMemoryStores();
        ResilienceProperties props = props(1);
        AgentRuntime runtime = Buzhou.runtime(primary, stores,
                ResilienceModule.configure(props, "primary", new ResilienceStats()));
        AgentSession session = runtime.spawn("app", "agent", "sess");
        assertThatThrownBy(() -> session.chat("hi-1")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> session.chat("hi-2")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> session.chat("hi-3"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException.class);
        session.close();
    }

    /** 运维计数：切换与耗尽入账。 */
    @Test
    void statsRecordSwitches() {
        ResilienceStats stats = new ResilienceStats();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("down"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("ok"));

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props(1), "primary", stats, List.of(new NamedFallbackModel("secondary", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "sess");
        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(stats.details()).containsEntry("fallbackSwitches", 1L);
        assertThat(stats.details()).containsEntry("fallbackExhausted", 0L);
        session.close();
    }

    // ---- helpers ----

    private static AgentSession newRuntime(ScriptedChatModel primary, ScriptedChatModel secondary,
            String modelName, int maxAttempts) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props(maxAttempts), modelName, new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        return runtime.spawn("app", "agent", "sess-" + System.nanoTime());
    }

    /** maxAttempts=1（无重试）+ 快熔断（window=10/min=2/threshold=0.5/cooldown 长）。 */
    private static ResilienceProperties props(int maxAttempts) {
        ResilienceProperties.Circuit circuit = new ResilienceProperties.Circuit(
                null, 10, 2, 0.5, Duration.ofSeconds(60), null);
        return new ResilienceProperties(true, maxAttempts, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, circuit, null);
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    /** 内容拒绝响应（元数据 finishReason=content_filter，静默通道——与 ResilienceEndToEndTest 同口径）。 */
    private static org.springframework.ai.chat.model.ChatResponse refusalResponse() {
        org.springframework.ai.chat.metadata.ChatGenerationMetadata metadata =
                org.springframework.ai.chat.metadata.ChatGenerationMetadata.builder()
                        .finishReason("content_filter")
                        .build();
        return new org.springframework.ai.chat.model.ChatResponse(List.of(
                new org.springframework.ai.chat.model.Generation(new AssistantMessage(""), metadata)));
    }
}
