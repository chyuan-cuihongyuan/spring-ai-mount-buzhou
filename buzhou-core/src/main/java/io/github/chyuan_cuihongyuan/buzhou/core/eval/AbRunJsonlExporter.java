package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

/**
 * A/B run OLAP JSONL 导出（spec 94 §A / T353，spec 88 的 AB 面同构扩展）：
 * verdict 明细平铺「一行一个独立 JSON 对象」，与 eval run / 观测导出同装载面
 * （DuckDB/ClickHouse 三表 join——同一 runId/时间轴下的质量-行为-对比联合分析）。
 *
 * <p><b>行形态</b>（汇总列反规范化进每行；输出原文不落盘故无 actual 列——spec 74）：
 * <pre>
 * item   : {"kind":"item","runId":...,"datasetName":...,"itemId":...,"winner":...,
 *          "reason":...,"error":...,"winRateA":...,"winRateB":...,"winsA":...,
 *          "winsB":...,"ties":...,"errors":...,"total":...,
 *          "datasetFingerprint":...,"startedAt":...,"finishedAt":...}
 * summary: {"kind":"summary","runId":...,...同汇总列...}
 * </pre>
 * 静态面（AB 查询本就是 PairwiseEvalRunner 静态方法——不重复构造链）；未知 runId =
 * runFound=false 零行（诚实）。
 *
 * @since 1.0.0
 */
public final class AbRunJsonlExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 导出结果（item 行数 / 是否含 summary 行 / run 是否存在）。 */
    public record AbJsonlResult(long itemLines, boolean summaryLine, boolean runFound) {
    }

    private AbRunJsonlExporter() {
    }

    /** 单 A/B run 导出（item 行 ×N + summary 行 ×1；未知 run = 零行 + runFound=false）。 */
    public static AbJsonlResult exportRun(SessionStateStore store, String runId, Writer out)
            throws IOException {
        Optional<PairwiseEvalRunner.PairwiseEvalResult> run = PairwiseEvalRunner.abRun(store, runId);
        if (run.isEmpty()) {
            return new AbJsonlResult(0, false, false);
        }
        return writeRun(run.get(), out);
    }

    /** 全部 A/B run 导出（startedAt 倒序）。 */
    public static AbJsonlResult exportAll(SessionStateStore store, Writer out) throws IOException {
        List<PairwiseEvalRunner.AbRunSummary> runs = PairwiseEvalRunner.abRuns(store, null);
        long items = 0;
        boolean anySummary = false;
        for (PairwiseEvalRunner.AbRunSummary summary : runs) {
            AbJsonlResult part = exportRun(store, summary.runId(), out);
            items += part.itemLines();
            anySummary |= part.summaryLine();
        }
        return new AbJsonlResult(items, anySummary, true);
    }

    private static AbJsonlResult writeRun(PairwiseEvalRunner.PairwiseEvalResult run, Writer out)
            throws IOException {
        long items = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (PairwiseEvalRunner.PairwiseItemResult item : run.items()) {
                gen.writeStartObject();
                gen.writeStringField("kind", "item");
                writeRunColumns(gen, run);
                gen.writeStringField("itemId", item.itemId());
                gen.writeStringField("winner", item.error() != null || item.verdict() == null
                        ? null : item.verdict().winner().name());
                gen.writeStringField("reason", item.error() != null || item.verdict() == null
                        ? null : item.verdict().reason());
                gen.writeStringField("error", item.error());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                items++;
            }
            gen.writeStartObject();
            gen.writeStringField("kind", "summary");
            writeRunColumns(gen, run);
            gen.writeEndObject();
            gen.flush();
            out.write('\n');
        }
        return new AbJsonlResult(items, true, true);
    }

    private static void writeRunColumns(JsonGenerator gen,
            PairwiseEvalRunner.PairwiseEvalResult run) throws IOException {
        gen.writeStringField("runId", run.runId());
        gen.writeStringField("datasetName", run.datasetName());
        PairwiseEvalRunner.PairwiseSummary s = run.summary();
        gen.writeNumberField("winRateA", s.winRateA());
        gen.writeNumberField("winRateB", s.winRateB());
        gen.writeNumberField("winsA", s.winsA());
        gen.writeNumberField("winsB", s.winsB());
        gen.writeNumberField("ties", s.ties());
        gen.writeNumberField("errors", s.errors());
        gen.writeNumberField("total", s.total());
        if (run.datasetFingerprint() != null) {
            gen.writeStringField("datasetFingerprint", run.datasetFingerprint());
        }
        gen.writeStringField("startedAt", run.startedAt().toString());
        gen.writeStringField("finishedAt", run.finishedAt().toString());
    }
}
