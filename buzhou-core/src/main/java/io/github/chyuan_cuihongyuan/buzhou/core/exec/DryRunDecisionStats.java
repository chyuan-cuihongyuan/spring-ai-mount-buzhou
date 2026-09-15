package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * dry-run 决策分布（L 会话 1700 系 R21 = effort #1720 / spec 1720 /
 * 票 T2641 + T2642 / impl 1320）——Terraform plan 的决策分布思想：
 * {@link DryRunHook} 给出「真跑会怎样」的预演，但预演结论的分布
 * （放行/拦下/预演自身出错）无读数——「拦了多少」是 dry-run 价值的
 * 直接量度。
 *
 * <p>实例面线程安全：`Decision` 闭集（WOULD_RUN 会放行 / WOULD_BLOCK
 * 会拦截 / PLAN_ERROR 预演自身出错）+`record`+`census`+拦截占比
 * （无样本 −1）+resetForTest。纯读面 opt-in，不改 dry-run 判定。
 *
 * @since 1.0.0
 */
public final class DryRunDecisionStats {

    /** dry-run 预演决策闭集。 */
    public enum Decision { WOULD_RUN, WOULD_BLOCK, PLAN_ERROR }

    private final Map<Decision, AtomicLong> counters = new EnumMap<>(Decision.class);

    /** 默认构造。 */
    public DryRunDecisionStats() {
        for (Decision decision : Decision.values()) {
            counters.put(decision, new AtomicLong());
        }
    }

    /** 记一次预演结论。 */
    public void record(Decision decision) {
        counters.get(decision).incrementAndGet();
    }

    /**
     * @param planned      预演总数
     * @param wouldRun     会放行数
     * @param wouldBlock   会拦截数
     * @param planErrors   预演自身出错数
     * @param blockRatio   拦截占比 wouldBlock/planned；无样本哨兵 −1
     */
    public record PlanCensus(long planned, long wouldRun, long wouldBlock,
                             long planErrors, double blockRatio) {
    }

    /** 快照。 */
    public PlanCensus census() {
        long run = counters.get(Decision.WOULD_RUN).get();
        long block = counters.get(Decision.WOULD_BLOCK).get();
        long errors = counters.get(Decision.PLAN_ERROR).get();
        long planned = run + block + errors;
        double ratio = planned == 0 ? -1d : (double) block / planned;
        return new PlanCensus(planned, run, block, errors, ratio);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}
