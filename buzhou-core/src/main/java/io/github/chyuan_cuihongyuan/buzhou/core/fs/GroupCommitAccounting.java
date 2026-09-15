package io.github.chyuan_cuihongyuan.buzhou.core.fs;

/**
 * 组提交账面（spec 1846 / T2893 / impl 1447）——MySQL group commit /
 * PostgreSQL commit_delay 思想：多个写请求**合并一次刷盘**（fsync 摊薄——
 * 每次刷盘贵而写本身便宜），账面回答三问：摊薄了几倍（amortization）、
 * 省了几次刷（savedFlushes）、省了多少时延成本（savings）——组提交值不值
 *（等待合并的迟滞 vs 刷盘节省）由数说话。
 *
 * <p>纯函数零状态、只记账不合并（刷盘策略归宿主）。
 */
public final class GroupCommitAccounting {

    private GroupCommitAccounting() {
    }

    /**
     * 组提交账契约：writes ≥ 0、0 ≤ flushes ≤ writes（有写必有刷且刷不多于
     * 写——一刷多写是合并、一写多刷是浪费不入账）；成本 ≥ 0。
     */
    public record Account(long writes, long flushes, long soloCostNanos,
                          long batchedCostNanos) {

        public Account {
            boolean malformed = writes < 0 || flushes < 0 || flushes > writes
                    || soloCostNanos < 0 || batchedCostNanos < 0
                    || (writes > 0 && flushes == 0);
            if (malformed) {
                throw new IllegalArgumentException(String.format(
                        "非法组提交账：writes=%d, flushes=%d, solo=%d, batched=%d",
                        writes, flushes, soloCostNanos, batchedCostNanos));
            }
        }

        /** 摊薄倍数 = writes/flushes（零写 -1 哨兵；越高一刷多写越有效）。 */
        public double amortizationRatio() {
            return writes == 0 ? -1d : (double) writes / flushes;
        }

        /** 省下的刷盘次数 = writes − flushes。 */
        public long savedFlushes() {
            return writes - flushes;
        }

        /** 时延净省 = writes×solo − flushes×batched（可为负——合并不划算）。 */
        public long savingsNanos() {
            return writes * soloCostNanos - flushes * batchedCostNanos;
        }

        /** 节省率 = savings/(writes×solo)（零写或零solo -1 哨兵）。 */
        public double savingsRatio() {
            long denominator = writes * soloCostNanos;
            return denominator == 0 ? -1d : (double) savingsNanos() / denominator;
        }
    }
}
