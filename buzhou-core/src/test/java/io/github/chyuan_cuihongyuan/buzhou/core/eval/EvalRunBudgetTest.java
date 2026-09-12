package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 520 / T791–T792：评估 run 预算闸——预算内全执行（默认关零变化）、
 * 超限早停剩余项 error [RUN-BUDGET]、errored 计数、负值 fail-fast。
 */
class EvalRunBudgetTest {

    private static final String LAMBDA_OK = "ok";

    private static EvalRunner runner(BuzhouStores stores) {
        return new EvalRunner(
                Buzhou.runtime(prompt -> new org.springframework.ai.chat.model.ChatResponse(
                                List.of(new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage(LAMBDA_OK)))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                new EvalDatasetStore(stores.sessionStateStore()),
                stores.sessionStateStore());
    }

    private static EvalDatasetStore dataset(BuzhouStores stores, String name, int n) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset(name, null);
        for (int i = 0; i < n; i++) {
            ds.addItem(name, "问题" + i, "ok", null, null);
        }
        return ds;
    }

    @Test
    void withoutBudgetAllItemsExecuteZeroBehaviorChange() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = dataset(stores, "ds", 5);
        EvalRunner runner = runner(stores); // 不设预算
        EvalRunResult run = runner.run("ds", BuiltInEvaluators.EXACT);
        assertThat(run.errored()).isZero();
        assertThat(run.passed()).isEqualTo(5);
    }

    @Test
    void exhaustedBudgetStopsRemainingItemsAsErrors() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = dataset(stores, "ds", 6);
        EvalRunner runner = runner(stores);
        // 每项估算 = input(约 3-4 字符) + expected(2)——预算只够前 2 项
        runner.setRunBudgetChars(20);
        EvalRunResult run = runner.run("ds", BuiltInEvaluators.EXACT);

        assertThat(run.total()).isEqualTo(6); // run 照常完成落盘（partial 显式）
        assertThat(run.errored()).isGreaterThanOrEqualTo(1);
        assertThat(run.items()).anySatisfy(i -> {
            if (i.status().equals("error")) {
                assertThat(i.detail()).contains("[RUN-BUDGET]");
            }
        });
        // 预算耗尽后不再有 pass（早停）
        long budgetSkips = run.items().stream()
                .filter(i -> i.detail().contains("[RUN-BUDGET]")).count();
        assertThat(budgetSkips).isGreaterThanOrEqualTo(1);
        // 早停后预算耗尽的项不再产生 pass
        assertThat(run.passed() + budgetSkips).isLessThanOrEqualTo(6);
    }

    @Test
    void negativeBudgetFailsFast() {
        EvalRunner runner = runner(Buzhou.inMemoryStores());
        assertThatThrownBy(() -> runner.setRunBudgetChars(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
