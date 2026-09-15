package io.github.chyuan_cuihongyuan.buzhou.mcp;

/**
 * 重连退避阶梯（spec 1844 / T2889 / impl 1445）——TCP/HTTP 客户端重连
 * 惯例（Resilience4j retry / Postgres libpq）：断线重连延迟指数爬升
 *（100→200→400ms…）但**封顶**（不无限涨——网络分区恢复后首连别等天荒
 * 地老），超过最大尝试次数则判 GIVE_UP（交给上层兜底/人工）——「退避
 * 抚网络、封顶保时延、上限知放弃」三段式。溢出安全：先比阶数再乘。
 *
 * <p>纯函数零状态、只算延迟不执行重连（连接管理归宿主）。
 */
public final class ReconnectBackoffLadder {

    private ReconnectBackoffLadder() {
    }

    /** 重连裁决两态：RETRY 再试 / GIVE_UP 放弃（超最大尝试）。 */
    public enum Verdict {

        /** 未超最大尝试——按阶梯延迟再试。 */
        RETRY,

        /** 超过最大尝试——放弃（上层兜底/人工介入）。 */
        GIVE_UP
    }

    /**
     * 阶梯延迟：min(base × multiplier^(attempt−1), cap)。契约：attempt ≥ 1、
     * base ≥ 1、multiplier ≥ 1、cap ≥ base（fail-fast）；溢出安全——增幅
     * 触顶即返回 cap 不做天文乘法。
     */
    public static long delayMillis(int attempt, long baseMillis, long multiplier,
                                   long capMillis) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt 不能小于 1：" + attempt);
        }
        if (baseMillis < 1 || multiplier < 1 || capMillis < baseMillis) {
            throw new IllegalArgumentException(String.format(
                    "非法阶梯参：base=%d, multiplier=%d, cap=%d（要求 base≥1、multiplier≥1、cap≥base）",
                    baseMillis, multiplier, capMillis));
        }
        long delay = baseMillis;
        for (int i = 1; i < attempt; i++) {
            if (delay > capMillis / multiplier) {
                return capMillis; // 触顶即封——不做溢出乘法
            }
            delay *= multiplier;
            if (delay >= capMillis) {
                return capMillis;
            }
        }
        return Math.min(delay, capMillis);
    }

    /**
     * 放弃裁决：attempt &gt; maxAttempts 即 GIVE_UP（边界含——第 maxAttempts
     * 次仍是 RETRY）。契约：attempt ≥ 1、maxAttempts ≥ 1。
     */
    public static Verdict verdict(int attempt, int maxAttempts) {
        if (attempt < 1 || maxAttempts < 1) {
            throw new IllegalArgumentException(String.format(
                    "attempt=%d, maxAttempts=%d（均须 ≥ 1）", attempt, maxAttempts));
        }
        return attempt > maxAttempts ? Verdict.GIVE_UP : Verdict.RETRY;
    }
}
