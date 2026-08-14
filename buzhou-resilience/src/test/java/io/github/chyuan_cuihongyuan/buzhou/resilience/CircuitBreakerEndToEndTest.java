package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
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
 * 熔断器端到端（spec 15「熔断器」，impl-56）：经完整 advisor 链驱动——连续失败跳闸后
 * <b>模型零调用</b>快速失败；跨会话共享（进程级）；半开探测恢复；AUTH 不跳闸；流式入口拒绝。
 */
class CircuitBreakerEndToEndTest {

    /** 连续 NETWORK 终态失败达阈值跳闸：下一轮模型零调用、快速失败异常 + 事件上报。 */
    @Test
    void tripsAndFailsFastWithoutTouchingModel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = props(circuit(10, 2, 0.5, 60_000));

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi-1")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> session.chat("hi-2")).isInstanceOf(UncheckedIOException.class);
        assertThat(model.seenPrompts).hasSize(2);

        // 第三轮：熔断 OPEN——模型零调用（seenPrompts 不再增长），快速失败异常直上。
        assertThatThrownBy(() -> session.chat("hi-3"))
                .isInstanceOf(ModelCircuitOpenException.class)
                .hasMessageContaining("OPEN");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> ModelCircuitBreaker.EVENT_CALL_REJECTED.equals(e.type()));
        assertThat(events).anyMatch(e -> ModelCircuitBreaker.EVENT_STATE_CHANGED.equals(e.type())
                && "OPEN".equals(e.payload().get("to")));
        session.close();
    }

    /** 进程级共享：同一 runtime 下会话 A 跳闸，会话 B 首轮即被拒（B 的模型调用从未发出）。 */
    @Test
    void openStateSharedAcrossSessionsOfSameRuntime() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = props(circuit(10, 2, 0.5, 60_000));

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props));

        AgentSession sessionA = runtime.spawn("app", "agent", "sess-a");
        assertThatThrownBy(() -> sessionA.chat("hi")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> sessionA.chat("hi")).isInstanceOf(UncheckedIOException.class);
        sessionA.close();

        AgentSession sessionB = runtime.spawn("app", "agent", "sess-b");
        List<SessionEvent> eventsB = listen(sessionB);
        assertThatThrownBy(() -> sessionB.chat("hi")).isInstanceOf(ModelCircuitOpenException.class);
        assertThat(model.seenPrompts).hasSize(2); // B 从未触达模型
        assertThat(eventsB).anyMatch(e -> ModelCircuitBreaker.EVENT_CALL_REJECTED.equals(e.type()));
        sessionB.close();
    }

    /** 半开恢复：冷却后首个调用作为探测，成功回 CLOSED、后续调用正常直达模型。 */
    @Test
    void halfOpenProbeRecoversAfterCooldown() throws InterruptedException {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        model.enqueue(new AssistantMessage("recovered-1"));
        model.enqueue(new AssistantMessage("recovered-2"));
        ResilienceProperties props = props(circuit(10, 2, 0.5, 80));

        AgentSession session = newRuntime(model, props);
        assertThatThrownBy(() -> session.chat("hi-1")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> session.chat("hi-2")).isInstanceOf(UncheckedIOException.class);

        Thread.sleep(150); // 过冷却（80ms）
        assertThat(session.chat("hi-3")).isEqualTo("recovered-1"); // 探测成功 → CLOSED
        assertThat(session.chat("hi-4")).isEqualTo("recovered-2"); // 后续正常
        assertThat(model.seenPrompts).hasSize(4);
        session.close();
    }

    /** AUTH（401）为 IGNORED：连续鉴权失败不跳闸，调用仍直达模型（快速暴露配置错误）。 */
    @Test
    void authFailuresDoNotTripCircuit() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(httpError(401));
        model.enqueueThrow(httpError(401));
        model.enqueueThrow(httpError(401));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = props(circuit(10, 2, 0.5, 60_000));

        AgentSession session = newRuntime(model, props);
        for (int i = 0; i < 3; i++) {
            int attempt = i;
            assertThatThrownBy(() -> session.chat("hi-" + attempt))
                    .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
        }
        assertThat(model.seenPrompts).hasSize(3); // 每次都直达模型，从未被熔断拦截
        assertThat(session.chat("hi-4")).isEqualTo("ok");
        session.close();
    }

    /** 流式入口拒绝：OPEN 期 stream() 直接 Flux.error（不对故障 provider 开流）。 */
    @Test
    void streamRejectedWhenOpen() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = props(circuit(10, 2, 0.5, 60_000));

        AgentSession session = newRuntime(model, props);
        assertThatThrownBy(() -> session.chat("hi-1")).isInstanceOf(UncheckedIOException.class);
        assertThatThrownBy(() -> session.chat("hi-2")).isInstanceOf(UncheckedIOException.class);

        assertThatThrownBy(() -> session.stream("hi-3").blockLast())
                .hasStackTraceContaining("ModelCircuitOpenException");
        assertThat(model.seenPrompts).hasSize(2);
        session.close();
    }

    // ---- helpers ----

    private static ResilienceProperties.Circuit circuit(int window, int minCalls, double threshold,
            long cooldownMs) {
        return new ResilienceProperties.Circuit(null, window, minCalls, threshold,
                Duration.ofMillis(cooldownMs), null);
    }

    private static ResilienceProperties props(ResilienceProperties.Circuit circuit) {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, circuit);
    }

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props));
        return runtime.spawn("app", "resilience-agent", "sess-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    private static org.springframework.web.client.HttpClientErrorException httpError(int status) {
        return org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatusCode.valueOf(status), "provider error",
                org.springframework.http.HttpHeaders.EMPTY, null, null);
    }
}
