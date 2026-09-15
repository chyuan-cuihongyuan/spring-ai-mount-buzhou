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

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金丝雀路径端到端补测（K 会话 R32 / spec 1227+ / T1869——adviseCallCanary 6 +
 * degradeFromCanary 8 + recordCandidateUsage 13 missed 集中区）：canaryEnabled + 权重
 * 确定性路由到备模型、金丝雀终态失败后的链序回退（主模型在链首位）、canary-selected 事件。
 * 先例：FallbackChainEndToEndTest（多模型 runtime harness）。
 */
class CanaryPathEndToEndTest {

    /** canaryEnabled + 权重全给备模型：会话哈希确定性选中 secondary。 */
    private static ResilienceProperties.Fallback canaryFallback() {
        return new ResilienceProperties.Fallback(
                List.of("secondary"), null, true, Map.of("secondary", 100), null, 0);
    }

    private static ResilienceProperties props() {
        ResilienceProperties.Circuit circuit = new ResilienceProperties.Circuit(
                null, 10, 2, 0.5, Duration.ofSeconds(60), null);
        return new ResilienceProperties(true, 3, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, circuit, canaryFallback(),
                null, null, null, null, null, null);
    }

    private static AgentSession newRuntime(ScriptedChatModel primary, ScriptedChatModel secondary,
            List<SessionEvent> events) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props(), "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "sess-" + System.nanoTime());
        session.addEventListener(events::add);
        return session;
    }

    @Test
    void canaryRoutesToSecondaryAndSucceeds() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("from-primary"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("from-canary"));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        AgentSession session = newRuntime(primary, secondary, events);

        String reply = session.chat("hi");

        assertThat(reply).isEqualTo("from-canary");
        assertThat(events).anyMatch(e ->
                io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_CANARY_SELECTED
                        .equals(e.type()));
        assertThat(secondary.seenPrompts).hasSize(1);
        session.close();
    }

    @Test
    void canaryFailureDegradesToPrimaryByChainOrder() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueThrow(new UncheckedIOException(new java.io.IOException("canary down")));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        AgentSession session = newRuntime(primary, secondary, events);

        String reply = session.chat("hi");

        // 金丝雀终态失败 → 链序回退主模型（主模型在链首位）
        assertThat(reply).isEqualTo("primary-reply");
        assertThat(primary.seenPrompts).hasSize(1);
        assertThat(events).anyMatch(e ->
                FallbackChain.EVENT_SWITCHED.equals(e.type())
                        && "secondary".equals(e.payload().get("from"))
                        && "primary".equals(e.payload().get("to")));
        session.close();
    }

    @Test
    void canarySelectedEventCarriesModelAndSession() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("ok"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("also-ok"));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        AgentSession session = newRuntime(primary, secondary, events);

        session.chat("hi");

        var selected = events.stream()
                .filter(e -> io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain
                        .EVENT_CANARY_SELECTED.equals(e.type()))
                .findFirst()
                .orElseThrow();
        assertThat(selected.payload().get("model")).isEqualTo("secondary");
        assertThat(selected.payload()).containsKey("sessionId");
        session.close();
    }
}
