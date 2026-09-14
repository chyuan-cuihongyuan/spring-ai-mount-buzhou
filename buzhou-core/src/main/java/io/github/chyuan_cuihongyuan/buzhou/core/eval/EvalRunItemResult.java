package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 评估 run 单项结果（spec 52 §D / T193）。
 *
 * @param itemId        评估项 Id
 * @param status        pass | fail | error | pruned | cancelled（error = 执行异常/评估器返回 null；
 *                      pruned = spec 901 中途剪枝未执行；cancelled = spec 1505 宿主请求取消未执行）
 * @param detail        评估器 detail 或异常摘要
 * @param actualPreview 实际输出预览（2048 字符截断；截断以 "…" 标记）
 * @param durationMs    该项执行耗时
 */
public record EvalRunItemResult(String itemId, String status, String detail,
                                String actualPreview, long durationMs) {

    static final String STATUS_PASS = "pass";
    static final String STATUS_FAIL = "fail";
    static final String STATUS_ERROR = "error";
    /** impl-654 / spec 901：失败率中途剪枝——本项未执行（算力止损）。 */
    static final String STATUS_PRUNED = "pruned";
    /** spec 1505 / T2261：宿主请求取消——本项未执行（与剪枝的失败率止损语义分立）。 */
    static final String STATUS_CANCELLED = "cancelled";
}
