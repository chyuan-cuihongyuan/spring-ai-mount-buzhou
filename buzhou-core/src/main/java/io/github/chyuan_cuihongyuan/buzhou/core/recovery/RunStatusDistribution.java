package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行状态分布与滞后审计（spec 1429 / T2159 / impl 1082）——Temporal
 * workflow stats（状态分布 + 进度滞后）思想：{@link RunRegistry} 的快照
 * 列表只有 list(status) 原子查询，巡检报表需要的两件事无读面——①状态
 * 分布（RUNNING 淤积多少）；②**turn 滞后**（currentTurn − lastCompletedTurn：
 * 崩溃时将丢失的未持久化轮数——恢复承诺「续跑点恒为 lastCompletedTurn
 * 之后」的暴露窗口量化）。
 *
 * <p>纯函数零状态：吃快照列表（巡检周期性调用）；worst offenders 榜
 * （滞后降序取前 N，会话 id 典序破平）指向具体风险会话。
 */
public final class RunStatusDistribution {

    /** worst offenders 榜容量。 */
    static final int WORST_CAPACITY = 3;

    private RunStatusDistribution() {
    }

    /**
     * @param sessionId 会话 id
     * @param turnLag   未持久化轮数 = currentTurn − lastCompletedTurn（崩溃暴露窗口）
     */
    public record TurnLag(String sessionId, int turnLag) {
    }

    /**
     * @param statusHistogram 状态直方（RUNNING/INTERRUPTED/COMPLETED；缺省状态计 0 不出现）
     * @param runningLagMax   RUNNING 快照的最大 turn 滞后（无 RUNNING = 0）
     * @param worstOffenders  滞后 Top（滞后降序会话 id 典序；容量 {@value #WORST_CAPACITY}）
     */
    public record Report(Map<RunStatus, Integer> statusHistogram,
                         int runningLagMax, List<TurnLag> worstOffenders) {
    }

    /** 审计入口：注册表快照列表（list(status) 结果的合并或全量枚举）。 */
    public static Report analyze(List<RunStateSnapshot> snapshots) {
        Map<RunStatus, Integer> histogram = new LinkedHashMap<>();
        for (RunStatus status : RunStatus.values()) {
            histogram.put(status, 0);
        }
        for (RunStateSnapshot s : snapshots) {
            if (s.status() != null) {
                histogram.merge(s.status(), 1, Integer::sum);
            }
        }
        List<TurnLag> lags = snapshots.stream()
                .map(s -> new TurnLag(s.sessionId(),
                        Math.max(0, s.currentTurn() - s.lastCompletedTurn())))
                .toList();
        int runningLagMax = lags.isEmpty() ? 0
                : lags.stream()
                        .filter(l -> isRunning(snapshots, l.sessionId()))
                        .mapToInt(TurnLag::turnLag).max().orElse(0);
        List<TurnLag> worst = lags.stream()
                .sorted(Comparator.comparingInt(TurnLag::turnLag).reversed()
                        .thenComparing(TurnLag::sessionId))
                .limit(WORST_CAPACITY)
                .filter(l -> l.turnLag() > 0)
                .toList();
        return new Report(java.util.Collections.unmodifiableMap(histogram),
                runningLagMax, List.copyOf(worst));
    }

    private static boolean isRunning(List<RunStateSnapshot> snapshots, String sessionId) {
        return snapshots.stream()
                .anyMatch(s -> s.sessionId().equals(sessionId)
                        && s.status() == RunStatus.RUNNING);
    }
}
