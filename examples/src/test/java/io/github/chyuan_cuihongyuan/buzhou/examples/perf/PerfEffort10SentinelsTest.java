package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #10 新能力 perf 哨兵（spec 51 §C / T183 / impl-152）：TTFT 打点开销、
 * rateTurn 写入开销、候选限流闸开销、shadow 提交开销——10 倍宽幅粗粒度回归哨兵，
 * nightly 以 -Dgroups=perf 激活。
 */
@Tag("perf")
class PerfEffort10SentinelsTest {

    /** TTFT 打点：流式消费带打点路径 P95 上限（打点=两次 nanoTime+一次 map 写，首轮实测 <2ms 含流开销）。 */
    private static final double TTFT_OVERHEAD_P95_MAX_MILLIS = 50;

    /** rateTurn 单次反馈（state put + 事件）P95 上限（首轮实测 <1ms）。 */
    private static final double RATE_TURN_P95_MAX_MILLIS = 20;

    /** 候选限流闸：RPM 预检+扣减单次 P95 上限（两次 map 查找，首轮实测 <0.1ms）。 */
    private static final double CANDIDATE_GATE_P95_MAX_MILLIS = 5;

    /** shadow 提交（即发即忘）P95 上限（首轮实测 <1ms）。 */
    private static final double SHADOW_SUBMIT_P95_MAX_MILLIS = 20;

    private static final int SAMPLES = 30;

    /** ①TTFT 打点开销：带 TTFT/TPOT 打点的流式端到端消费（对比量级：无打点流式同量级）。 */
    @Test
    void ttftMarkingOverheadSentinel() {
        StreamingModel model = new StreamingModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime rt = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan.buzhou.observability
                .ObservabilityModule.configureSync(stores,
                        io.github.chyuan_cuihongyuan.buzhou.observability.ObservabilityConfig
                                .testDefaults(),
                        "perf-ttft"));
        double p95 = p95Of(() -> {
            AgentSession session = rt.spawn("app", "ag", "perf-ttft-" + System.nanoTime());
            StringBuilder sb = new StringBuilder();
            session.stream("q").doOnNext(r -> sb.append("x")).blockLast();
            session.close();
            return sb.length() > 0;
        });
        assertThat(p95).isLessThan(TTFT_OVERHEAD_P95_MAX_MILLIS);
    }

    /** ②rateTurn 写入：单次反馈（校验 + state put + 事件分发）。 */
    @Test
    void rateTurnWriteOverheadSentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("ok"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "ag", "perf-rate");
        session.chat("q");
        double p95 = p95Of(() -> {
            session.rateTurn(1, "numeric", "5", null, null);
            return true;
        });
        assertThat(p95).isLessThan(RATE_TURN_P95_MAX_MILLIS);
        session.close();
    }

    /** ③候选限流闸：acquireOrThrow 预检+扣减（RPM 桶大容量防误拒）。 */
    @Test
    void candidateRateGateOverheadSentinel() {
        io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter limiter =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter(
                        1_000_000, null, Duration.ofMillis(1),
                        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.FAIL_FAST,
                        null);
        double p95 = p95Of(() -> {
            limiter.acquireOrThrow("perf-gate");
            return true;
        });
        assertThat(p95).isLessThan(CANDIDATE_GATE_P95_MAX_MILLIS);
    }

    /** ④shadow 提交：主路径成功后 submit（护栏检查+虚拟线程启动，即发即忘）。 */
    @Test
    void shadowSubmitOverheadSentinel() throws Exception {
        ShadowTrafficController controller = new ShadowTrafficController(
                List.of(new NamedFallbackModel("shadow", new InstantModel())),
                new ResilienceProperties.Shadow(Boolean.TRUE, null, null, null));
        Prompt prompt = new Prompt("q");
        double p95 = p95Of(() -> {
            controller.submit(prompt, "primary", 5, event -> {
            });
            return true;
        });
        // 提交即返回（后台线程消化）；等待后台任务清空避免跨测试泄漏
        Thread.sleep(200);
        assertThat(p95).isLessThan(SHADOW_SUBMIT_P95_MAX_MILLIS);
    }

    // ---- helpers ----

    private static double p95Of(Supplier<Boolean> action) {
        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            assertThat(action.get()).isTrue();
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        java.util.Arrays.sort(samples);
        return samples[(int) (SAMPLES * 0.95)];
    }

    static class ScriptedChatModel implements ChatModel {
        private final java.util.Queue<ChatResponse> script = new java.util.concurrent.ConcurrentLinkedQueue<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse next = script.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("d"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }

    /** 三块流式模型（带 usage，触发 TTFT/TPOT 完整打点路径）。 */
    static class StreamingModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("c"))),
                    ChatResponseMetadata.builder().usage(new DefaultUsage(10, 3)).build());
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            ChatResponse r1 = new ChatResponse(List.of(new Generation(new AssistantMessage("a"))));
            ChatResponse r2 = new ChatResponse(List.of(new Generation(new AssistantMessage("b"))));
            ChatResponse r3 = call(prompt);
            return reactor.core.publisher.Flux.just(r1, r2, r3);
        }
    }

    /** 秒回模型（shadow 消化用）。 */
    static class InstantModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("s"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
