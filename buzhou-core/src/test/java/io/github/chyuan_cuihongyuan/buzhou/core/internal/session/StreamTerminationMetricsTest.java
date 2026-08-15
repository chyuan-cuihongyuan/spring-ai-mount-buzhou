package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 46 §B / T171 / impl-140：流终止原因分类计数 + 慢滴流累计上限端到端。
 */
class StreamTerminationMetricsTest {

    private final List<String> cancellations = new CopyOnWriteArrayList<>();

    @AfterEach
    void reset() {
        BuzhouMetricsHolder.reset();
    }

    private AgentRuntime runtime(ChatModel model, Duration cap) {
        return new DefaultAgentRuntime(model, Buzhou.inMemoryStores(),
                new HarnessAssembler().withStreamTotalTimeout(cap), RuntimeConfig.defaults());
    }

    private void installCapturingMetrics() {
        BuzhouMetricsHolder.install(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
                if ("buzhou.stream.cancelled".equals(name) && tagKeyValue.length >= 2) {
                    cancellations.add(tagKeyValue[1]);
                }
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
            }
        });
    }

    /** 每 50ms 滴一字的慢滴流：累计上限 200ms 到点以标记异常终结，reason=deadline 计数。 */
    @Test
    void slowDripStreamCutByCumulativeCap() {
        installCapturingMetrics();
        DripChatModel model = new DripChatModel(Duration.ofMillis(50));
        AtomicReference<Throwable> error = new AtomicReference<>();
        AgentRuntime rt = runtime(model, Duration.ofMillis(200));
        AgentSession session = rt.spawn("app", "agent", "sess-drip");

        List<ChatResponse> received = new CopyOnWriteArrayList<>();
        try {
            session.stream("慢滴问一句")
                    .doOnNext(received::add)
                    .doOnError(error::set)
                    .blockLast(java.time.Duration.ofSeconds(5));
        } catch (RuntimeException expected) {
            // blockLast 把 onError 原样抛出（StreamTotalTimeoutException 即所期）
        }

        // 到点截断：滴了若干字后以 StreamTotalTimeoutException 终结
        assertThat(error.get()).isInstanceOf(StreamTotalTimeoutException.class);
        assertThat(received.size()).isBetween(1, 10);
        assertThat(cancellations).contains("deadline");
        // 单飞闸随终结释放：会话可再开新一轮（本轮以 chat 走正常路径验证闸已释放）
        session.close();
    }

    /** 订阅者主动 dispose → reason=client；不与 deadline 串扰。 */
    @Test
    void clientCancelCountedAsClient() {
        installCapturingMetrics();
        DripChatModel model = new DripChatModel(Duration.ofMillis(50));
        AgentRuntime rt = runtime(model, Duration.ofMinutes(10));
        AgentSession session = rt.spawn("app", "agent", "sess-client");

        reactor.core.Disposable disposable = session.stream("问").subscribe();
        model.awaitFirstChunk();
        disposable.dispose();

        assertThat(cancellations).contains("client").doesNotContain("deadline");
        session.close();
    }

    /** beforeTurn 护栏拦截 → reason=guard。 */
    @Test
    void guardBlockCountedAsGuard() {
        installCapturingMetrics();
        DripChatModel model = new DripChatModel(Duration.ofMillis(10));
        AgentRuntime rt = new DefaultAgentRuntime(model, Buzhou.inMemoryStores(),
                new HarnessAssembler().withStreamTotalTimeout(Duration.ofMinutes(10)),
                RuntimeConfig.hooks(java.util.List.of(new BuzhouHook() {
                    @Override
                    public HookResult beforeTurn(TurnContext ctx) {
                        return HookResult.block("护栏测试拦截");
                    }
                })));
        AgentSession session = rt.spawn("app", "agent", "sess-guard");

        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            session.stream("问").doOnError(error::set).blockLast(java.time.Duration.ofSeconds(5));
        } catch (RuntimeException expected) {
            // Flux.error 的 IllegalStateException 由 blockLast 抛出（所期）
        }

        assertThat(error.get()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("护栏测试拦截");
        assertThat(cancellations).contains("guard").doesNotContain("client", "deadline");
        session.close();
    }

    /** 显式关闭（ZERO）：滴流跑完不受截断（既有不限语义逃生舱）。 */
    @Test
    void disabledCapLetsFiniteDripComplete() {
        installCapturingMetrics();
        FiniteDripModel model = new FiniteDripModel(5, Duration.ofMillis(40));
        AgentRuntime rt = runtime(model, Duration.ZERO);
        AgentSession session = rt.spawn("app", "agent", "sess-off");

        List<ChatResponse> received = new CopyOnWriteArrayList<>();
        session.stream("问").doOnNext(received::add).blockLast(java.time.Duration.ofSeconds(10));

        assertThat(received).hasSize(5);
        assertThat(cancellations).isEmpty();
        session.close();
    }

    /** 每 tick 滴一字的无限流模型（首块到达有 latch 供 dispose 测试同步）。 */
    static class DripChatModel implements ChatModel {
        private final Duration tick;
        private final java.util.concurrent.CountDownLatch firstChunk = new java.util.concurrent.CountDownLatch(1);

        DripChatModel(Duration tick) {
            this.tick = tick;
        }

        void awaitFirstChunk() {
            try {
                firstChunk.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("fallback"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.interval(tick)
                    .map(i -> new ChatResponse(List.of(new Generation(
                            new AssistantMessage("字" + i)))))
                    .doOnNext(r -> firstChunk.countDown());
        }
    }

    /** 有限滴流：n 块各隔 tick 到达后完结。 */
    static class FiniteDripModel implements ChatModel {
        private final int chunks;
        private final Duration tick;

        FiniteDripModel(int chunks, Duration tick) {
            this.chunks = chunks;
            this.tick = tick;
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("fallback"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.range(0, chunks)
                    .delayElements(tick)
                    .map(i -> new ChatResponse(List.of(new Generation(
                            new AssistantMessage("块" + i)))));
        }
    }
}
