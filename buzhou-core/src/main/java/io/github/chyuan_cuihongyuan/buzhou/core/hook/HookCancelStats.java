package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 钩子取消面统计（L 会话 1700 系 R19 = effort #1718 / spec 1718 /
 * 票 T2637 + T2638 / impl 1318）——OpenTelemetry exporter 的取消路径遥测
 * 思想：{@link HookChain} 在轮次被取消时的短路（跳过未执行钩子）是无声的
 * ——「钩子没跑是因为取消」还是「钩子压根没注册」，取消占比给出答案。
 *
 * <p>实例面线程安全：`record(boolean ran)` 逐次记账（ran=false = 因取消
 * 被跳过）+`snapshot()` 吐 observed/cancelledSkipped/completed/取消占比
 * （无样本哨兵 −1）+resetForTest。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class HookCancelStats {

    private final AtomicLong observed = new AtomicLong();
    private final AtomicLong cancelledSkipped = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();

    /** 记一次钩子调用机会（ran=false = 因取消被跳过）。 */
    public void record(boolean ran) {
        observed.incrementAndGet();
        (ran ? completed : cancelledSkipped).incrementAndGet();
    }

    /**
     * @param observed         调用机会总数
     * @param cancelledSkipped 因取消被跳过数
     * @param completed        正常执行数
     * @param cancelRatio      取消占比 cancelledSkipped/observed；无样本哨兵 −1
     */
    public record CancelSnapshot(long observed, long cancelledSkipped,
                                 long completed, double cancelRatio) {
    }

    /** 快照。 */
    public CancelSnapshot snapshot() {
        long total = observed.get();
        double ratio = total == 0 ? -1d : (double) cancelledSkipped.get() / total;
        return new CancelSnapshot(total, cancelledSkipped.get(), completed.get(), ratio);
    }

    /** 测试归零。 */
    public void resetForTest() {
        observed.set(0);
        cancelledSkipped.set(0);
        completed.set(0);
    }
}
