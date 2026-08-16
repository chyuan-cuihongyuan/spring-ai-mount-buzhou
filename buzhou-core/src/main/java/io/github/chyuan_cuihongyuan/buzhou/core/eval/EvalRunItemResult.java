package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 评估 run 单项结果（spec 52 §D / T193）。
 *
 * @param itemId        评估项 Id
 * @param status        pass | fail | error（error = 执行异常/评估器返回 null）
 * @param detail        评估器 detail 或异常摘要
 * @param actualPreview 实际输出预览（2048 字符截断；截断以 "…" 标记）
 * @param durationMs    该项执行耗时
 */
public record EvalRunItemResult(String itemId, String status, String detail,
                                String actualPreview, long durationMs) {

    static final String STATUS_PASS = "pass";
    static final String STATUS_FAIL = "fail";
    static final String STATUS_ERROR = "error";
}
