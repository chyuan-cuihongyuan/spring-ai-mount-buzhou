package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * 评估 run OLAP JSONL 导出（spec 88 §A / T337，与观测导出 spec 60/67 同族，
 * Langfuse/Helicone 摄取面思想）：run 明细平铺为「一行一个独立 JSON 对象」——
 * DuckDB/ClickHouse {@code read_json_auto} 直接装载，与观测导出按 runId/时间轴
 * join 做质量-行为联合分析。
 *
 * <p><b>行形态</b>（字段序稳定；汇总列反规范化进每行——单表分析免 join）：
 * <pre>
 * item   : {"kind":"item","runId":...,"datasetName":...,"itemId":...,"status":...,
 *          "detail":...,"duration_ms":...,"actualPreview":...,"passRate":...,
 *          "passed":...,"failed":...,"errored":...,"total":...,
 *          "datasetFingerprint":...,"startedAt":...,"finishedAt":...}
 * summary: {"kind":"summary","runId":...,"datasetName":...,"passRate":...,
 *          "passed":...,"failed":...,"errored":...,"total":...,
 *          "datasetFingerprint":...,"startedAt":...,"finishedAt":...}
 * </pre>
 * 序列化走 Jackson JsonGenerator（换行天然转义——绝不手工拼接）；未知 runId =
 * {@code runFound=false} 零行（诚实：不产出空 summary 假装导出成功）。
 *
 * @since 1.0.0
 */
public final class EvalRunJsonlExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 导出结果（item 行数 / 是否含 summary 行 / run 是否存在）。 */
    public record EvalJsonlResult(long itemLines, boolean summaryLine, boolean runFound) {
    }

    private final EvalQueryService query;

    public EvalRunJsonlExporter(EvalQueryService query) {
        this.query = query;
    }

    /** 单 run 导出（item 行 ×N + summary 行 ×1；未知 run = 零行 + runFound=false）。 */
    public EvalJsonlResult exportRun(String runId, Writer out) throws IOException {
        java.util.Optional<EvalRunResult> run = query.run(runId);
        if (run.isEmpty()) {
            return new EvalJsonlResult(0, false, false);
        }
        long items = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (EvalRunItemResult item : run.get().items()) {
                gen.writeStartObject();
                gen.writeStringField("kind", "item");
                writeRunColumns(gen, run.get());
                gen.writeStringField("itemId", item.itemId());
                gen.writeStringField("status", item.status());
                gen.writeStringField("detail", item.detail());
                gen.writeNumberField("duration_ms", item.durationMs());
                gen.writeStringField("actualPreview", item.actualPreview());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                items++;
            }
            gen.writeStartObject();
            gen.writeStringField("kind", "summary");
            writeRunColumns(gen, run.get());
            gen.writeEndObject();
            gen.flush();
            out.write('\n');
        }
        return new EvalJsonlResult(items, true, true);
    }

    /** 全部 run 导出（startedAt 倒序；每 run item 行 + summary 行）。 */
    public EvalJsonlResult exportAll(Writer out) throws IOException {
        List<EvalQueryService.EvalRunSummary> runs = query.allRuns();
        long items = 0;
        boolean anySummary = false;
        for (EvalQueryService.EvalRunSummary summary : runs) {
            EvalJsonlResult part = exportRun(summary.runId(), out);
            items += part.itemLines();
            anySummary |= part.summaryLine();
        }
        return new EvalJsonlResult(items, anySummary, true);
    }

    private static void writeRunColumns(JsonGenerator gen, EvalRunResult run) throws IOException {
        gen.writeStringField("runId", run.runId());
        gen.writeStringField("datasetName", run.datasetName());
        gen.writeNumberField("passRate", run.passRate());
        gen.writeNumberField("passed", run.passed());
        gen.writeNumberField("failed", run.failed());
        gen.writeNumberField("errored", run.errored());
        gen.writeNumberField("total", run.total());
        if (run.datasetFingerprint() != null) {
            gen.writeStringField("datasetFingerprint", run.datasetFingerprint());
        }
        gen.writeStringField("startedAt", run.startedAt().toString());
        gen.writeStringField("finishedAt", run.finishedAt().toString());
    }
}
