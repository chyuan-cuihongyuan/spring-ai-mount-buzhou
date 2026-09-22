package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * 分片偏斜审计（spec 1906 / T3013 / impl 1507）——Spark/ShardedDB
 * data skew 语义：hottest 分片负载 / 平均负载 = 偏斜比。1.0 绝对
 * 均衡；2.0 = 热分片双倍承担。重分片触发线有刻度——主动式而非
 * 故障后复盘式。
 *
 * <p>纯函数零状态；无流量（全零）时偏斜无意义 fail-fast。
 */
public final class ShardSkewAudit {

    private ShardSkewAudit() {
    }

    /**
     * 偏斜比：max/avg。契约：loads 非空、逐值 ≥ 0、avg &gt; 0
     * （全零 = 无流量，fail-fast）。
     */
    public static double skewRatio(long[] loads) {
        long max = validate(loads);
        long sum = 0;
        for (long load : loads) {
            sum += load;
        }
        double avg = (double) sum / loads.length;
        return max / avg;
    }

    /**
     * 重分片触发判定：偏斜比 ≥ threshold → 建议重分片。契约：
     * threshold ≥ 1（fail-fast）。
     */
    public static boolean needsReshard(long[] loads, double skewThreshold) {
        if (skewThreshold < 1.0) {
            throw new IllegalArgumentException(
                    "skewThreshold 不能小于 1：" + skewThreshold);
        }
        return skewRatio(loads) >= skewThreshold;
    }

    private static long validate(long[] loads) {
        if (loads == null || loads.length == 0) {
            throw new IllegalArgumentException("负载表不能为空");
        }
        long max = 0;
        long sum = 0;
        for (long load : loads) {
            if (load < 0) {
                throw new IllegalArgumentException("负载不能为负：" + load);
            }
            max = Math.max(max, load);
            sum += load;
        }
        if (sum == 0) {
            throw new IllegalArgumentException(
                    "负载全零——无流量谈偏斜无意义");
        }
        return max;
    }
}
