package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具合并节省读面（L 会话 1700 系 R15 = effort #1714 / spec 1714 /
 * 票 T2629 + T2630 / impl 1314）——Go singleflight / groupcache 的合并
 * 回喂遥测思想：{@link ToolCallCoalescer} 把同参并发调用合并成一次真实
 * 执行——「省了多少」须有账：合并组数/省去的调用数/节省时延/节省比。
 *
 * <p>实例面线程安全：`recordGroup(members)` 记一个合并组（members = 组内
 * 调用数，省去 members−1 次真实执行）；`recordLatencySaved(millis)` 累计
 * 省去的等待时延。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class ToolCoalesceStats {

    private final AtomicLong groups = new AtomicLong();
    private final AtomicLong callsJoined = new AtomicLong();
    private final AtomicLong savedCalls = new AtomicLong();
    private final AtomicLong latencySavedMillis = new AtomicLong();

    /** 记录一个合并组（members≥2 才算合并；1 = 未合并不记账）。 */
    public void recordGroup(int members) {
        if (members < 2) {
            return;
        }
        groups.incrementAndGet();
        callsJoined.addAndGet(members);
        savedCalls.addAndGet(members - 1L);
    }

    /** 累计节省时延（毫秒；负值忽略）。 */
    public void recordLatencySaved(long millis) {
        if (millis > 0) {
            latencySavedMillis.addAndGet(millis);
        }
    }

    /**
     * @param groups              合并组数
     * @param callsJoined         参与合并的调用总数
     * @param savedCalls          省去的真实执行数（Σ members−1）
     * @param latencySavedMillis  累计节省时延
     * @param savingRatio         节省比 savedCalls/callsJoined；无合并哨兵 −1
     */
    public record CoalesceSavings(long groups, long callsJoined, long savedCalls,
                                  long latencySavedMillis, double savingRatio) {
    }

    /** 快照。 */
    public CoalesceSavings snapshot() {
        long joined = callsJoined.get();
        double ratio = joined == 0 ? -1d : (double) savedCalls.get() / joined;
        return new CoalesceSavings(groups.get(), joined, savedCalls.get(),
                latencySavedMillis.get(), ratio);
    }

    /** 测试归零。 */
    public void resetForTest() {
        groups.set(0);
        callsJoined.set(0);
        savedCalls.set(0);
        latencySavedMillis.set(0);
    }
}
