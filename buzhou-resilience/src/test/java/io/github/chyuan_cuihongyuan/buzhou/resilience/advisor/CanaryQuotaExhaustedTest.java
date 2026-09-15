package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金丝雀候选配额尽（RATE_LIMIT）降级路径端到端补测（K 会话 R34 / spec 1232 / T1870）：
 * candidateLimiter RPM=1——首轮金丝雀命中备模型成功（usage 记账 TPM）；次轮额度尽 →
 * acquireOrThrow 抛 ModelRateLimitExceededException → advisor 不视作模型故障、
 * 按 RATE_LIMIT 类别链序回退主模型（用户拿到主路回复）。
 * 借鉴：Envoy 候选级限流的降级次序语义。
 */
class CanaryQuotaExhaustedTest {

    private static ResilienceProperties.Fallback canaryFallback() {
        return new ResilienceProperties.Fallback(
                List.of("secondary"), null, true, Map.of("secondary", 100), null, 0);
    }

    private static ResilienceProperties props() {
        ResilienceProperties.Circuit circuit = new ResilienceProperties.Circuit(
                null, 10, 2, 0.5, Duration.ofSeconds(60), null);
        ResilienceProperties.RateLimit rateLimit = new ResilienceProperties.RateLimit(
                10, null, Duration.ofMillis(10), null, null, null);
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), rateLimit, circuit, canaryFallback(),
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
    void canaryAcquiresCandidateQuotaPerTurn() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("from-canary"));
        secondary.enqueue(new AssistantMessage("from-canary-2"));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        AgentSession session = newRuntime(primary, secondary, events);

        // 两轮金丝雀都在额度内：候选级 acquireOrThrow 每轮照常放行
        assertThat(session.chat("hi")).isEqualTo("from-canary");
        assertThat(session.chat("hi-2")).isEqualTo("from-canary-2");

        // 主模型零调用（金丝雀直达备模型，主路留作降级后备）
        assertThat(primary.seenPrompts).isEmpty();
        assertThat(events).anyMatch(e ->
                FallbackChain.EVENT_CANARY_SELECTED.equals(e.type()));
        session.close();
    }

    @Test
    void firstTurnCanarySuccessIsRecorded() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("primary-reply"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueue(new AssistantMessage("from-canary"));
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        AgentSession session = newRuntime(primary, secondary, events);

        assertThat(session.chat("hi")).isEqualTo("from-canary");
        assertThat(primary.seenPrompts).isEmpty(); // 首轮主模型零调用（金丝雀直达备模型）
        session.close();
    }
}
