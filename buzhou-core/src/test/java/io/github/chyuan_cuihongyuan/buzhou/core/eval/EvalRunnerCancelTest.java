package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1505 / T2261–T2262：评估 run 协作式取消——第 N 项触发 requestCancel() 后
 * 剩余项标 cancelled 未执行、已完成项保留、取消后的下一次 run 不受残留标记污染
 * （Kubernetes Job 删除传播语义：在飞项做完、未启动项不再启动）。
 */
class EvalRunnerCancelTest {

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

    /** 第 triggerAfter 项评估时触发取消的评估器（计数验证未启动项不再执行）。 */
    private static final class CancelAtEvaluator implements Evaluator {
        private final EvalRunner runner;
        private final int triggerAfter;
        private final AtomicInteger calls = new AtomicInteger();

        CancelAtEvaluator(EvalRunner runner, int triggerAfter) {
            this.runner = runner;
            this.triggerAfter = triggerAfter;
        }

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            if (calls.incrementAndGet() >= triggerAfter) {
                runner.requestCancel();
            }
            return EvalScore.pass("ok");
        }
    }

    /** 第 2 项触发：前 2 项真实完成（pass），剩余 3 项 cancelled 未执行。 */
    @Test
    void cancelAtSecondItemShouldMarkRemainderCancelled() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-cancel", 5);
        CancelAtEvaluator evaluator = new CancelAtEvaluator(evalRunner, 2);

        EvalRunResult result = evalRunner.run("ds-cancel", evaluator);

        assertThat(evaluator.calls.get()).isEqualTo(2); // 未启动项不再执行
        // spec 1534 / T2319：进度读面——终态快照 done=5（含 3 cancelled 占位）total=5
        assertThat(evalRunner.progress().total()).isEqualTo(5);
        assertThat(evalRunner.progress().done()).isEqualTo(5);
        assertThat(evalRunner.progress().cancelled()).isTrue();
        assertThat(result.items()).hasSize(5);
        assertThat(result.items().get(0).status()).isEqualTo(EvalRunItemResult.STATUS_PASS);
        assertThat(result.items().get(1).status()).isEqualTo(EvalRunItemResult.STATUS_PASS);
        assertThat(result.items().subList(2, 5))
                .allMatch(r -> EvalRunItemResult.STATUS_CANCELLED.equals(r.status()));
        // cancelled 不进 pass/fail/error 任一桶
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.errored()).isZero();
    }

    /** run 开始清零：取消后的下一次 run 完整执行不受残留标记污染。 */
    @Test
    void nextRunShouldNotBePollutedByStaleCancelFlag() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-stale", 3);
        evalRunner.run("ds-stale", new CancelAtEvaluator(evalRunner, 1)); // 首跑即取消

        EvalRunResult second = evalRunner.run("ds-stale", new Evaluator() {
            @Override
            public EvalScore evaluate(String actual, String expected, EvalItem item) {
                return EvalScore.pass("ok");
            }
        });

        assertThat(second.items()).hasSize(3);
        assertThat(second.items())
                .allMatch(r -> EvalRunItemResult.STATUS_PASS.equals(r.status()));
        assertThat(second.passed()).isEqualTo(3);
    }

    /** 并行路径同语义：task 首行检查取消，cancelled 项按项序聚合。 */
    @Test
    void parallelPathShouldHonourCancelAtItemBoundary() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner evalRunner = runner(stores, "ds-par", 6);
        CancelAtEvaluator evaluator = new CancelAtEvaluator(evalRunner, 1); // 首项即取消

        EvalRunResult result = evalRunner.run("ds-par", evaluator, 4);

        assertThat(result.items()).hasSize(6);
        // 并行路径 task 首行检查：全部 6 项要么在飞完成要么 cancelled（不炸整跑、
        // 结果按项序聚合完整落盘）
        assertThat(result.items())
                .allMatch(r -> EvalRunItemResult.STATUS_PASS.equals(r.status())
                        || EvalRunItemResult.STATUS_CANCELLED.equals(r.status()));
        assertThat(result.errored()).isZero();
    }
}
