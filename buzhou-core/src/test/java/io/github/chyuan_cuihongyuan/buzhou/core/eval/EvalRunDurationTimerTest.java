package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 111 §B / T404：评估 run 总时长 timer 红队——eval 与 ab 双 timer 各记一次；
  时长非负；per-item durationMs 之外的整跑视角（数据集规模感知的时长告警底座）。
 */
class EvalRunDurationTimerTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> timers = new ConcurrentLinkedQueue<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            timers.add(name + ":" + duration.toNanos());
        }
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void evalRunAndAbRunTimersEachRecordedOnce() {
        CapturingMetrics metrics = new CapturingMetrics();
        BuzhouMetricsHolder.install(metrics);
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("dur", null);
        ds.addItem("dur", "q1", "ok", null, null);
        ChatModel echo = prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage(prompt.getInstructions().getLast().getText()))));
        var runtime = Buzhou.runtime(echo, stores, RuntimeConfig.defaults());

        new EvalRunner(runtime, ds, stores.sessionStateStore())
                .run("dur", BuiltInEvaluators.EXACT);
        new PairwiseEvalRunner(ds, new PairwiseJudge(echo))
                .compare("dur", runtime, runtime, 1);

        assertThat(metrics.timers.stream().map(t -> t.split(":")[0]).toList())
                .contains("buzhou.eval.run.duration", "buzhou.eval.ab-run.duration");
        metrics.timers.stream().filter(t -> t.startsWith("buzhou.eval."))
                .forEach(t -> assertThat(Long.parseLong(t.split(":")[1]))
                        .isGreaterThanOrEqualTo(0));
    }
}
