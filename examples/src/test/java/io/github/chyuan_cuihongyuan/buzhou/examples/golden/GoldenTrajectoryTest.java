package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.budget.TokenBudgetHook;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.EventSequenceAssert;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹回归集（spec 32 / T111 / impl-86）：六大机制各一条「输入脚本 → 事件序列断言」
 * 黄金轨迹——机制行为回归的系统性防线（红队测对抗输入、perf 测时延，本集测行为不变性）。
 * 全部进 ci.yml 常规跑（回归性质，快且该挡）。
 */
class GoldenTrajectoryTest {

    private static final Duration FAST = Duration.ofMillis(1);
    private static final Duration FAST_MAX = Duration.ofMillis(20);

    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(100, 50)).build());
        }
    }

    /** G1 降级链：主模型 NETWORK 耗尽 → fallback.switched → 备模型回复（其后不再有切换）。 */
    @Test
    void g1FallbackSwitchTrajectory() {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError());
        primary.enqueueThrow(networkError());
        ScriptedChatModel secondary = new ScriptedChatModel();
        secondary.enqueueText("from-backup");
        ResilienceProperties props = new ResilienceProperties(true, 2, FAST, FAST_MAX,
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null);
        AgentRuntime runtime = Buzhou.runtime(primary, Buzhou.inMemoryStores(),
                ResilienceModule.configure(props, "primary", new ResilienceStats(),
                        List.of(new NamedFallbackModel("backup", secondary))));
        AgentSession session = runtime.spawn("app", "agent", "g1");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        assertThat(session.chat("hi")).isEqualTo("from-backup");

        events.assertContainsInOrder("retry-exhausted", "fallback.switched")
                .assertNeverAfter("fallback.switched", "fallback.switched");
        session.close();
    }

    /** G2 预算闸：累计触顶 → budget.token-hard-stop → 其后模型零调用（无新的累计事件）。 */
    @Test
    void g2BudgetHardStopTrajectory() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouTokenBudgetProperties budget = new BuzhouTokenBudgetProperties(
                null, null, 200L, null, null);
        AgentRuntime runtime = Buzhou.runtime(model, stores, new RuntimeConfig(
                List.of(new TokenBudgetHook(budget, "golden", stores.observabilityStore())),
                Set.of(), Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "agent", "g2");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).isEqualTo("r2");
        assertThat(session.chat("q3")).contains("预算上限");

        events.assertContainsInOrder("budget.tokens-accumulated", "budget.token-hard-stop")
                .assertNeverAfter("budget.token-hard-stop", "budget.tokens-accumulated");
        session.close();
    }

    /** G3 日配额：turns-per-day=1 第二轮被拦 → quota.exceeded 恰一次，模型零新调用。 */
    @Test
    void g3QuotaExceededTrajectory() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        ResilienceProperties.SessionQuota quota =
                new ResilienceProperties.SessionQuota(1, null, null);
        AgentRuntime runtime = Buzhou.runtime(model, stores, new RuntimeConfig(
                List.of(new SessionQuotaHook(quota, new ResilienceStats())),
                Set.of(), Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "agent", "g3");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).contains("配额上限");

        events.assertCount("quota.exceeded", 1)
                .assertPayload("quota.exceeded", p -> p.containsKey("sessionId"));
        session.close();
    }

    /** G4 熔断恢复：失败率跳闸 → 冷却过半开探测成功 → 回 CLOSED（state-changed 三段轨迹）。 */
    @Test
    void g4CircuitRecoveryTrajectory() throws InterruptedException {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError());
        model.enqueueThrow(networkError());
        model.enqueueThrow(networkError());
        model.enqueueText("recovered");
        ResilienceProperties props = new ResilienceProperties(true, 2, FAST, FAST_MAX,
                2.0, 0.0, null, Duration.ofSeconds(5), null,
                new ResilienceProperties.Circuit(null, 4, 1, 0.5, Duration.ofMillis(80),
                        null, null), // min-calls=1：单次逻辑调用耗尽即跳闸（样本按逻辑调用计）
                null, null);
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(props, "g4-model", new ResilienceStats(), List.of()));
        AgentSession session = runtime.spawn("app", "agent", "g4");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        try {
            session.chat("q1"); // 2 次失败样本 → 跳闸 OPEN（终态失败上抛或降级反馈，轨迹不变）
        } catch (RuntimeException expectedOnExhaustion) {
            // 重试耗尽上抛路径——事件轨迹不受影响
        }
        Thread.sleep(120); // 过冷却
        assertThat(session.chat("q2")).isEqualTo("recovered"); // 半开探测成功 → CLOSED

        events.assertContainsInOrder("circuit.state-changed", "circuit.state-changed")
                .assertPayload("circuit.state-changed",
                        p -> "OPEN".equals(p.get("to")) || "CLOSED".equals(p.get("to")));
        assertThat(events.types().stream().filter("circuit.state-changed"::equals).count())
                .isGreaterThanOrEqualTo(2);
        session.close();
    }

    /** G5 REASK：首轮不合规 → structured.reask → 第二轮合规实体返回。 */
    @Test
    void g5StructuredReaskTrajectory() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("我不想要 JSON。");
        model.enqueueText("{\"answer\":42}");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "g5");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        Answer a = session.chatForEntity("the answer?", Answer.class);

        assertThat(a.answer()).isEqualTo(42);
        events.assertCount("structured.reask", 1);
        session.close();
    }

    /** G6 fork：session.forked 恰一次；分支继承历史独立演化。 */
    @Test
    void g6ForkTrajectory() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        model.enqueueText("b1");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults());
        AgentSession source = runtime.spawn("app", "agent", "g6-src");
        EventSequenceAssert events = EventSequenceAssert.attachGlobal(runtime); // forked 发往分支/全局通道
        source.chat("q1");

        AgentSession branch = runtime.fork("g6-src", "app", "agent", "g6-branch");
        assertThat(branch.chat("q2")).isEqualTo("b1");

        events.assertCount("session.forked", 1)
                .assertPayload("session.forked", p -> "g6-src".equals(p.get("sourceSessionId")));
        source.close();
        branch.close();
    }

    // ---- helpers ----

    record Answer(long answer) {
    }

    private static UncheckedIOException networkError() {
        return new UncheckedIOException(new java.io.IOException("connection reset"));
    }
}
