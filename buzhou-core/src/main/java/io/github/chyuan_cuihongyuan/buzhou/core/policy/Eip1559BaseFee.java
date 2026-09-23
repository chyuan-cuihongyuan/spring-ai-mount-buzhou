package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.math.BigInteger;

/**
 * EIP-1559 基础费调节（spec 4030 / T6061 / impl 2131）——弹性费用
 * 市场思想（EIP-1559 / go-ethereum calcBaseFee）：每轮按用量
 * vs 目标调节基础费——满块（2×target）涨、空块跌、恰目标不变；
 * 弹性钳制：gas limit = 2×target 机器性约束下单步幅度天然
 * ≤ ±1/8——有界不尖叫；跌到地板 {@code minBaseFee} 止跌。
 *
 * <p>整数精确运算（{@link BigInteger} + floor division）与
 * go-ethereum Euclidean division 同口径——下跌方向不舍入回零；
 * 超弹性（用量 > 2×target）fail-fast——机器性约束由调用方
 * gas limit 保证，越界即违约上抛不静默钳制。
 *
 * <p>与 ModelRateLimiter（令牌桶硬闸门）互补：费用市场软调节
 * vs 速率硬闸。
 */
public final class Eip1559BaseFee {

    /** 单步最大变化分母（EIP-1559 BASE_FEE_MAX_CHANGE_DENOMINATOR）。 */
    private static final BigInteger MAX_CHANGE_DENOMINATOR = BigInteger.valueOf(8L);

    /** 块上限相对目标的弹性倍数（EIP-1559 ELASTICITY_MULTIPLIER）。 */
    private static final BigInteger ELASTICITY_MULTIPLIER = BigInteger.valueOf(2L);

    private final BigInteger targetGas;
    private final BigInteger minBaseFee;
    private BigInteger currentBaseFee;
    private long lastDelta;

    /** 定构（target>0、min≥0、initial≥min 否则 fail-fast）。 */
    public Eip1559BaseFee(long targetGas, long initialBaseFee, long minBaseFee) {
        if (targetGas <= 0 || minBaseFee < 0 || initialBaseFee < minBaseFee) {
            throw new IllegalArgumentException(
                    "target>0 / min≥0 / initial≥min：" + targetGas + "/" + initialBaseFee + "/" + minBaseFee);
        }
        this.targetGas = BigInteger.valueOf(targetGas);
        this.minBaseFee = BigInteger.valueOf(minBaseFee);
        this.currentBaseFee = BigInteger.valueOf(initialBaseFee);
        this.lastDelta = 0L;
    }

    /**
     * 记账一轮用量并推进基础费（满块涨、空块跌、恰目标不变）。
     *
     * @param gasUsed 本轮用量（[0, 2×target]，越界 fail-fast）
     * @return 调整后基础费
     */
    public long advance(long gasUsed) {
        BigInteger used = BigInteger.valueOf(gasUsed);
        if (gasUsed < 0 || used.compareTo(targetGas.multiply(ELASTICITY_MULTIPLIER)) > 0) {
            throw new IllegalArgumentException("用量需在 [0, 2×target]：" + gasUsed
                    + "（target=" + targetGas + "——机器性约束由调用方 gas limit 保证）");
        }
        BigInteger gasDelta = used.subtract(targetGas);
        BigInteger feeDelta = floorDiv(floorDiv(currentBaseFee.multiply(gasDelta), targetGas),
                MAX_CHANGE_DENOMINATOR);
        BigInteger previous = currentBaseFee;
        BigInteger next = currentBaseFee.add(feeDelta);
        if (next.compareTo(minBaseFee) < 0) {
            next = minBaseFee;
        }
        currentBaseFee = next;
        lastDelta = next.subtract(previous).longValueExact();
        return currentBaseFee.longValueExact();
    }

    /** 当前基础费读数。 */
    public long currentBaseFee() {
        return currentBaseFee.longValueExact();
    }

    /** 最近一次调整量读数（带号——涨正跌负）。 */
    public long lastDelta() {
        return lastDelta;
    }

    /** 目标用量读数。 */
    public long targetGas() {
        return targetGas.longValueExact();
    }

    /** 费用地板读数。 */
    public long minBaseFee() {
        return minBaseFee.longValueExact();
    }

    /** floor division（denominator 恒正；负被除数向 −∞ 舍——Euclidean 口径）。 */
    private static BigInteger floorDiv(BigInteger numerator, BigInteger denominator) {
        BigInteger[] quotientRemainder = numerator.divideAndRemainder(denominator);
        if (quotientRemainder[1].signum() != 0 && numerator.signum() < 0) {
            return quotientRemainder[0].subtract(BigInteger.ONE);
        }
        return quotientRemainder[0];
    }
}
