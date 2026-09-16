package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 抢占重算账本（spec 2012 / T3125 / impl 1563）——vLLM preemption /
 * recompute 思想：抢占决策不是免费的——victim 已做工作作废（重算
 * 浪费），换来高优先级任务提前完成（等待节省）。本件记每笔抢占的
 * 双面账并出净收益/浪费率——「抢占在赚还是在赔」可对账（净收益
 * 持续为负即该降抢占阈值）。
 *
 * <p>synchronized 小临界区；纯计数零背景线程。
 */
public final class PreemptionLedger {

    private long preemptions;
    private long recomputations;
    private long wastedWorkTicks;
    private long savedTicks;
    private final Map<String, Long> wastedByVictim = new HashMap<>();
    private final Set<String> recomputed = new HashSet<>();

    /**
     * 记一次抢占：victimId 已做 workTicks 作废（浪费面），抢占方因
     * 提前执行节省 savedTicks（收益面）。契约：workTicks/savedTicks
     * ≥ 0、victimId 非空（fail-fast）。
     */
    public synchronized void recordPreemption(String victimId, long workTicks, long savedTicks) {
        if (victimId == null) {
            throw new IllegalArgumentException("victimId 不能为 null");
        }
        if (workTicks < 0 || savedTicks < 0) {
            throw new IllegalArgumentException("ticks 须 ≥ 0：work=" + workTicks + " saved=" + savedTicks);
        }
        preemptions++;
        wastedWorkTicks += workTicks;
        this.savedTicks += savedTicks;
        wastedByVictim.merge(victimId, workTicks, Long::sum);
    }

    /** 记 victim 重算完成（幂等——同一 victim 只计一次）。 */
    public synchronized void recordRecomputation(String victimId) {
        if (victimId == null) {
            throw new IllegalArgumentException("victimId 不能为 null");
        }
        if (recomputed.add(victimId)) {
            recomputations++;
        }
    }

    /** 净收益 = 节省 − 浪费（负数即抢占在赔——降阈值信号）。 */
    public synchronized long netBenefitTicks() {
        return savedTicks - wastedWorkTicks;
    }

    /**
     * 浪费率 = 浪费 ÷（浪费+节省）∈ [0,1]；零账（无抢占）为 0——
     * 空账不除零。
     */
    public synchronized double wasteRatio() {
        long total = wastedWorkTicks + savedTicks;
        return total == 0 ? 0.0d : (double) wastedWorkTicks / total;
    }

    /** 重算发生率 = 重算 victim 数 ÷ 抢占数（0 即抢占的 victim 全被放弃/由他处兜底）。 */
    public synchronized double recomputeRate() {
        return preemptions == 0 ? 0.0d : (double) recomputations / preemptions;
    }

    /** 账面快照：抢占数/重算数/浪费/节省/净收益。 */
    public synchronized PreemptionStats stats() {
        return new PreemptionStats(preemptions, recomputations, wastedWorkTicks,
                savedTicks, netBenefitTicks());
    }

    /** 抢占账快照。 */
    public record PreemptionStats(long preemptions, long recomputations,
                                  long wastedWorkTicks, long savedTicks,
                                  long netBenefitTicks) {
    }
}
