package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.ArrayDeque;

/**
 * 取消延迟追踪观察者（spec 1411 / T2123 / impl 1064）——Temporal cancellation
 * latency 思想：取消的正确性看「发出信号 → 实际停止」的时延。{@code cancel()}
 * 即返（requestCancel + 事件 + 指标），在途轮次的实际终结异步到达（流式经
 * doFinally → failTurnOnce → onTurnError 链路）——本观察者以未决取消键测距：
 * onCancel 时若该会话有<b>在途轮</b>则记请求时刻，轮终结（onTurnEnd/onTurnError）
 * 时消费键入环。
 *
 * <p><b>实例 = 单会话</b>（SessionObserver 回调不携带 sessionId，构造期绑定——
 * TurnErrorSampler 同型装配：customizer 内 {@code new CancelLatencyTracker(ctx.sessionId())}）。
 * 无轮取消（轮间操作）不入账；会话内轮次单飞（spec 40 §B）保证无并发轮竞态。
 * 环容量 {@value #RING_CAPACITY} 样本（StoreLatencyRing 口径先例）。
 * 纯读面：不触 cancel/终结语义。
 */
public final class CancelLatencyTracker implements SessionObserver {

    /** 延迟样本环容量。 */
    public static final int RING_CAPACITY = 64;

    private final String sessionId;
    private final ArrayDeque<Long> ring = new ArrayDeque<>(RING_CAPACITY);
    /** 累计入环样本数（环只保最近 RING_CAPACITY 个作分位窗——tracked 是全量）。 */
    private long trackedTotal;
    /** 未决取消请求时刻（epoch millis）；null = 无未决取消。 */
    private Long pendingCancelAt;
    private boolean turnInFlight;
    private final Object lock = new Object();

    /** 装配期构造：绑定会话（customizer 内以 ctx.sessionId() 提供）。 */
    public CancelLatencyTracker(String sessionId) {
        this.sessionId = sessionId == null ? "" : sessionId;
    }

    /** 本观察者绑定的会话 id。 */
    public String sessionId() {
        return sessionId;
    }

    @Override
    public void onTurnStart(int turnSeq, String userInput) {
        synchronized (lock) {
            turnInFlight = true;
        }
    }

    @Override
    public void onCancel() {
        synchronized (lock) {
            if (turnInFlight) {
                pendingCancelAt = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onTurnEnd(int turnSeq, String finalReply) {
        settle();
    }

    @Override
    public void onTurnError(int turnSeq, Throwable error) {
        settle();
    }

    /** 轮终结：消费未决取消键（有则延迟入环），清在途标记。 */
    private void settle() {
        synchronized (lock) {
            turnInFlight = false;
            if (pendingCancelAt == null) {
                return;
            }
            long latency = Math.max(0, System.currentTimeMillis() - pendingCancelAt);
            pendingCancelAt = null;
            trackedTotal++;
            if (ring.size() >= RING_CAPACITY) {
                ring.pollFirst(); // 挤掉最旧
            }
            ring.addLast(latency);
        }
    }

    /** 只读快照：累计入环数 + recent-rank 分位 + 未决取消标记。 */
    public CancelLatencyStats stats() {
        long[] sorted;
        int pending;
        synchronized (lock) {
            sorted = ring.stream().mapToLong(Long::longValue).sorted().toArray();
            pending = pendingCancelAt == null ? 0 : 1;
        }
        return new CancelLatencyStats(trackedTotal,
                percentile(sorted, 0.50), percentile(sorted, 0.95), pending);
    }

    /** 测试归零口：环与未决/在途状态全清。 */
    public void resetForTest() {
        synchronized (lock) {
            ring.clear();
            trackedTotal = 0;
            pendingCancelAt = null;
            turnInFlight = false;
        }
    }

    /** 升序秩插值分位（R-7 近似；空环 -1 哨兵）。 */
    private static long percentile(long[] sorted, double q) {
        if (sorted.length == 0) {
            return -1;
        }
        int index = (int) Math.ceil(q * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    /**
     * @param tracked        累计入环延迟样本数
     * @param p50Millis      延迟 P50（毫秒；空环 -1 哨兵）
     * @param p95Millis      延迟 P95（毫秒；空环 -1 哨兵）
     * @param pendingCancels 未决取消（0/1——单飞会话至多一个在途取消）
     */
    public record CancelLatencyStats(long tracked, long p50Millis,
                                     long p95Millis, int pendingCancels) {
    }
}
