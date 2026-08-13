package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.time.Instant;

/**
 * Run 快照（wayfinder2 impl-06 / T32）：以 <b>Completed-Turn</b> 为快照单元——
 * {@code lastCompletedTurn} 即崩溃恢复的续跑点（其后内容经悬空修复/事件日志回放重建）。
 *
 * @param sessionId          会话 id（= run id）
 * @param appId              应用 id（恢复服务重新 spawn 所需）
 * @param agentName          agent 名
 * @param status             run 状态
 * @param currentTurn        当前在途轮次（turn 开始时写入）
 * @param lastCompletedTurn  最后一个已完结轮次（= 快照边界；afterTurn 时推进）
 * @param ownerId            持有者（运行时实例 id）
 * @param updatedAt          最近更新时间
 */
public record RunStateSnapshot(
        String sessionId,
        String appId,
        String agentName,
        RunStatus status,
        int currentTurn,
        int lastCompletedTurn,
        String ownerId,
        Instant updatedAt) {

    public RunStateSnapshot {
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    /** 派生：推进到新的在途轮次（保留既有 lastCompletedTurn）。 */
    public RunStateSnapshot startingTurn(int turn, String owner) {
        return new RunStateSnapshot(sessionId, appId, agentName, RunStatus.RUNNING,
                turn, lastCompletedTurn, owner, Instant.now());
    }

    /** 派生：完结一轮（快照边界推进）。 */
    public RunStateSnapshot completingTurn(int turn, String owner) {
        return new RunStateSnapshot(sessionId, appId, agentName, status,
                turn, Math.max(lastCompletedTurn, turn), owner, Instant.now());
    }

    /** 派生：变更状态（如 COMPLETED / INTERRUPTED）。 */
    public RunStateSnapshot withStatus(RunStatus newStatus) {
        return new RunStateSnapshot(sessionId, appId, agentName, newStatus,
                currentTurn, lastCompletedTurn, ownerId, Instant.now());
    }
}
