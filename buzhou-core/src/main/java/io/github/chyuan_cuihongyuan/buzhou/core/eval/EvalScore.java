package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 评估得分（spec 52 §C / T192）：passed 判定 + 512 字符截断的 detail
 * （防 actual 全文灌进 run 记录；截断以 "…" 标记）。
 */
public record EvalScore(boolean passed, String detail) {

    static final int DETAIL_LIMIT = 512;

    public EvalScore {
        if (detail != null && detail.length() > DETAIL_LIMIT) {
            detail = detail.substring(0, DETAIL_LIMIT) + "…";
        }
    }

    public static EvalScore pass(String detail) {
        return new EvalScore(true, detail);
    }

    public static EvalScore fail(String detail) {
        return new EvalScore(false, detail);
    }
}
