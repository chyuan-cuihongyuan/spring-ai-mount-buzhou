package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 535 / T823：error 项重试一次——抖动 error 重跑转 pass（detail
 * [RETRIED] 留痕）、语义 fail 不重试（不掩盖真实回归）、默认关。
 */
class EvalErrorRetryTest {

    private static EvalRunner runner(BuzhouStores stores, String dataset, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(dataset, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(dataset, "问题" + i, "ok", null, null);
        }
        return new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("ok")))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore());
    }

    /** 首次调用抛错、随后通过的评估器（单数据集单项场景）。 */
    private static final class FlakyOnceEvaluator implements Evaluator {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("judge 抖动");
            }
            return EvalScore.pass("ok");
        }

        int calls() {
            return calls.get();
        }
    }

    /** 恒定语义 fail 的评估器（计数调用验证不重试）。 */
    private static final class AlwaysFailEvaluator implements Evaluator {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            calls.incrementAndGet();
            return EvalScore.fail("语义不符");
        }

        int calls() {
            return calls.get();
        }
    }

    @Test
    void errorItemRetriedOnceAndTurnsPass() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds", 1);
        runner.setErrorRetryOnce(true);
        FlakyOnceEvaluator evaluator = new FlakyOnceEvaluator();
        EvalRunResult run = runner.run("ds", evaluator);

        assertThat(run.errored()).isZero();
        assertThat(run.passed()).isEqualTo(1);
        assertThat(run.items().getFirst().detail()).startsWith("[RETRIED]");
        assertThat(evaluator.calls()).isEqualTo(2); // 首跑 + 重试
    }

    @Test
    void semanticFailIsNeverRetried() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds", 1);
        runner.setErrorRetryOnce(true);
        AlwaysFailEvaluator evaluator = new AlwaysFailEvaluator();
        EvalRunResult run = runner.run("ds", evaluator);

        assertThat(run.failed()).isEqualTo(1);
        assertThat(evaluator.calls()).isEqualTo(1); // 语义 fail 不重试
        assertThat(run.items().getFirst().detail()).doesNotStartWith("[RETRIED]");
    }

    @Test
    void retryDisabledByDefault() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds", 1);
        FlakyOnceEvaluator evaluator = new FlakyOnceEvaluator();
        EvalRunResult run = runner.run("ds", evaluator);
        assertThat(run.errored()).isEqualTo(1);
        assertThat(evaluator.calls()).isEqualTo(1); // 默认关——零行为变化
    }
}
