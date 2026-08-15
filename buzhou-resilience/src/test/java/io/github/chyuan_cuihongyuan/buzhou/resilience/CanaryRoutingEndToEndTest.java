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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 48 §B / T175 / impl-144：加权金丝雀端到端——会话稳定哈希加权分流、同会话粘住、
 * 金丝雀目标失败按链序回退（含原主模型）、默认关零变化。
 */
class CanaryRoutingEndToEndTest {

    /** 权重 1:9（primary:secondary）：大样本下 secondary 占多数（稳定哈希确定性，宽幅断言）。 */
    @Test
    void weightedSplitFavorsHeavyCandidateAndSticksPerSession() {
        int secondaryCount = 0;
        int sessions = 60;
        for (int i = 0; i < sessions; i++) {
            AgentSession session = spawnWeighted("primary", "secondary", Map.of("secondary", 9));
            String first = session.chat("q1");
            String second = session.chat("q2");
            // 同会话粘住：两轮同源
            assertThat(second).isEqualTo(first);
            if (first.equals("from-secondary")) {
                secondaryCount++;
            }
            session.close();
        }
        // 期望 90%（54/60）；稳定哈希确定性但避免算法锁死，宽幅 ±15pp
        assertThat(secondaryCount).isBetween(sessions * 75 / 100, sessions);
    }

    /** 金丝雀选中备模型后失败：按链序回退到主模型（switched from=secondary to=primary）。 */
    @Test
    void canaryTargetFailureDegradesBackToPrimary() {
        // 阶段一：固定回复模型探出「稳定选中 secondary」的会话 id（权重 1:99，20 样本内必中）
        String chosenId = null;
        for (int i = 0; i < 20 && chosenId == null; i++) {
            AgentSession probe = spawnWithModels(
                    new FixedReplyModel("from-primary"), new FixedReplyModel("from-secondary"),
                    canaryProps(Map.of("secondary", 99)), "pick-" + i);
            if (probe.chat("probe").equals("from-secondary")) {
                chosenId = "pick-" + i;
            }
            probe.close();
        }
        assertThat(chosenId).as("20 个样本内必有权重 99 的备模型命中").isNotNull();

        // 阶段二：同 id 在新 runtime（同权重/同名）确定性复现选择——secondary 抛错、primary 正常
        // → 按链序回退主模型
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("from-primary"));
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueThrow(networkError("secondary down"));
        AgentSession session = spawnWithModels(primary, secondary,
                canaryProps(Map.of("secondary", 99)), chosenId);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("from-primary");
        assertThat(events).anyMatch(e -> FallbackChain.EVENT_SWITCHED.equals(e.type())
                && "secondary".equals(e.payload().get("from"))
                && "primary".equals(e.payload().get("to")));
        session.close();
    }

    /** canary.selected 事件每会话恰一次（多轮不重复）。 */
    @Test
    void canarySelectedEventEmittedOncePerSession() {
        AgentSession session = spawnWeighted("primary", "secondary", Map.of("secondary", 9));
        List<SessionEvent> events = listen(session);
        session.chat("q1");
        session.chat("q2");
        session.chat("q3");
        assertThat(events.stream()
                .filter(e -> FallbackChain.EVENT_CANARY_SELECTED.equals(e.type()))).hasSize(1);
        session.close();
    }

    /** 默认关：无 canary.selected 事件、全部走主模型（既有行为逐字节不变）。 */
    @Test
    void disabledByDefaultAllPrimaryAndNoCanaryEvents() {
        int primaryCount = 0;
        for (int i = 0; i < 30; i++) {
            ScriptedChatModel primary = new ScriptedChatModel();
            primary.enqueue(new AssistantMessage("from-primary"));
            ScriptedChatModel secondary = new ScriptedChatModel();
            secondary.enqueue(new AssistantMessage("from-secondary"));
            ResilienceProperties props = new ResilienceProperties(
                    true, 1, Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null,
                    Duration.ofSeconds(5), null, circuit(), plainFallback(), null);
            BuzhouStores stores = Buzhou.inMemoryStores();
            AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                    props, "primary", new ResilienceStats(),
                    List.of(new NamedFallbackModel("secondary", secondary))));
            AgentSession session = runtime.spawn("app", "agent", "off-" + i);
            List<SessionEvent> events = listen(session);
            if (session.chat("q").equals("from-primary")) {
                primaryCount++;
            }
            assertThat(events).noneMatch(e -> FallbackChain.EVENT_CANARY_SELECTED.equals(e.type()));
            session.close();
        }
        assertThat(primaryCount).isEqualTo(30);
    }

    // ---- helpers ----

    /** primary/secondary 固定回复 "from-<name>" 的加权金丝雀会话。 */
    private static AgentSession spawnWeighted(String primaryName, String secondaryName,
            Map<String, Integer> weights) {
        return spawnWithModels(new FixedReplyModel("from-" + primaryName),
                new FixedReplyModel("from-" + secondaryName),
                canaryProps(weights), "sess-" + System.nanoTime());
    }

    private static AgentSession spawnWithModels(ChatModel primary, ChatModel secondary,
            ResilienceProperties props, String sessionId) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props, "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary))));
        return runtime.spawn("app", "agent", sessionId);
    }

    /** canary 开 + 权重；maxAttempts=1 无重试 + 快熔断参数（沿用 FallbackChainEndToEndTest 口径）。 */
    private static ResilienceProperties canaryProps(Map<String, Integer> weights) {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, circuit(),
                new ResilienceProperties.Fallback(null, null, Boolean.TRUE, weights), null);
    }

    private static ResilienceProperties.Fallback plainFallback() {
        return new ResilienceProperties.Fallback(null, null, null, null);
    }

    private static ResilienceProperties.Circuit circuit() {
        return new ResilienceProperties.Circuit(null, 10, 2, 0.5, Duration.ofSeconds(60), null);
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    /** 固定回复模型（区分调用落点；金丝雀分流断言用）。 */
    static final class FixedReplyModel implements ChatModel {
        private final String reply;

        FixedReplyModel(String reply) {
            this.reply = reply;
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
