package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 302 / impl-325：模型重试预算接线端到端（Buzhou.runtime 全链，对齐
 * ResilienceEndToEndTest 模板）——零余额首失败即拒（单次尝试原错上抛）/
 * 有余额重试成功路径不受影响 / 未启用零变化。
 */
class RetryBudgetAdvisorTest {

    @AfterEach
    void clearHolder() {
        RetryBudgetHolder.set(null);
    }

    /** 小退避、关抖动、默认可重试表 {RATE_LIMIT, NETWORK}。 */
    private static ResilienceProperties fastBackoff(int maxAttempts) {
        return new ResilienceProperties(
                true, maxAttempts, Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);
    }

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props));
        return runtime.spawn("app", "budget-agent", "sess-budget-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    @Test
    void shouldDenyRetryAndRethrow_whenBudgetEmpty() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-budget"));
        model.enqueueText("should-never-happen");
        RetryBudgetHolder.set(RetryBudget.of(0.1, 0)); // 零余额：deposit 0.1% 远不够 1 次
        AgentSession session = newRuntime(model, fastBackoff(3));
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi"))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("boom-budget");
        assertThat(model.seenPrompts).as("预算拒绝：仅首尝试，无重试").hasSize(1);
        assertThat(RetryBudgetHolder.current().denied()).isEqualTo(1);
        assertThat(events).anyMatch(e -> "retry-exhausted".equals(e.type())
                && "retry-budget-denied".equals(e.payload().get("reason")));
        session.close();
    }

    @Test
    void shouldRetryWithinBudget_thenSucceed() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("transient-1"));
        model.enqueueText("ok");
        RetryBudgetHolder.set(RetryBudget.of(0.1, 2)); // 预存 2 次额度
        AgentSession session = newRuntime(model, fastBackoff(3));

        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(RetryBudgetHolder.current().withdrawn()).isEqualTo(1);
        session.close();
    }

    @Test
    void shouldKeepExistingBehavior_whenBudgetAbsent() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        model.enqueueText("ok");
        AgentSession session = newRuntime(model, fastBackoff(3));

        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(model.seenPrompts).as("未启用预算：既有重试行为逐位不变").hasSize(3);
        session.close();
    }
}
