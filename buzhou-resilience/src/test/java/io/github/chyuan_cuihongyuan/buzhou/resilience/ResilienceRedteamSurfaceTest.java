package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 红队对抗四批（spec 51 §B / T182 / impl-151）：配额绕过（跳级唯一性——每轮每候选至多
 * 一次触达）与金丝雀漂移（多轮不漂移）的确定性对抗断言；反馈伪造与 shadow 泄漏面在
 * core 测试（TurnFeedbackTest）与黄金轨迹（G24）承载，观察档见 redteam/README.md。
 */
class ResilienceRedteamSurfaceTest {

    /**
     * 配额绕过对抗：候选链全败一轮——每个候选恰被触达一次（限流跳级/熔断跳级/失败跳级
     * 均不得造成同候选重复触达 = 重试风暴绕过配额的面被钉死）。
     */
    @Test
    void quotaSkipUniquenessEachCandidateTouchedAtMostOncePerTurn() {
        CountingModel secondary = new CountingModel("sec");
        CountingModel tertiary = new CountingModel("ter");
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueueThrow(networkError("primary down"));
        primary.enqueueThrow(networkError("primary down"));

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                noLimitProps(), "primary", new ResilienceStats(),
                List.of(new NamedFallbackModel("secondary", secondary),
                        new NamedFallbackModel("tertiary", tertiary))));
        AgentSession session = runtime.spawn("app", "ag", "rt-quota");
        try {
            session.chat("q1");
        } catch (RuntimeException expected) {
            // 全链失败上抛（所期）
        }

        // 轮一：主失败 → sec 失败 → ter 失败 → 耗尽：每候选恰一次
        assertThat(secondary.calls()).isEqualTo(1);
        assertThat(tertiary.calls()).isEqualTo(1);

        try {
            session.chat("q2");
        } catch (RuntimeException expected) {
            // 同口径全败（所期）
        }
        // 轮二：每轮每候选至多一次（两轮共两次）
        assertThat(secondary.calls()).isEqualTo(2);
        assertThat(tertiary.calls()).isEqualTo(2);
        session.close();
    }

    /** 金丝雀漂移对抗：连续多轮（含失败回退轮）后，后续成功轮仍粘住同一目标。 */
    @Test
    void canaryNoDriftAcrossFailureTurns() {
        // 权重 1:99 → 几乎全会话选 secondary；选中后 secondary 先挂一轮回退主、再成功粘回
        int stuckSessions = 0;
        int probed = 0;
        for (int i = 0; i < 20; i++) {
            ScriptedChatModel primary = new ScriptedChatModel();
            primary.enqueueThrow(networkError("p-fail"));
            primary.enqueue(new AssistantMessage("from-primary"));
            primary.enqueue(new AssistantMessage("from-primary"));
            ScriptedChatModel secondary = new ScriptedChatModel();
            secondary.enqueueThrow(networkError("s-fail"));
            secondary.enqueue(new AssistantMessage("from-secondary"));
            secondary.enqueue(new AssistantMessage("from-secondary"));
            BuzhouStores stores = Buzhou.inMemoryStores();
            AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                    canaryProps(), "primary", new ResilienceStats(),
                    List.of(new NamedFallbackModel("secondary", secondary))));
            AgentSession session = runtime.spawn("app", "ag", "rt-drift-" + i);

            // 轮一：目标失败 → 回退主（无第二次回退源）
            try {
                session.chat("q1");
            } catch (RuntimeException expected) {
                // 目标与主全败：上抛（部分会话可能选中主——按实际选择分流）
            }
            // 轮二/三：恢复后的轮次——粘性检查（两轮同源即不漂移）
            String a = session.chat("q2");
            String b = session.chat("q3");
            assertThat(b).as("会话 %d 多轮漂移", i).isEqualTo(a);
            if ("from-secondary".equals(a)) {
                stuckSessions++;
            }
            probed++;
            session.close();
        }
        assertThat(probed).isEqualTo(20);
        assertThat(stuckSessions).as("权重 99 的备模型应有大量粘性会话").isGreaterThan(10);
    }

    // ---- helpers ----

    private static ResilienceProperties noLimitProps() {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null, null);
    }

    private static ResilienceProperties canaryProps() {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null,
                new ResilienceProperties.Fallback(null, null, Boolean.TRUE,
                        java.util.Map.of("secondary", 99)),
                null, null);
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    /** 计数模型：恒抛网络错（失败候选触达计数用）。 */
    static final class CountingModel extends ScriptedChatModel {
        private final String name;
        private final AtomicInteger calls = new AtomicInteger();

        CountingModel(String name) {
            this.name = name;
        }

        int calls() {
            return calls.get();
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            calls.incrementAndGet();
            throw new UncheckedIOException(new IOException(name + " down"));
        }
    }
}
