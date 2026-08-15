package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

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
import io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹 F（spec 51 §A / T181 / impl-150）：effort #10 新机制——
 * 反馈捕获（G22）、金丝雀稳定（G23）、shadow 隔离（G24）。
 */
class GoldenTrajectoryEffort10Test {

    // ---- G22 反馈捕获：chat → rateTurn → 事件 + 落键轨迹 ----

    @Test
    void g22FeedbackCaptureTrajectory() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("回复一号"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        AgentSession session = runtime.spawn("app", "ag", "g22");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        session.chat("问一句");
        session.rateTurn(1, "boolean", "false", "不好", null);

        // 事件序列：turn.feedback 载荷完整（turnSeq/type/value/comment/source）
        assertThat(events).anyMatch(e -> "turn.feedback".equals(e.type())
                && Integer.valueOf(1).equals(e.payload().get("turnSeq"))
                && "boolean".equals(e.payload().get("type"))
                && "false".equals(e.payload().get("value"))
                && "user".equals(e.payload().get("source")));
        // 落键轨迹终点：state store 可枚举
        assertThat(stores.sessionStateStore().scanByPrefix("g22", "buzhou.feedback."))
                .hasSize(1);
        session.close();
    }

    // ---- G23 金丝雀稳定：同会话多轮粘住首选 ----

    @Test
    void g23CanarySticksPerSession() {
        int secondarySessions = 0;
        for (int i = 0; i < 30; i++) {
            ScriptedChatModel primary = new ScriptedChatModel();
            primary.enqueue(new AssistantMessage("from-primary"));
            primary.enqueue(new AssistantMessage("from-primary"));
            ScriptedChatModel secondary = new ScriptedChatModel();
            secondary.enqueue(new AssistantMessage("from-secondary"));
            secondary.enqueue(new AssistantMessage("from-secondary"));
            BuzhouStores stores = Buzhou.inMemoryStores();
            AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                    canaryProps(Map.of("secondary", 9)), "primary", new ResilienceStats(),
                    List.of(new NamedFallbackModel("secondary", secondary))));
            AgentSession session = runtime.spawn("app", "ag", "g23-" + i);
            List<SessionEvent> events = new CopyOnWriteArrayList<>();
            session.addEventListener(events::add);

            String first = session.chat("q1");
            String second = session.chat("q2");
            // 粘性轨迹：同会话两轮同源（漂移 = 轨迹断言失败）
            assertThat(second).isEqualTo(first);
            // canary.selected 恰一次（无论落主或备）
            assertThat(events.stream()
                    .filter(e -> FallbackChain.EVENT_CANARY_SELECTED.equals(e.type()))).hasSize(1);
            if ("from-secondary".equals(first)) {
                secondarySessions++;
            }
            session.close();
        }
        // 分流面：宽幅断言（9:1 期望，稳定哈希确定性但算法不锁死）
        assertThat(secondarySessions).isBetween(15, 30);
    }

    // ---- G24 shadow 隔离：探测零回注 ----

    @Test
    void g24ShadowIsolationTrajectory() throws Exception {
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("from-primary"));
        ScriptedChatModel shadow = new ScriptedChatModel();
        shadow.enqueue(new AssistantMessage("SHADOW-ONLY"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                shadowProps(), "primary", new ResilienceStats(), null,
                List.of(new NamedFallbackModel("shadow-model", shadow))));
        AgentSession session = runtime.spawn("app", "ag", "g24");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        String reply = session.chat("hi");
        // 隔离轨迹：用户回复来自主模型（shadow 输出零回注）
        assertThat(reply).isEqualTo("from-primary");

        long deadline = System.currentTimeMillis() + 5000;
        while (events.stream().noneMatch(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type()))
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        // 对照事件到达；且无降级切换（探测不产生 fallback 语义）
        assertThat(events).anyMatch(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type())
                && "primary".equals(e.payload().get("primary"))
                && "shadow-model".equals(e.payload().get("shadow")));
        assertThat(events).noneMatch(e -> FallbackChain.EVENT_SWITCHED.equals(e.type()));
        session.close();
    }

    // ---- helpers ----

    private static ResilienceProperties canaryProps(Map<String, Integer> weights) {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null,
                new ResilienceProperties.Fallback(null, null, Boolean.TRUE, weights),
                null, null);
    }

    private static ResilienceProperties shadowProps() {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null,
                new ResilienceProperties.Shadow(Boolean.TRUE, null, null, null));
    }
}
