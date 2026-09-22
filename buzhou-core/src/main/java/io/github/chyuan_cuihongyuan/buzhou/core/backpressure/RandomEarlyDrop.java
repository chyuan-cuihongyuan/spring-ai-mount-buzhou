package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 随机早期丢弃（spec 1910 / T3021 / impl 1511）——RED 经典 AQM：
 * 队列逼近满位前按概率提前丢。minTh 以下不丢、maxTh 以上按 maxP
 * 丢、两线之间线性爬升——把「尾丢」的成批同步突丢摊成均匀提前丢，
 * 打破全局同步。
 *
 * <p>纯函数零状态；真实丢包掷骰归调用方（本面只给概率与分区）。
 */
public final class RandomEarlyDrop {

    private RandomEarlyDrop() {
    }

    /** 队列分区：BELOW 丢弃线以下 / LINEAR 线性概率区 / ABOVE 已达 maxP 区。 */
    public enum Zone { BELOW, LINEAR, ABOVE }

    /**
     * 丢弃概率：minTh 以下 0；minTh–maxTh 之间线性升到 maxP；
     * maxTh 以上恒 maxP。契约：0 ≤ minTh < maxTh、maxP ∈ (0,1]、
     * queueSize ≥ 0（fail-fast）。
     */
    public static double dropProbability(int queueSize, int minTh, int maxTh,
                                         double maxP) {
        validate(minTh, maxTh, maxP);
        if (queueSize < 0) {
            throw new IllegalArgumentException("queueSize 不能为负：" + queueSize);
        }
        if (queueSize <= minTh) {
            return 0.0;
        }
        if (queueSize >= maxTh) {
            return maxP;
        }
        return maxP * (queueSize - minTh) / (double) (maxTh - minTh);
    }

    /**
     * 队列分区读数：BELOW（安全）/ LINEAR（预警——概率爬升中）/
     * ABOVE（顶格）。边界含下：恰 minTh 进入 LINEAR、恰 maxTh 进入
     * ABOVE。
     */
    public static Zone zone(int queueSize, int minTh, int maxTh) {
        if (minTh < 0 || maxTh <= minTh) {
            throw new IllegalArgumentException(String.format(
                    "须满足 0 ≤ minTh < maxTh：%d, %d", minTh, maxTh));
        }
        if (queueSize < 0) {
            throw new IllegalArgumentException("queueSize 不能为负：" + queueSize);
        }
        if (queueSize <= minTh) {
            return Zone.BELOW;
        }
        if (queueSize < maxTh) {
            return Zone.LINEAR;
        }
        return Zone.ABOVE;
    }

    private static void validate(int minTh, int maxTh, double maxP) {
        if (minTh < 0 || maxTh <= minTh) {
            throw new IllegalArgumentException(String.format(
                    "须满足 0 ≤ minTh < maxTh：%d, %d", minTh, maxTh));
        }
        if (maxP <= 0.0 || maxP > 1.0) {
            throw new IllegalArgumentException(
                    "maxP 须在 (0,1]：" + maxP);
        }
    }
}
