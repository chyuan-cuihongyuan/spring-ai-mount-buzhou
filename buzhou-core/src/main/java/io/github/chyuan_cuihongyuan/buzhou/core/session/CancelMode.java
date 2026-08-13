package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 取消模式（wayfinder2 impl-05 / T31 / docs/spec/12）：取消运行中请求的三档语义，
 * 匹配调用方意图（立即止损 vs 干净收尾）。
 *
 * <p>来源：AutoGen（{@code CancellationToken}=立即 abort + {@code ExternalTermination}=
 * 当前 turn 完成后停 的两档）+ OpenAI Agents SDK（cancel 后清理语义）——Buzhou 取
 * 两档 + 中间档「当前工具批后」共三档。
 */
public enum CancelMode {

    /**
     * 立即：中断全部在飞工具调用、<b>丢弃在飞结果</b>（防半成品泄漏，吸取 AutoGen
     * 「未消费 partial 丢弃」语义）；后续工具轮次被护栏替换为优雅取消收尾。
     */
    IMMEDIATE,

    /**
     * 当前工具批完成后停：不中断在飞工具（其结果正常回喂一次），但<b>不再进入下一轮
     * think→tool 递归</b>——循环优雅收尾。
     */
    AFTER_CURRENT_TOOLS,

    /**
     * 当前 Turn 完整收尾：不打扰本轮（模型自然产出最终回复、部分输出完整落
     * Completed-Turn），取消仅作标记与可观测；会话仍可继续 chat。
     */
    AFTER_CURRENT_TURN
}
