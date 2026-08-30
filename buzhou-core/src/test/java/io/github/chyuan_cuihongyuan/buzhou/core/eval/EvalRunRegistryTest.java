package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 77 §B / T308：活跃 run 注册表红队——run 生命周期内计数可见（eval 与 ab 两 kind）；
 * runId 幂等 + Registration close 幂等；gauge 经 metrics 面注册且值随在飞数走；
 * kind 间隔离。
 */
class EvalRunRegistryTest {

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
        EvalRunRegistry.install(null);
    }

    /** 捕获 gauge 注册的假 metrics（name+tag → supplier）。 */
    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String[]> gauges = new ConcurrentLinkedQueue<>();
        final Map<String, Supplier<Number>> suppliers = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
        }

        @Override
        public void gauge(String name, Supplier<Number> value, String... tagKeyValue) {
            gauges.add(new String[]{name, String.join("=", tagKeyValue)});
            suppliers.put(name + "|" + String.join("=", tagKeyValue), value);
        }
    }

    /** 模型侧钩子：call 期间断言在飞计数。 */
    static final class ProbeModel implements ChatModel {
        final Runnable duringCall;
        final ChatModel delegate;

        ProbeModel(Runnable duringCall, ChatModel delegate) {
            this.duringCall = duringCall;
            this.delegate = delegate;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            duringCall.run();
            return delegate.call(prompt);
        }
    }

    private static ChatModel echo(String text) {
        return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void evalRunTrackedDuringLifecycleAndReleasedAfter() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("d", null);
        ds.addItem("d", "q", "ok", null, null);
        int[] seenDuring = {-1};
        ProbeModel probe = new ProbeModel(
                () -> seenDuring[0] = EvalRunRegistry.global().active(EvalRunRegistry.KIND_EVAL),
                echo("ok"));
        AgentRuntime runtime = Buzhou.runtime(probe, stores, RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, ds, stores.sessionStateStore());

        runner.run("d", BuiltInEvaluators.EXACT);

        assertThat(seenDuring[0]).isEqualTo(1); // 模型调用即在飞窗口内
        assertThat(EvalRunRegistry.global().active(EvalRunRegistry.KIND_EVAL)).isZero(); // 出界即释放
    }

    @Test
    void abRunTrackedInOwnKind() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("d", null);
        ds.addItem("d", "q", "x", null, null);
        int[] seenDuring = {-1};
        AgentRuntime runtimeA = Buzhou.runtime(new ProbeModel(
                () -> seenDuring[0] = EvalRunRegistry.global().active(EvalRunRegistry.KIND_AB),
                echo("a")), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(echo("b"), stores, RuntimeConfig.defaults());
        PairwiseJudge judge = new PairwiseJudge(prompt -> new ChatResponse(List.of(
                new Generation(new AssistantMessage("WINNER_A a 更好")))));

        new PairwiseEvalRunner(ds, judge, stores.sessionStateStore())
                .compare("d", runtimeA, runtimeB, 1);

        assertThat(seenDuring[0]).isEqualTo(1);
        assertThat(EvalRunRegistry.global().active(EvalRunRegistry.KIND_AB)).isZero();
        assertThat(EvalRunRegistry.global().active(EvalRunRegistry.KIND_EVAL)).isZero(); // kind 隔离
    }

    @Test
    void beginIsIdempotentPerRunIdAndCloseIsIdempotent() {
        EvalRunRegistry registry = EvalRunRegistry.create();
        EvalRunRegistry.install(registry);

        EvalRunRegistry.Registration first = registry.begin("eval", "run-1");
        EvalRunRegistry.Registration duplicate = registry.begin("eval", "run-1");
        registry.begin("eval", "run-2");

        assertThat(registry.active("eval")).isEqualTo(2); // run-1 幂等只计一次
        first.close();
        duplicate.close(); // 幂等：第二次 close 不误删他人
        assertThat(registry.active("eval")).isEqualTo(1);
    }

    @Test
    void gaugeRegisteredPerKindWithValueTracking() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        EvalRunRegistry registry = EvalRunRegistry.create();
        EvalRunRegistry.install(registry);

        EvalRunRegistry.Registration r1 = registry.begin("eval", "run-1");
        EvalRunRegistry.Registration r2 = registry.begin("ab", "run-2");

        assertThat(metrics.gauges).containsExactlyInAnyOrder(
                new String[]{EvalRunRegistry.GAUGE_NAME, "kind=eval"},
                new String[]{EvalRunRegistry.GAUGE_NAME, "kind=ab"});
        Supplier<Number> evalGauge = metrics.suppliers.get(EvalRunRegistry.GAUGE_NAME + "|kind=eval");
        assertThat(evalGauge.get().intValue()).isEqualTo(1);
        r1.close();
        assertThat(evalGauge.get().intValue()).isZero(); // 出 gauge 窗口即归零
        assertThat(metrics.suppliers.get(EvalRunRegistry.GAUGE_NAME + "|kind=ab").get().intValue())
                .isEqualTo(1);
        r2.close();
    }
}
