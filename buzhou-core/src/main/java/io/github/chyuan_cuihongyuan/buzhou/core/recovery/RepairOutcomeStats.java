package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 悬挂修复动作结果普查（L 会话 1700 系 R24 = effort #1723 / spec 1723 /
 * 票 T2647 + T2648 / impl 1323）——Kubernetes events 的自愈动作审计思想：
 * {@link DanglingCallRepairer} 修悬挂调用，但修复动作做了什么选择
 * （重放/标记失败/目标已消失跳过）无普查——「自愈动作分布」是恢复域
 * 的健康画像。
 *
 * <p>实例面线程安全：`Action` 闭集（REPLAYED 重放成功 / MARKED_FAILED
 * 标记失败终止 / SKIPPED_GONE 目标已消失跳过）+`record`+`census`
 * （动作占比与哨兵）+`resetForTest`。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class RepairOutcomeStats {

    /** 修复动作闭集。 */
    public enum Action { REPLAYED, MARKED_FAILED, SKIPPED_GONE }

    private final Map<Action, AtomicLong> counters = new EnumMap<>(Action.class);

    /** 默认构造。 */
    public RepairOutcomeStats() {
        for (Action action : Action.values()) {
            counters.put(action, new AtomicLong());
        }
    }

    /** 记一次修复动作。 */
    public void record(Action action) {
        counters.get(action).incrementAndGet();
    }

    /**
     * @param total        动作总数
     * @param replayed     重放数
     * @param markedFailed 标记失败数
     * @param skippedGone  跳过数
     * @param replayRatio  重放占比 replayed/total；无样本哨兵 −1
     */
    public record RepairCensus(long total, long replayed, long markedFailed,
                               long skippedGone, double replayRatio) {
    }

    /** 快照。 */
    public RepairCensus census() {
        long replayed = counters.get(Action.REPLAYED).get();
        long failed = counters.get(Action.MARKED_FAILED).get();
        long gone = counters.get(Action.SKIPPED_GONE).get();
        long total = replayed + failed + gone;
        double ratio = total == 0 ? -1d : (double) replayed / total;
        return new RepairCensus(total, replayed, failed, gone, ratio);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}
