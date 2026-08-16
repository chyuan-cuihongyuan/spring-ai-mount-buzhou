package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.time.Instant;

/**
 * 评估项（spec 52 §A / T190）：数据集内单条输入-期望对，带可选溯源
 * （Langfuse DatasetItem.sourceTraceId 语义收窄为 sessionId + turnSeq；手工项两字段可空）。
 *
 * @param id               条目 Id（dataset 内单调递增、6 位零填充——键序即添加序）
 * @param input            评估输入（非空）
 * @param expected         期望输出（非空；评估器口径由 Evaluator 解释）
 * @param sourceSessionId  溯源会话（回流项必填；手工项可空）
 * @param sourceTurnSeq    溯源轮次（回流项必填；手工项可空）
 * @param createdAt        创建时刻
 */
public record EvalItem(String id, String input, String expected,
                       String sourceSessionId, Integer sourceTurnSeq, Instant createdAt) {
}
