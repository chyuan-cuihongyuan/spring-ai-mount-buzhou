package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.math.BigInteger;

/**
 * 难度目标重定（spec 4031 / T6063 / impl 2132）——周期性有界重
 * 定思想（Bitcoin difficulty retarget，2016 块窗 ±4× 钳制）：
 * 每窗 {@code windowBlocks} 个间隔按窗内实际耗时 vs 目标耗时
 * 重定一次目标——实际耗时钳到 [target/4, target×4]，单窗调整
 * 幅度天然 ≤ 4×（不尖叫）；{@code powLimit} 封顶（目标上限 =
 * 难度地板）；重定块自身开启新窗（Bitcoin 同语义），持续运行。
 *
 * <p>时间戳要求非递减（倒流 fail-fast——确定性）；BigInteger
 * 精确运算（截断除法与正域 floor 同口径）。
 *
 * <p>与 {@link Eip1559BaseFee} 同族不同面：费用市场每轮小幅
 * 软调节 vs 难度周期窗钳制硬重定。
 */
public final class DifficultyRetarget {

    /** 单窗最大调整倍数（Bitcoin retarget 钳制因子）。 */
    private static final BigInteger MAX_ADJUST_FACTOR = BigInteger.valueOf(4L);

    /** 目标下限（重定不产出非正目标）。 */
    private static final BigInteger MIN_TARGET = BigInteger.ONE;

    private final long targetSpacing;
    private final int windowBlocks;
    private final BigInteger powLimit;
    private final BigInteger targetTimespan;
    private BigInteger currentTarget;
    private long windowStart = -1L;
    private long lastBlockTime = -1L;
    private int spacingsInWindow;

    /**
     * 定构（spacing>0、windowBlocks>0、powLimit≥initial>0 否则 fail-fast）。
     *
     * @param targetSpacing 每块目标间隔（毫秒级时间单位自定）
     * @param windowBlocks 每窗间隔数（2016 思想）
     * @param powLimit 目标上限（难度地板）
     * @param initialTarget 初始难度目标
     */
    public DifficultyRetarget(long targetSpacing, int windowBlocks, long powLimit, long initialTarget) {
        if (targetSpacing <= 0 || windowBlocks <= 0 || powLimit <= 0 || initialTarget <= 0
                || initialTarget > powLimit) {
            throw new IllegalArgumentException("spacing>0 / windowBlocks>0 / 0<initial≤powLimit："
                    + targetSpacing + "/" + windowBlocks + "/" + initialTarget + "/" + powLimit);
        }
        this.targetSpacing = targetSpacing;
        this.windowBlocks = windowBlocks;
        this.powLimit = BigInteger.valueOf(powLimit);
        this.targetTimespan = BigInteger.valueOf(targetSpacing)
                .multiply(BigInteger.valueOf(windowBlocks));
        this.currentTarget = BigInteger.valueOf(initialTarget);
    }

    /**
     * 记账块到达；窗满即重定并滚动新窗。
     *
     * @param time 块时间戳（非递减，倒流 fail-fast）
     * @return 当前难度目标
     */
    public long block(long time) {
        if (time < lastBlockTime) {
            throw new IllegalArgumentException("时间倒流：" + time + " < " + lastBlockTime);
        }
        if (windowStart < 0) {
            windowStart = time;
            lastBlockTime = time;
            return currentTarget.longValueExact();
        }
        spacingsInWindow++;
        long firstOfWindow = windowStart;
        lastBlockTime = time;
        if (spacingsInWindow >= windowBlocks) {
            retarget(time - firstOfWindow);
            windowStart = time;   // 重定块开启新窗
            spacingsInWindow = 0;
        }
        return currentTarget.longValueExact();
    }

    /** 当前难度目标读数。 */
    public long currentTarget() {
        return currentTarget.longValueExact();
    }

    /** 距下次重定的间隔数读数。 */
    public int blocksToRetarget() {
        return windowBlocks - spacingsInWindow;
    }

    /** 每窗间隔数读数。 */
    public int windowBlocks() {
        return windowBlocks;
    }

    /** 目标每窗耗时读数（spacing × windowBlocks）。 */
    public long targetTimespan() {
        return targetTimespan.longValueExact();
    }

    /** 重定核心：实际耗时钳 [target/4, ×4] 后比例缩放，powLimit 封顶。 */
    private void retarget(long actualTimespan) {
        BigInteger actual = BigInteger.valueOf(actualTimespan);
        BigInteger low = targetTimespan.divide(MAX_ADJUST_FACTOR);
        BigInteger high = targetTimespan.multiply(MAX_ADJUST_FACTOR);
        if (actual.compareTo(low) < 0) {
            actual = low;
        }
        if (actual.compareTo(high) > 0) {
            actual = high;
        }
        BigInteger next = currentTarget.multiply(actual).divide(targetTimespan);
        if (next.compareTo(powLimit) > 0) {
            next = powLimit;
        }
        if (next.compareTo(MIN_TARGET) < 0) {
            next = MIN_TARGET;
        }
        currentTarget = next;
    }
}
