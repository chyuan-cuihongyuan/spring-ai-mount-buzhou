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
 * spec 88 §B / T338：eval run JSONL 导出红队——行数（items+summary）；每行独立
 * JSON 可解析（换行转义纪律）；汇总列反规范化（单行即可分析）；指纹列存在；
 * 未知 runId 零行诚实；exportAll 倒序全量。
 */
class EvalRunJsonlExporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exportRunWritesItemAndSummaryLinesAsValidJson() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("exp", null);
        ds.addItem("exp", "hit1", "hit1", null, null);
        ds.addItem("exp", "miss1", "other", null, null);
        EvalRunResult runResult = new EvalRunner(
                Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                        new AssistantMessage(prompt.getInstructions().getLast().getText())))),
                        stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults()),
                ds, stores.sessionStateStore()).run("exp", BuiltInEvaluators.EXACT);

        StringWriter out = new StringWriter();
        EvalRunJsonlExporter.EvalJsonlResult result =
                new EvalRunJsonlExporter(new EvalQueryService(stores.sessionStateStore()))
                        .exportRun(runResult.runId(), out);

        assertThat(result.runFound()).isTrue();
        assertThat(result.itemLines()).isEqualTo(2);
        String[] lines = out.toString().strip().split("\n");
        assertThat(lines).hasSize(3); // 2 item + 1 summary

        Map<String, Object> first = MAPPER.readValue(lines[0], Map.class);
        assertThat(first.get("kind")).isEqualTo("item");
        assertThat(first.get("runId")).isEqualTo(runResult.runId());
        assertThat(first.get("datasetName")).isEqualTo("exp");
        assertThat(first.get("passRate")).isEqualTo(0.5); // 汇总列反规范化进 item 行
        assertThat(first).containsKey("datasetFingerprint");

        Map<String, Object> summary = MAPPER.readValue(lines[2], Map.class);
        assertThat(summary.get("kind")).isEqualTo("summary");
        assertThat(summary.get("total")).isEqualTo(2);
        assertThat(summary.get("passed")).isEqualTo(1);

        // detail 含换行的行也保持一行一记录（Jackson 转义纪律）——item 行都能独立解析即证
        for (String line : lines) {
            Map<String, Object> parsed = MAPPER.readValue(line, Map.class);
            assertThat(parsed).containsKey("runId");
        }
    }

    @Test
    void unknownRunExportsNothingHonestly() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();

        StringWriter out = new StringWriter();
        EvalRunJsonlExporter.EvalJsonlResult result =
                new EvalRunJsonlExporter(new EvalQueryService(stores.sessionStateStore()))
                        .exportRun("no-such", out);

        assertThat(result.runFound()).isFalse();
        assertThat(result.itemLines()).isZero();
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void exportAllCoversEveryRunInReverseOrder() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("multi", null);
        ds.addItem("multi", "q1", "q1", null, null);
        var runtime = Buzhou.runtime(prompt -> new ChatResponse(List.of(new Generation(
                new AssistantMessage(prompt.getInstructions().getLast().getText())))),
                stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        EvalRunner runner = new EvalRunner(runtime, ds, stores.sessionStateStore());
        EvalRunResult older = runner.run("multi", BuiltInEvaluators.EXACT);
        Thread.sleep(5); // startedAt 可分
        EvalRunResult newer = runner.run("multi", BuiltInEvaluators.EXACT);

        StringWriter out = new StringWriter();
        EvalRunJsonlExporter.EvalJsonlResult result =
                new EvalRunJsonlExporter(new EvalQueryService(stores.sessionStateStore()))
                        .exportAll(out);

        assertThat(result.itemLines()).isEqualTo(2);
        String[] lines = out.toString().strip().split("\n");
        assertThat(lines).hasSize(4); // 2×(1 item + 1 summary)
        assertThat(parsedRow(lines[0]).get("runId"))
                .isEqualTo(newer.runId()); // 倒序：新 run 在前
        assertThat(parsedRow(lines[2]).get("runId"))
                .isEqualTo(older.runId());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parsedRow(String line) throws Exception {
        return MAPPER.readValue(line, Map.class);
    }
}
