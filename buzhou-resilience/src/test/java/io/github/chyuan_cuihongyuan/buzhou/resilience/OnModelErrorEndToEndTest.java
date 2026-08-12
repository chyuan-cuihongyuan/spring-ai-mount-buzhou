package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * onModelError 切面端到端（spec「onModelError 切面」/ 04 号票）：
 * 韧性层终态失败（重试耗尽 / 超时）→ Hook 可吞错回填兜底响应，或放行让异常按底座原语义抛出。
 */
class OnModelErrorEndToEndTest {

    /** 重试耗尽为触发源：onModelError Hook 吞错回填兜底响应，用户得到受控回复而非裸异常。 */
    @Test
    void retryExhaustionTriggersFallback() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = new ResilienceProperties(true, 2,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);

        AgentSession session = newRuntime(model, props, List.of(fallbackHook("模型暂不可用，请稍后重试")));
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("模型暂不可用，请稍后重试");
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_RETRY_EXHAUSTED.equals(e.type()));
        session.close();
    }

    /** 未接 onModelError Hook（默认放行）：终态失败异常按底座原语义抛出。 */
    @Test
    void noHookPassthroughRethrows() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = new ResilienceProperties(true, 2,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);

        AgentSession session = newRuntime(model, props, List.of());
        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(UncheckedIOException.class);
        session.close();
    }

    /** 超时为触发源：deadline 超时 → onModelError Hook 兜底（证明三类终态源都汇到统一切面）。 */
    @Test
    void timeoutTriggersFallback() throws Exception {
        ResilienceEndToEndTest.BlockingChatModel model = ResilienceEndToEndTest.BlockingChatModel.neverReturns();
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofMillis(50), null);

        AgentSession session = newRuntime(model, props, List.of(fallbackHook("超时兜底")));
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("超时兜底");
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_TIMEOUT_FIRED.equals(e.type()));
        session.close();
    }

    /** onModelError 切面里 ctx.error() 暴露终态失败原因（Hook 可据此做归因留痕）。 */
    @Test
    void errorIsExposedToHook() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom"));
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);

        java.util.concurrent.atomic.AtomicReference<Throwable> seen = new java.util.concurrent.atomic.AtomicReference<>();
        AgentSession session = newRuntime(model, props, List.of(new io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook() {
            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult onModelError(
                    io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext ctx) {
                seen.set(ctx.error());
                return io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.CONTINUE; // 放行
            }
        }));
        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(UncheckedIOException.class);
        assertThat(seen.get()).isNotNull();
        session.close();
    }

    /** Block(reason) 形态的兜底：onModelError 返回 Block → 回填文本响应。 */
    @Test
    void blockReasonAsFallback() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom"));
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null, null);

        AgentSession session = newRuntime(model, props, List.of(new io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook() {
            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult onModelError(
                    io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext ctx) {
                return io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.block("已记录，请联系管理员");
            }
        }));
        assertThat(session.chat("hi")).isEqualTo("已记录，请联系管理员");
        session.close();
    }

    // ---- helpers ----

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props,
                                           List<io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook> hooks) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        RuntimeConfig config = RuntimeConfig.merge(
                ResilienceModule.configure(props),
                RuntimeConfig.hooks(hooks));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);
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

    /** 吞错兜底 Hook：onModelError 回填一个受控 ChatClientResponse。 */
    private static io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook fallbackHook(String fallbackText) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook() {
            @Override
            public io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult onModelError(
                    io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext ctx) {
                ChatResponse chat = new ChatResponse(List.of(new Generation(new AssistantMessage(fallbackText))));
                ChatClientResponse fallback = ChatClientResponse.builder().chatResponse(chat).build();
                return io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.replace(fallback);
            }
        };
    }
}
