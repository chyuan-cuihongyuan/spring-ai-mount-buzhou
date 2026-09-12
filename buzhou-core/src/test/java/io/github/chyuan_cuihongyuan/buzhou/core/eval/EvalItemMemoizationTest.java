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
 * spec 708 / T1016–T1017：评估项记忆化——命中零模型调用+[MEMO] 前缀、
 * key 失配全量重跑、默认关零行为、ERROR 不缓存。
 */
class EvalItemMemoizationTest {

    /** 计数模型：每次真实模型调用 +1（ScriptedChatModel 池外兜底）。 */
    private static ScriptedChatModel countingModel(AtomicInteger calls) {
        return new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                calls.incrementAndGet();
                return super.call(prompt);
            }
        };
    }

    private static EvalDatasetStore dataset(BuzhouStores stores) {
        EvalDatasetStore datasetStore = new EvalDatasetStore(stores.sessionStateStore());
        datasetStore.createDataset("suite", null);
        datasetStore.addItem("suite", "a1", "a1", null, null); // input=expected → EXACT 必过
        return datasetStore;
    }

    @Test
    void memoHitSkipsModelCallAndMarksDetail() {
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = dataset(stores);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setMemoizationKey("judge-v1");

        EvalRunResult first = runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(first.items().get(0).status()).isEqualTo("pass");
        assertThat(calls.get()).isEqualTo(1);
        assertThat(first.items().get(0).detail()).doesNotContain("[MEMO]");

        EvalRunResult second = runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(calls.get()).as("命中记忆化——模型零调用").isEqualTo(1);
        assertThat(second.items().get(0).status()).isEqualTo("pass");
        assertThat(second.items().get(0).detail()).startsWith("[MEMO]");
        assertThat(second.items().get(0).actualPreview()).isEqualTo("a1");
    }

    @Test
    void keyChangeInvalidatesAndNewItemRunsPerItem() {
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = dataset(stores);
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());
        runner.setMemoizationKey("judge-v1");
        runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(calls.get()).isEqualTo(1);

        // 判定身份换版 → sig 全量失配 → 真实重跑
        runner.setMemoizationKey("judge-v2");
        model.enqueueText("a1");
        EvalRunResult second = runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(calls.get()).as("key 失配必须重跑").isEqualTo(2);
        assertThat(second.items().get(0).detail()).doesNotContain("[MEMO]");

        // 新增条目：旧条目照常命中（项粒度），新条目真实执行
        datasetStore.addItem("suite", "a1", "a1", null, null); // id=000002
        model.enqueueText("a1");
        EvalRunResult third = runner.run("suite", BuiltInEvaluators.EXACT);
        assertThat(calls.get()).as("只有新条目重跑").isEqualTo(3);
        assertThat(third.items().get(0).detail()).startsWith("[MEMO]");
        assertThat(third.items().get(1).detail()).doesNotContain("[MEMO]");
        assertThat(third.total()).isEqualTo(2);
    }

    @Test
    void disabledByDefaultAndErrorsNotCached() {
        AtomicInteger calls = new AtomicInteger();
        ScriptedChatModel model = countingModel(calls);
        model.enqueueText("a1");
        model.enqueueText("a1");
        model.enqueueText("a1");
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        EvalDatasetStore datasetStore = dataset(stores);
        // judge 抛异常 → ERROR 三态（瞬时故障形态）
        Evaluator boom = (actual, expected, item) -> {
            throw new IllegalStateException("judge 崩了");
        };
        EvalRunner runner = new EvalRunner(runtime, datasetStore, stores.sessionStateStore());

        EvalRunResult first = runner.run("suite", boom);
        assertThat(first.items().get(0).status()).isEqualTo("error");
        EvalRunResult second = runner.run("suite", boom);
        assertThat(calls.get()).as("默认关——二跑照常执行").isEqualTo(2);

        // 设置 key 后 error 仍不缓存（真实重跑）
        runner.setMemoizationKey("judge-v1");
        EvalRunResult third = runner.run("suite", boom);
        assertThat(calls.get()).isEqualTo(3);
        assertThat(third.items().get(0).detail()).doesNotContain("[MEMO]");
        EvalRunResult fourth = runner.run("suite", boom);
        assertThat(calls.get()).as("ERROR 不缓存——同 key 二跑仍真实执行").isEqualTo(4);
        assertThat(fourth.items().get(0).detail()).doesNotContain("[MEMO]");
    }
}
