package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 工具调用结局（wayfinder2 impl-07 / T33）：事件溯源日志的 outcome 维度
 * （Temporal Event History 的 Activity 结果「只记录一次」语义）。
 */
public enum ToolCallOutcome {
    COMPLETED,
    FAILED,
    TIMEOUT,
    CANCELLED,
    /** 参数未过 schema、工具未执行（impl-04 通道）。 */
    VALIDATION_REJECTED,
    /**
     * 本调用合法但因同批同伴前检未过而整批未派发（spec 122 / impl-271 superstep
     * 原子批）——「未执行因同伴」，与「执行了但失败」可区分。
     */
    BATCH_ABORTED
}
