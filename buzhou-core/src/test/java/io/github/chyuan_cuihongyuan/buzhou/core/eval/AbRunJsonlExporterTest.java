package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 94 §B / T354：A/B run JSONL 导出红队——行数（items+summary）；每行独立
 * JSON（winner/reason/error verdict 面 + 汇总反规范化列 + 指纹列）；未知 runId
 * 诚实零行；exportAll 倒序。spec 88 的 AB 面同构。
 */
class AbRunJsonlExporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> row(String line) throws Exception {
        return MAPPER.readValue(line, Map.class);
    }

    @Test
    void exportAbRunWritesVerdictAndSummaryLines() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("abx", null);
        ds.addItem("abx", "q1", "e1", null, null);
        ds.addItem("abx", "q2", "e2", null, null);
        var runtimeA = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage("gold-q" + prompt.getInstructions().getLast().getText())))),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runtimeB = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage("plain-" + prompt.getInstructions().getLast().getText())))),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        // 内容型 judge（双向一致：gold 标记所在位赢——与 PairwiseEvalRunnerTest 同款）
        var result = new PairwiseEvalRunner(ds, new PairwiseJudge(new PairwiseEvalRunnerTest.GoldContentJudge()),
                stores.sessionStateStore()).compare("abx", runtimeA, runtimeB, 1);

        StringWriter out = new StringWriter();
        AbRunJsonlExporter.AbJsonlResult export =
                AbRunJsonlExporter.exportRun(stores.sessionStateStore(), result.runId(), out);

        assertThat(export.runFound()).isTrue();
        assertThat(export.itemLines()).isEqualTo(2);
        String[] lines = out.toString().strip().split("\n");
        assertThat(lines).hasSize(3);

        Map<String, Object> first = row(lines[0]);
        assertThat(first.get("kind")).isEqualTo("item");
        assertThat(first.get("winner")).isEqualTo("WINNER_A");
        assertThat(first.get("runId")).isEqualTo(result.runId());
        assertThat(first.get("winRateA")).isEqualTo(1.0); // 汇总列反规范化
        assertThat(first).containsKey("datasetFingerprint");

        Map<String, Object> summary = row(lines[2]);
        assertThat(summary.get("kind")).isEqualTo("summary");
        assertThat(summary.get("total")).isEqualTo(2);
        assertThat(summary.get("winsA")).isEqualTo(2);
    }

    @Test
    void unknownAbRunExportsNothing() throws Exception {
        StringWriter out = new StringWriter();
        AbRunJsonlExporter.AbJsonlResult export =
                AbRunJsonlExporter.exportRun(Buzhou.inMemoryStores().sessionStateStore(),
                        "no-such", out);

        assertThat(export.runFound()).isFalse();
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void exportAllAbCoversRunsInReverseOrder() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("abm", null);
        ds.addItem("abm", "q", "e", null, null);
        var runtimeA = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage("gold-x")))),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runtimeB = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage("plain-x")))),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        var runner = new PairwiseEvalRunner(ds, new PairwiseJudge(prompt -> new ChatResponse(
                List.of(new Generation(new AssistantMessage("WINNER_A 好"))))),
                stores.sessionStateStore());
        var older = runner.compare("abm", runtimeA, runtimeB, 1);
        Thread.sleep(5);
        var newer = runner.compare("abm", runtimeA, runtimeB, 1);

        StringWriter out = new StringWriter();
        AbRunJsonlExporter.AbJsonlResult export =
                AbRunJsonlExporter.exportAll(stores.sessionStateStore(), out);

        assertThat(export.itemLines()).isEqualTo(2);
        String[] lines = out.toString().strip().split("\n");
        assertThat(lines).hasSize(4);
        assertThat(row(lines[0]).get("runId")).isEqualTo(newer.runId()); // 倒序
        assertThat(row(lines[2]).get("runId")).isEqualTo(older.runId());
    }
}
