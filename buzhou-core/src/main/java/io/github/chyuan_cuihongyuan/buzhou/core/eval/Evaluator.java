package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 评估器 SPI（spec 52 §C / T192）：判定单条评估项的实际输出。
 *
 * <p>内置 {@link ExactEvaluator} / {@link ContainsEvaluator} / {@link RegexEvaluator}；
 * 宿主实现本接口即得领域自定义判定（Langfuse Scores 的 code evaluator 语义收窄）。
 * LLM-as-judge 不内置、不做门禁——宿主可自行实现本接口调 judge（边界沿用 effort #7）。
 */
@FunctionalInterface
public interface Evaluator {

    /**
     * 判定一条评估项。
     *
     * @param actual   被评估的实际输出（run 捕获；非空）
     * @param expected 期望输出（评估口径由实现解释：全等/子串/正则）
     * @param item     原评估项（溯源与元信息可用）
     * @return 得分（实现不得返回 null——返回 null 按该项 error 处理由 runner 记录）
     */
    EvalScore evaluate(String actual, String expected, EvalItem item);
}
