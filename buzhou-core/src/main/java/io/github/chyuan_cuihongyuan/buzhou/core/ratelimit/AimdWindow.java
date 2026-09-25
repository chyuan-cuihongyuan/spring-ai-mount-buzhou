package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

/**
 * AIMD Window 加性增/乘性减窗口（spec 6041 / T6281 / impl 2241）——
 * TCP 拥塞避免思想：**成功加性增（+1）、失败乘性减（×β）**
 * 的自适应限额窗口——固定阈值限流（无拥塞信号利用）与纯
 * 加性调节（收敛慢）的病解；乘性减快速退避释放压力，加性增
 * 缓慢探测余量（锯齿收敛——稳定性经典）。钳制于 [min,max]
 * （语义边界 fail-fast：min>max、β∉(0,1)）。
 *
 * <p>与 GradientAdaptiveLimiter（concurrent）同族不同面：
 * 梯度延迟信号 vs 成败二值 AIMD 锯齿；与 RetryBudget 不同
 * 面：重试预算 vs 并发窗口。
 */
public final class AimdWindow {

    private final long minLimit;
    private final long maxLimit;
    private final double decreaseFactor;
    private long current;

    /** 建窗（min≤max、β∈(0,1) fail-fast；初始=min）。 */
    public AimdWindow(long minLimit, long maxLimit, double decreaseFactor) {
        if (minLimit > maxLimit) {
            throw new IllegalArgumentException("min 不可大于 max: " + minLimit + ">" + maxLimit);
        }
        if (minLimit < 1) {
            throw new IllegalArgumentException("min 必须≥1: " + minLimit);
        }
        if (decreaseFactor <= 0 || decreaseFactor >= 1) {
            throw new IllegalArgumentException("衰减因子须在 (0,1): " + decreaseFactor);
        }
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
        this.decreaseFactor = decreaseFactor;
        this.current = minLimit;
    }

    /** 成功：加性增 +1（钳 max）。 */
    public void onSuccess() {
        current = Math.min(maxLimit, current + 1);
    }

    /** 失败：乘性减（钳 min；向上取整防归零）。 */
    public void onFailure() {
        current = Math.max(minLimit, (long) Math.ceil(current * decreaseFactor));
    }

    /** 当前窗口读数。 */
    public long current() {
        return current;
    }

    /** 下限读数。 */
    public long minLimit() {
        return minLimit;
    }

    /** 上限读数。 */
    public long maxLimit() {
        return maxLimit;
    }

    /** 衰减因子读数。 */
    public double decreaseFactor() {
        return decreaseFactor;
    }
}
