package io.github.chyuan_cuihongyuan.buzhou.memory.evaluation;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * 压缩保真度评测（wayfinder2 impl-14 / T39 / docs/spec/12 §memory-11，LangChain
 * trace 注入式 eval 方法论的确定性落地）：注入「压缩前水位之下才可答」的探针，
 * 断言摘要保住答案——evidence-id 精确断言（指针在/原文可回查）或关键词保持。
 * LLM judge 可经 {@link Judge} 接口替换（默认确定性 judge：渲染文本包含判定）。
 */
public final class CompactionFidelityEval {

    /** 探针：问题 + 压缩前才有的期望要点（关键词，或 evidence-id 指针）。 */
    public record Probe(String question, String expectKeyword) {
    }

    /** 判定器（可插拔 LLM judge；默认确定性包含判定）。 */
    public interface Judge {
        boolean answerable(String summaryRender, Probe probe);
    }

    /** 评测结果。 */
    public record Result(int total, int retained, List<String> misses) {
        public double fidelityRate() {
            return total == 0 ? 1.0 : (double) retained / total;
        }
    }

    private final Judge judge;

    public CompactionFidelityEval() {
        this((render, probe) -> render.contains(probe.expectKeyword()));
    }

    public CompactionFidelityEval(Judge judge) {
        this.judge = judge;
    }

    /** 评测：探针要点在摘要渲染（或其 evidence 指针域）中保持的比例。 */
    public Result evaluate(NineSectionSummary summary, List<Probe> probes) {
        String render = summary == null ? "" : summary.render();
        List<String> misses = new ArrayList<>();
        int retained = 0;
        for (Probe probe : probes) {
            if (judge.answerable(render, probe)) {
                retained++;
            } else {
                misses.add(probe.question());
            }
        }
        return new Result(probes.size(), retained, misses);
    }
}
