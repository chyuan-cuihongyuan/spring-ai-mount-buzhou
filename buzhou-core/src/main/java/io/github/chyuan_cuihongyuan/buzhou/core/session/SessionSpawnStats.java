package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话 spawn 统计读面（spec 1432 / T2165 / impl 1083）——HikariCP 连接池
 * 建连统计思想（attempts/collisions 是池治理第一读数）：spawn 冲突
 * （{@link SessionAlreadyActiveException}——同 id 已被租约持有）与 steal
 * 抢占（{@code SpawnOptions.steal()}）的频次无读面——「spawn 失败率多少、
 * 抢占多频繁」是会话 id 规划与冲突治理的依据。
 *
 * <p>进程级静态读面（ToolArgsValidator.validationStats 同款先例——埋点在
 * internal runtime，读面归公共类）；{@code activePeak} 为 spawn 时点观测的
 * 活跃会话数峰值（口径显式：仅 spawn 路径采样，非全时段）。守恒式：
 * {@code attempts = successes + collisions}（steal 成功计入 successes，
 * 语义上是 spawn 的一种完成形态）。{@link #resetForTest()} 归零注入点。
 */
public final class SessionSpawnStats {

    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong SUCCESSES = new AtomicLong();
    private static final AtomicLong COLLISIONS = new AtomicLong();
    private static final AtomicLong STEALS = new AtomicLong();
    private static final AtomicInteger ACTIVE_PEAK = new AtomicInteger();

    private SessionSpawnStats() {
    }

    public static void recordAttempt() {
        ATTEMPTS.incrementAndGet();
    }

    public static void recordCollision() {
        COLLISIONS.incrementAndGet();
    }

    public static void recordSteal() {
        STEALS.incrementAndGet();
    }

    public static void recordSuccess(int activeAtSpawn) {
        SUCCESSES.incrementAndGet();
        ACTIVE_PEAK.accumulateAndGet(activeAtSpawn, Math::max);
    }

    /** 只读快照：总量守恒 + 活跃峰值水位。 */
    public static Snapshot stats() {
        return new Snapshot(ATTEMPTS.get(), SUCCESSES.get(), COLLISIONS.get(),
                STEALS.get(), ACTIVE_PEAK.get());
    }

    /** 测试归零口：静态读数的 reset 注入点。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        SUCCESSES.set(0);
        COLLISIONS.set(0);
        STEALS.set(0);
        ACTIVE_PEAK.set(0);
    }

    /**
     * @param attempts    累计 spawn 尝试数
     * @param successes   成功 spawn 数（含 steal 成功）
     * @param collisions  同 id 冲突拒绝数（SessionAlreadyActiveException）
     * @param steals      steal 抢占路径执行数
     * @param activePeak  spawn 时点观测的活跃会话峰值水位（口径：仅 spawn 路径采样）
     */
    public record Snapshot(long attempts, long successes, long collisions,
                           long steals, int activePeak) {
    }
}
