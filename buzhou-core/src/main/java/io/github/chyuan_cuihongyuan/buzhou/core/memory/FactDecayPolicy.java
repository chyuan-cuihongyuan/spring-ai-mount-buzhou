package io.github.chyuan_cuihongyuan.buzhou.core.memory;

import java.time.Duration;

/**
 * 事实置信度衰减策略（spec 604 / T858，letta memory blocks 置信度衰减借鉴；
 * 自 memory 模块移驻 core——与 DefaultFactStore 同址供 GuardModule 装配，spec 626）：
 * 指数半衰——{@code 衰减后置信度 = confidence × 2^(−elapsedTurns / halfLifeTurns)}，
 * 低于 {@link #floor()} 即停止注入（陈年低置信事实不再占提示词预算）。
 *
 * <p>仅时间维衰减；冲突驱动的置信下调（新矛盾事实压低旧事实）留雾区。
 *
 * <p>spec 707 / T965：自 {@code core.internal.memory} 迁出——跨模块复用类不入
 * internal（边界守卫 ModuleBoundaryGuardTest 口径）。
 *
 * @param halfLifeTurns 半衰期（轮次；每过一个半衰期置信度减半）
 * @param floor         注入下限 [0,1)：衰减后 &lt; floor 的事实被过滤
 */
public record FactDecayPolicy(double halfLifeTurns, double floor) {

    private static final double MAX_FLOOR_EXCLUSIVE = 1.0;

    public FactDecayPolicy {
        if (!(halfLifeTurns > 0)) {
            throw new IllegalArgumentException("halfLifeTurns 必须为正（当前 " + halfLifeTurns + "）");
        }
        if (!(floor >= 0 && floor < MAX_FLOOR_EXCLUSIVE)) {
            throw new IllegalArgumentException("floor 必须在 [0,1)（当前 " + floor + "）");
        }
    }

    /** 常用预设：半衰 8 轮、下限 0.25。 */
    public static FactDecayPolicy defaults() {
        return new FactDecayPolicy(8, 0.25);
    }

    /** 便捷构造（半衰用时长口径按轮折算，仅示意用途）。 */
    public static FactDecayPolicy ofHalfLife(Duration halfLife, Duration perTurn) {
        double turns = Math.max(1, halfLife.toMillis() / (double) Math.max(1, perTurn.toMillis()));
        return new FactDecayPolicy(turns, 0.25);
    }

    /** 衰减后置信度（elapsedTurns ≤ 0 返回原值）。 */
    public double decayed(double confidence, int elapsedTurns) {
        if (elapsedTurns <= 0) {
            return confidence;
        }
        return confidence * Math.pow(2, -elapsedTurns / halfLifeTurns);
    }

    /** 是否仍值得注入。 */
    public boolean injectable(double confidence, int elapsedTurns) {
        return decayed(confidence, elapsedTurns) >= floor;
    }
}
