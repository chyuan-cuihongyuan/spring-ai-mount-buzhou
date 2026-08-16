package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheKeys;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #12 缓存面 perf 哨兵（spec 53 / T209 / impl-174）：缓存命中路径、键计算、
 * 流式命中重放——10 倍宽幅粗粒度回归哨兵，nightly 以 -Dgroups=perf 激活。
 */
@Tag("perf")
class PerfEffort12SentinelsTest {

    /** call 命中路径（advisor 查键+重放包装）P95 上限（首轮实测 <1ms）。 */
    private static final double CALL_HIT_P95_MAX_MILLIS = 15;

    /** 键计算（10 条消息 sha256 规范序列化）P95 上限（首轮实测 <0.5ms）。 */
    private static final double KEY_P95_MAX_MILLIS = 10;

    /** 流式命中重放（Flux.just 订阅消费）P95 上限（首轮实测 <2ms）。 */
    private static final double STREAM_HIT_P95_MAX_MILLIS = 20;

    private static final int SAMPLES = 30;

    private static ResilienceProperties cacheProps() {
        return new ResilienceProperties(null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                new ResilienceProperties.ResponseCache(Boolean.TRUE, 64, Duration.ofHours(1)));
    }

    /** ①call 命中路径：预填缓存后同问（应远低于直调开销）。 */
    @Test
    void callCacheHitOverheadSentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("cached");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(cacheProps(), "perf-cache", new ResilienceStats(), null, null));
        try (var warm = runtime.spawn("app", "ag", "perf-warm")) {
            assertThat(warm.chat("固定问题")).isEqualTo("cached");
        }
        double p95 = p95Of(() -> {
            try (var s = runtime.spawn("app", "ag", "perf-hit-" + System.nanoTime())) {
                return "cached".equals(s.chat("固定问题"));
            }
        });
        assertThat(p95).isLessThan(CALL_HIT_P95_MAX_MILLIS);
    }

    /** ②键计算：10 条消息 prompt 的规范化哈希。 */
    @Test
    void keyComputationSentinel() {
        List<UserMessage> messages = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(new UserMessage("消息 " + i + "：包含一定长度的内容用于序列化开销测量"));
        }
        Prompt prompt = new Prompt(new java.util.ArrayList<org.springframework.ai.chat.messages.Message>(messages));
        double p95 = p95Of(() -> ResponseCacheKeys.keyOf("m", prompt) != null);
        assertThat(p95).isLessThan(KEY_P95_MAX_MILLIS);
    }

    /** ③流式命中重放：预填后同问流式订阅。 */
    @Test
    void streamCacheHitReplaySentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("stream-cached");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(cacheProps(), "perf-sc", new ResilienceStats(), null, null));
        StringBuilder warm = new StringBuilder();
        try (var w = runtime.spawn("app", "ag", "perf-sc-warm")) {
            w.stream("流式固定问题").doOnNext(r -> {
                if (r.getResult() != null && r.getResult().getOutput() != null) {
                    warm.append(String.valueOf(r.getResult().getOutput().getText()));
                }
            }).blockLast();
        }
        assertThat(warm.length()).isGreaterThan(0);
        double p95 = p95Of(() -> {
            StringBuilder sb = new StringBuilder();
            try (var s = runtime.spawn("app", "ag", "perf-sc-hit-" + System.nanoTime())) {
                s.stream("流式固定问题").doOnNext(r -> {
                    if (r.getResult() != null && r.getResult().getOutput() != null) {
                        sb.append(String.valueOf(r.getResult().getOutput().getText()));
                    }
                }).blockLast();
            }
            return sb.length() > 0;
        });
        assertThat(p95).isLessThan(STREAM_HIT_P95_MAX_MILLIS);
    }

    private static double p95Of(Supplier<Boolean> op) {
        long[] samples = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            assertThat(op.get()).isTrue();
            samples[i] = (System.nanoTime() - start) / 1_000_000;
        }
        java.util.Arrays.sort(samples);
        return samples[(int) Math.floor(SAMPLES * 0.95) - 1];
    }
}
