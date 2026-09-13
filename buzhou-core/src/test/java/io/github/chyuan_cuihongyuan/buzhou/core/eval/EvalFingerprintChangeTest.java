package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 734 / T1068–T1069：数据集指纹变更信号——增删项置位、稳定复跑复位、
 * 首跑 false。
 */
class EvalFingerprintChangeTest {

    private EvalDatasetStore datasetWith(BuzhouStores stores, String name, String expected) {
        EvalDatasetStore store = new EvalDatasetStore(stores.sessionStateStore());
        store.createDataset(name, null);
        store.addItem(name, "a1", expected, null, null);
        return store;
    }

    @Test
    void fingerprintChangeIsSignaled() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                calls.incrementAndGet();
                return super.call(prompt);
            }
        };
        model.enqueueText("a1");
        model.enqueueText("a1");
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = datasetWith(stores, "suite", "a1");
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());

        runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(runner.lastFingerprintChanged()).isFalse(); // 首跑无前值

        // 增项 → 指纹变化
        datasetStore.addItem("suite", "a1", "a1", null, null);
        model.enqueueText("a1");
        runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(runner.lastFingerprintChanged()).isTrue();

        // 稳定复跑 → 复位
        model.enqueueText("a1");
        model.enqueueText("a1");
        runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(runner.lastFingerprintChanged()).isFalse();
    }
}
