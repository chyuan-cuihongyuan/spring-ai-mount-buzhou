package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 52 §F / T195 / impl-161：评估事件外发（emitEvent 公共面 + eval.run.completed）。
 */
class EvalEventsTest {

    @Test
    void runCompletionEmitsEventToGlobalListeners() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = (DefaultAgentRuntime) Buzhou.runtime(
                model, stores, RuntimeConfig.defaults());
        Queue<SessionEvent> received = new ConcurrentLinkedQueue<>();
        runtime.addGlobalEventListener(new SessionEventListener() {
            @Override
            public void onEvent(SessionEvent event) {
                received.add(event);
            }
        });

        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("d", null);
        datasetStore.addItem("d", "q", "a1", null, null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        EvalRunResult result = runner.run("d", BuiltInEvaluators.EXACT);

        SessionEvent evalEvent = received.stream()
                .filter(e -> "eval.run.completed".equals(e.type()))
                .findFirst().orElseThrow();
        Map<String, Object> payload = evalEvent.payload();
        assertThat(payload.get("runId")).isEqualTo(result.runId());
        assertThat(payload.get("datasetName")).isEqualTo("d");
        assertThat(payload.get("total")).isEqualTo(1);
        assertThat(payload.get("passed")).isEqualTo(1);
        assertThat(payload.get("passRate")).isEqualTo(1.0);
        assertThat(payload).containsKey("durationMs");
    }

    @Test
    void emptyRunEmitsNothingAndCustomEventGoesThroughChannel() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = (DefaultAgentRuntime) Buzhou.runtime(
                model, stores, RuntimeConfig.defaults());
        Queue<SessionEvent> received = new ConcurrentLinkedQueue<>();
        runtime.addGlobalEventListener(received::add);

        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("empty", null);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.run("empty", BuiltInEvaluators.EXACT);

        // 空集 run 不发 eval 事件（语义 = 评估完成，空集无评估发生）
        assertThat(received.stream().noneMatch(e -> e.type().startsWith("eval."))).isTrue();

        // emitEvent 公共面直通：宿主自定义事件同通道
        try (var session = runtime.spawn("app", "agent", "emit-probe")) {
            session.emitEvent("host.custom.event", Map.of("k", "v"));
        }
        SessionEvent custom = received.stream()
                .filter(e -> "host.custom.event".equals(e.type()))
                .findFirst().orElseThrow();
        assertThat(custom.payload()).containsEntry("k", "v");
    }
}
