package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 113 §B / T408：按版本查 run 红队——同指纹跨数据集名聚合（快照场景）；
  数据集演化后旧 run 归旧版本；旧记录（无指纹）不误入；摘要行携带指纹。
 * spec 82/100 组合：快照冻结版本 → 历史一查即得。
 */
class RunsOfVersionTest {

    @Test
    void runsOfVersionAggregatesAcrossDatasetNamesAndExcludesDrifted() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        var runtime = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                        new AssistantMessage(prompt.getInstructions().getLast().getText())))),
                stores, RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, ds, stores.sessionStateStore());

        // 数据集 v1（一条内容）→ 快照 frozen-v1 → 再跑快照；随后 live 演化（加条目）再跑
        ds.createDataset("live", null);
        ds.addItem("live", "q1", "q1", null, null);
        ds.snapshotDataset("live", "frozen-v1");
        EvalRunResult liveV1 = runner.run("live", BuiltInEvaluators.EXACT);
        EvalRunResult frozenRun = runner.run("frozen-v1", BuiltInEvaluators.EXACT);
        String v1Fingerprint = ds.fingerprint("live").orElseThrow();
        ds.addItem("live", "q2", "q2", null, null); // 演化 → v2
        EvalRunResult liveV2 = runner.run("live", BuiltInEvaluators.EXACT);

        var query = new EvalQueryService(stores.sessionStateStore());
        List<EvalQueryService.EvalRunSummary> v1Runs = query.runsOfVersion(v1Fingerprint);

        // 同版本跨名聚合（live v1 + frozen-v1），演化后的 v2 不入
        assertThat(v1Runs).extracting(EvalQueryService.EvalRunSummary::runId)
                .containsExactlyInAnyOrder(liveV1.runId(), frozenRun.runId())
                .doesNotContain(liveV2.runId());
        // 摘要行携带指纹（spec 113 §A）
        assertThat(v1Runs).allSatisfy(s ->
                assertThat(s.datasetFingerprint()).isEqualTo(v1Fingerprint));
        // 空/空白指纹 = 空结果（诚实不猜）
        assertThat(query.runsOfVersion(null)).isEmpty();
        assertThat(query.runsOfVersion(" ")).isEmpty();
    }
}
