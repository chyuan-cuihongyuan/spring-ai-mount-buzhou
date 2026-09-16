package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.HashMap;
import java.util.Map;

/**
 * 并发组闸（spec 2028 / T3157 / impl 1579）——GitHub Actions
 * concurrency group 思想：同组同时至多一个在跑——新进入者两种裁决：
 * cancel-in-progress=true 则**取代**在跑者（旧者被取消——防同组堆积，
 * 只留最新）；false 则拒（在跑者优先）。complete 带属主栅栏（只有
 * 现属主能释放——乱序完成不误伤）。
 *
 * <p>synchronized 小临界区；确定性无时钟。
 */
public final class ConcurrencyGroupGate {

    /** 进入裁决三态。 */
    public enum GroupOutcome {
        /** 组空闲或重入——获得属主。 */
        GRANTED,
        /** cancel-in-progress 生效——取代旧属主（旧者已被取消，调用方停工）。 */
        SUPERSEDED,
        /** 组占用且不取代——拒（稍后再试）。 */
        BUSY_REJECTED
    }

    private record Running(String taskId) {
    }

    private final boolean cancelInProgress;
    private final Map<String, Running> owners = new HashMap<>();
    private long supersessions;
    private long busyRejections;
    private long completions;
    private long fencedCompletions;

    public ConcurrencyGroupGate(boolean cancelInProgress) {
        this.cancelInProgress = cancelInProgress;
    }

    /**
     * 进入组：空闲 → GRANTED（成为属主）；同 taskId 重入幂等 GRANTED；
     * 占用且 cancelInProgress → SUPERSEDED（**新者成为属主**，旧者被
     * 取消——supersessions 计数，旧者侧由调用方对账停止）；占用且不
     * 取代 → BUSY_REJECTED（busyRejections 计数）。
     */
    public synchronized GroupOutcome tryEnter(String group, String taskId) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("group 不能为空");
        }
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为 null");
        }
        Running owner = owners.get(group);
        if (owner == null) {
            owners.put(group, new Running(taskId));
            return GroupOutcome.GRANTED;
        }
        if (owner.taskId().equals(taskId)) {
            return GroupOutcome.GRANTED; // 重入幂等
        }
        if (cancelInProgress) {
            owners.put(group, new Running(taskId));
            supersessions++;
            return GroupOutcome.SUPERSEDED;
        }
        busyRejections++;
        return GroupOutcome.BUSY_REJECTED;
    }

    /**
     * 完成释放：仅现属主匹配才释放（栅栏——被取代者的迟到 complete
     * 不误伤新属主，fencedCompletions 计数显形）。
     */
    public synchronized boolean complete(String group, String taskId) {
        if (group == null || taskId == null) {
            throw new IllegalArgumentException("group/taskId 不能为 null");
        }
        Running owner = owners.get(group);
        if (owner == null) {
            return false; // 组已空闲
        }
        if (!owner.taskId().equals(taskId)) {
            fencedCompletions++; // 非属主的迟到完成——栅栏拦下
            return false;
        }
        owners.remove(group);
        completions++;
        return true;
    }

    /** 当前属主（组空闲 null——观测面）。 */
    public synchronized String ownerOf(String group) {
        if (group == null) {
            throw new IllegalArgumentException("group 不能为 null");
        }
        Running owner = owners.get(group);
        return owner == null ? null : owner.taskId();
    }

    /** 四计数快照：取代/占用拒/正常完成/栅栏拦下的迟到完成。 */
    public synchronized GroupStats stats() {
        return new GroupStats(supersessions, busyRejections, completions, fencedCompletions);
    }

    /** 并发组账快照。 */
    public record GroupStats(long supersessions, long busyRejections,
                             long completions, long fencedCompletions) {
    }
}
