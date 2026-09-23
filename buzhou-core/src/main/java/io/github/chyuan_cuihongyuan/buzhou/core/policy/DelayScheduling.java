package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * 延迟调度（spec 5014 / T6129 / impl 2165）——数据本地性等待
 * 预算思想（Spark delay scheduling / locality wait）：每轮判定
 * 首选本地槽是否可用——可用即 `LAUNCH(preferred)`（零浪费）；
 * 不可用 skips++ 继续 WAIT；skips 超 `maxSkips` 预算 →
 * `LAUNCH(ANY)`（本地性换时效）并重置计数。立即任意执行
 * （远程拉数据带宽爆炸）与无限等本地槽（任务饿死）的病解。
 *
 * <p>降级梯 PROCESS_LOCAL→NODE_LOCAL→RACK_LOCAL→ANY（等待中
 * 逐轮放宽）；确定性轮次判定（无时间依赖）。与
 * SpeculativeStragglerPolicy（R27 副本竞争）互补。
 */
public final class DelayScheduling {

    /** 本地性级别（越靠前数据越近）。 */
    public enum Locality {
        PROCESS_LOCAL, NODE_LOCAL, RACK_LOCAL, ANY
    }

    /**
     * 调度裁决。
     *
     * @param launch 本轮是否启动
     * @param level 启动时的本地性级别（WAIT 时为 null）
     * @param skipsUsed 已消耗的等待轮数
     */
    public record Verdict(boolean launch, Locality level, int skipsUsed) {

        static Verdict wait(int skipsUsed) {
            return new Verdict(false, null, skipsUsed);
        }
    }

    private final int maxSkips;
    private int skipsUsed;
    private Locality relaxedTo = Locality.PROCESS_LOCAL;

    /** 定构（maxSkips ≥0 否则 fail-fast；0 = 不等本地立即 ANY）。 */
    public DelayScheduling(int maxSkips) {
        if (maxSkips < 0) {
            throw new IllegalArgumentException("maxSkips ≥0：" + maxSkips);
        }
        this.maxSkips = maxSkips;
    }

    /**
     * 每轮调度判定。
     *
     * @param preferredAvailable 首选本地槽是否可用
     * @return 裁决（WAIT 或 LAUNCH+级别）
     */
    public Verdict offer(boolean preferredAvailable) {
        if (preferredAvailable) {
            skipsUsed = 0;
            relaxedTo = Locality.PROCESS_LOCAL;
            return new Verdict(true, Locality.PROCESS_LOCAL, 0);
        }
        if (skipsUsed >= maxSkips) {
            skipsUsed = 0;
            relaxedTo = Locality.PROCESS_LOCAL;
            return new Verdict(true, Locality.ANY, 0);   // 本地性换时效
        }
        skipsUsed++;
        relaxedTo = nextLevel(relaxedTo);
        return Verdict.wait(skipsUsed);
    }

    /** 当前放宽到的级别读数。 */
    public Locality relaxedTo() {
        return relaxedTo;
    }

    /** 已消耗等待轮数读数。 */
    public int skipsUsed() {
        return skipsUsed;
    }

    private static Locality nextLevel(Locality current) {
        return switch (current) {
            case PROCESS_LOCAL -> Locality.NODE_LOCAL;
            case NODE_LOCAL -> Locality.RACK_LOCAL;
            default -> Locality.ANY;
        };
    }
}
