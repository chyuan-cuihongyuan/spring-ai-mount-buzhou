package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 协作预算让出（spec 4020 / T6041 / impl 2121）——异步运行时协作
 * 调度思想（Tokio coop budget）：任务每次被调度获得固定 N 点预算，
 * 关键路径操作（poll/IO 事件）逐次扣减，**预算耗尽即主动让出**
 （重新排队换取新预算）——单任务长循环不让出饿死同 worker 其他
 * 任务的病从运行时根除；让出点由关键操作自然分布（无需业务代码
 * 自觉 Thread.sleep/yield）。
 *
 * <p>本件为纯预算账（真调度归运行时）：poll 扣 1、预算尽
 * hasBudget()=false、yield() 重置满额、预算读数可审计。与
 * SpawnGate（准入）互补：彼管「进不进」，本管「进了之后霸不霸」。
 */
public final class CoopBudget {

    private final int initial;
    private int remaining;

    /** 定构（budget≥1 否则 fail-fast）。 */
    public CoopBudget(int budget) {
        if (budget < 1) {
            throw new IllegalArgumentException("budget≥1：" + budget);
        }
        this.initial = budget;
        this.remaining = budget;
    }

    /** 关键操作扣减（预算尽再扣 fail-fast——让出检查归调用方）。 */
    public void charge() {
        if (remaining <= 0) {
            throw new IllegalStateException("预算已尽——应先让出（yield）");
        }
        remaining--;
    }

    /** 预算余量判定（false 即让出时机）。 */
    public boolean hasBudget() {
        return remaining > 0;
    }

    /** 主动让出——预算重置满额（任务重新排队后继续）。 */
    public void yield() {
        remaining = initial;
    }

    /** 剩余预算读数。 */
    public int remaining() {
        return remaining;
    }

    /** 初始预算读数。 */
    public int initial() {
        return initial;
    }
}
