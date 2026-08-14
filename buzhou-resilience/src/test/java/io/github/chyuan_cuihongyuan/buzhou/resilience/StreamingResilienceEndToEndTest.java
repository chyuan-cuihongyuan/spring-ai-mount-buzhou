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
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 流式韧性端到端（spec「Streaming」/ 05 号票）：deadline + 分类 + onModelError；
 * 明确 M1 边界——不对已发 token 中途重试（单次失败即传播）。
 */
class StreamingResilienceEndToEndTest {

    /** 流式慢模型（永不发首 token）+ 短 deadline：timeout-fired 触发、流终止。 */
    @Test
    void streamTimeoutFiresAndTerminates() {
        StreamScriptedModel model = new StreamScriptedModel(Flux.never());
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofMillis(50), null);

        AgentSession session = newRuntime(model, props, List.of());
        List<SessionEvent> events = listen(session);

        long start = System.nanoTime();
        assertThatThrownBy(() -> session.stream("hi").blockLast()).isNotNull();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(2000);
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_TIMEOUT_FIRED.equals(e.type()));
        session.close();
    }

    /** 流式错误被分类，onModelError Hook 可兜底（用户拿到兜底回复）。 */
    @Test
    void streamErrorClassifiedWithFallback() {
        StreamScriptedModel model = new StreamScriptedModel(Flux.error(networkError("boom")));
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofSeconds(5), null);

        AgentSession session = newRuntime(model, props, List.of(fallbackHook("流式兜底")));
        List<SessionEvent> events = listen(session);

        ChatResponse resp = session.stream("hi").blockLast();
        assertThat(resp.getResult().getOutput().getText()).isEqualTo("流式兜底");
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_ERROR_CLASSIFIED.equals(e.type()));
        session.close();
    }

    /** 流式失败不发生中途重试：单次失败即传播，无 retry-attempted 事件、模型 stream 只被调一次。 */
    @Test
    void streamFailureDoesNotRetryMidStream() {
        StreamScriptedModel model = new StreamScriptedModel(Flux.error(networkError("boom")));
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofSeconds(5), null);

        AgentSession session = newRuntime(model, props, List.of());
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.stream("hi").blockLast()).isNotNull();
        assertThat(model.seenPrompts).hasSize(1); // 不重试
        assertThat(events).noneMatch(e -> e.type().startsWith("retry-"));
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

    /** 可控流式 ChatModel：stream 返回注入的 Flux（成功 / 错误信号 / 永不发）。 */
    static class StreamScriptedModel extends ScriptedChatModel {
        private final Flux<ChatResponse> streamFlux;

        StreamScriptedModel(Flux<ChatResponse> streamFlux) {
            this.streamFlux = streamFlux;
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            seenPrompts.add(prompt);
            return streamFlux;
        }
    }
}
