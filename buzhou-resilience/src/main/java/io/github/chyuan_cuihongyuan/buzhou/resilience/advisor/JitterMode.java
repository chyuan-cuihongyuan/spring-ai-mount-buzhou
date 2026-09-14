package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

/**
 * 退避抖动模式（spec 1631 / T2413，AWS Architecture Blog「Exponential Backoff
 * and Jitter」思想）：EQUAL（默认=既有 ±j 对称抖动）/ FULL（full jitter——
 * 随机化整个区间 [0, cap]，防重试风暴同步最优）/ DECORRELATED（与前次去相关
 * [base, min(cap, prev×3)]——防连续相关退避）。
 */
public enum JitterMode {

    /** 既有语义：capped × (1 − j + 2jU)——围绕计算值对称抖动。 */
    EQUAL,

    /** full jitter：random(0, capped)——AWS 推荐默认（防同步效果最优）。 */
    FULL,

    /** decorrelated jitter：random(base, min(cap, prev×3))——与上次退避去相关。 */
    DECORRELATED;

    /** yml 字符串解析（未知值 fail-fast）。 */
    public static JitterMode parse(String value) {
        if (value == null || value.isBlank()) {
            return EQUAL;
        }
        return JitterMode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
