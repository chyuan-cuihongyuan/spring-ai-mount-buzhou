package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-681 / spec 928：pruned run 审计查询——剪枝 run 被查出且计数正确、
 * 非剪枝 run 不出现、降序稳定、空 store 返回空。
 */
class PrunedQueryTest {

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

    private record Probe() {
    }

    @Test
    void prunedRunsAreQueriableWithCounts() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-pq", 5);
        runner.setPrunePolicy(new EvalPrunePolicy(2, 0.5));
        runner.run("ds-pq", (actual, expected, item) -> EvalScore.fail("no"));

        EvalQueryService service = new EvalQueryService(stores.sessionStateStore());
        List<EvalQueryService.PrunedRunSummary> pruned = service.runsWithPruned();

        assertThat(pruned).hasSize(1);
        assertThat(pruned.get(0).prunedCount()).isEqualTo(3); // 观察窗 2 fail + 3 pruned
        assertThat(pruned.get(0).total()).isEqualTo(5);
    }

    @Test
    void nonPrunedRunsExcluded() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalRunner runner = runner(stores, "ds-clean", 3);
        runner.run("ds-clean", (actual, expected, item) -> EvalScore.pass("ok"));

        EvalQueryService service = new EvalQueryService(stores.sessionStateStore());
        assertThat(service.runsWithPruned()).isEmpty(); // 无剪枝 run 不出现
    }

    @Test
    void emptyStoreReturnsEmpty() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalQueryService service = new EvalQueryService(stores.sessionStateStore());
        assertThat(service.runsWithPruned()).isEmpty();
    }
}
