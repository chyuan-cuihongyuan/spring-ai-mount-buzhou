package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 死信重投成功率读数（spec 846 / T1193，sidekiq retry set 扩散——847 死信
 * 台账的姊妹面）：死信重投尝试与成功计数+成功率+自上次成功以来的连续失败
 * streak——「重投是在恢复还是持续失败」量化。
 *
 * <p>纯记账（原子计数）；record 由重投路径装配侧喂；负值/无效枚举忽略。
 * 成功后 streak 归零；重投语义（何时重投/投几次）归调用方。
 */
public final class DeadLetterRedeliveryStats {

    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong consecutiveFailures = new AtomicLong();

    /** 记录一次重投结果。 */
    public void record(boolean success) {
        attempts.incrementAndGet();
        if (success) {
            successes.incrementAndGet();
            consecutiveFailures.set(0);
        } else {
            consecutiveFailures.incrementAndGet();
        }
    }

    /** 成功率（无尝试 = 0）。 */
    public double successRate() {
        long total = attempts.get();
        return total == 0 ? 0 : (double) successes.get() / total;
    }

    /** 当前连续失败 streak。 */
    public long consecutiveFailures() {
        return consecutiveFailures.get();
    }

    public long attempts() {
        return attempts.get();
    }

    public long successes() {
        return successes.get();
    }
}
