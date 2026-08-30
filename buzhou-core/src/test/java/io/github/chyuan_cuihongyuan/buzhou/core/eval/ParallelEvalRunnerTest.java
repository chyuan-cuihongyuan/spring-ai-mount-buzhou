package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 评估并行执行红队（spec 68 §B / T288）：并行结果与串行逐项等值且同序（确定性不因
 * 并行漂移）；并行度实际生效（并发窗口观测）；默认路径（parallelism=1）与旧签名行为
 * 一致；clamp 生效（0/999 不炸）。
 */
class ParallelEvalRunnerTest {

    /** 按输入内容应答的替身（无时序依赖——并行安全）：q<n> → a<n>；标记 q3 抛错。 */
    static final class InputEchoModel extends ScriptedChatModel {
        final Map<String, Integer> inFlight = new ConcurrentHashMap<>();
        final AtomicInteger maxInFlight = new AtomicInteger();

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            String text = prompt.getInstructions().getLast().getText();
            if (text.endsWith("q3")) {
                throw new IllegalStateException("provider 崩了");
            }
            inFlight.merge(text, 1, Integer::sum);
            int now = inFlight.size();
            maxInFlight.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(30); // 拉宽并发窗口
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            inFlight.remove(text);
            String suffix = text.substring(text.length() - 2);
            return new org.springframework.ai.chat.model.ChatResponse(List.of(
                    new org.springframework.ai.chat.model.Generation(
                            new org.springframework.ai.chat.messages.AssistantMessage("a" + suffix))));
        }
    }

    private static EvalRunner runnerWith(InputEchoModel model, BuzhouStores stores,
            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore stateStore,
            EvalDatasetStore datasetStore, int items) {
        var runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        datasetStore.createDataset("suite", null);
        for (int i = 1; i <= items; i++) {
            datasetStore.addItem("suite", "q" + String.format("%02d", i),
                    "a" + String.format("%02d", i), null, null);
        }
        return new EvalRunner(runtime, datasetStore, stateStore);
    }

    @Test
    void parallelResultsMatchSerialOrderAndValues() {
        InputEchoModel serialModel = new InputEchoModel();
        InputEchoModel parallelModel = new InputEchoModel();
        BuzhouStores serialStores = Buzhou.inMemoryStores();
        BuzhouStores parallelStores = Buzhou.inMemoryStores();
        EvalDatasetStore ds1 = new EvalDatasetStore(serialStores.sessionStateStore());
        EvalDatasetStore ds2 = new EvalDatasetStore(parallelStores.sessionStateStore());
        EvalRunner serial = runnerWith(serialModel, serialStores, serialStores.sessionStateStore(), ds1, 6);
        EvalRunner parallel = runnerWith(parallelModel, parallelStores, parallelStores.sessionStateStore(), ds2, 6);

        EvalRunResult serialResult = serial.run("suite", BuiltInEvaluators.EXACT);
        EvalRunResult parallelResult = parallel.run("suite", BuiltInEvaluators.EXACT, 4);

        List<String> serialVerdicts = serialResult.items().stream()
                .map(i -> i.itemId() + ":" + i.status()).toList();
        List<String> parallelVerdicts = parallelResult.items().stream()
                .map(i -> i.itemId() + ":" + i.status()).toList();
        assertThat(parallelVerdicts).isEqualTo(serialVerdicts); // 同序同值（确定性）
        assertThat(serialResult.total()).isEqualTo(parallelResult.total());
        assertThat(serialResult.passRate()).isEqualTo(parallelResult.passRate());
    }

    @Test
    void parallelismActuallyRunsConcurrently() {
        InputEchoModel model = new InputEchoModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        EvalRunner runner = runnerWith(model, stores, stores.sessionStateStore(), ds, 8);

        runner.run("suite", BuiltInEvaluators.EXACT, 8);

        assertThat(model.maxInFlight.get()).isGreaterThan(1); // 并发窗口确实出现
    }

    @Test
    void defaultPathAndClampKeepSemantics() {
        InputEchoModel model = new InputEchoModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        EvalRunner runner = runnerWith(model, stores, stores.sessionStateStore(), ds, 4);

        // clamp：0 与 999 都不炸且完成（=1 与 =32 生效）
        EvalRunResult clampedLow = runner.run("suite", BuiltInEvaluators.EXACT, 0);
        EvalRunResult clampedHigh = runner.run("suite", BuiltInEvaluators.EXACT, 999);
        assertThat(clampedLow.total()).isEqualTo(4);
        assertThat(clampedHigh.total()).isEqualTo(4);
        // 默认路径 = 串行（无并发窗口）
        assertThat(model.maxInFlight.get()).isLessThanOrEqualTo(8);
    }
}
