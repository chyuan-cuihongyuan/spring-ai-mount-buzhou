package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮间到达间隔读面（L 会话 1700 系 R12 = effort #1711 / spec 1711 /
 * 票 T2623 + T2624 / impl 1311）——产品交互节奏遥测思想（Prometheus 直方
 * 前置的统计面）：轮与轮的到达间隔刻画用户节奏（机器连打 vs 人工思考），
 * 为空闲判定（{@link IdleSessionMonitor}）与采样（TurnSamplerHook）提供
 * 依据。
 *
 * <p>纯函数零状态：吃按轮序排列的轮开始时间戳，吐间隔序列/中位/p95
 * （最近秩法）。&lt;2 轮哨兵 −1。输入应单调递增——非单调由调用方负责。
 *
 * @since 1.0.0
 */
public final class TurnInterArrivalStats {

    private TurnInterArrivalStats() {
    }

    /**
     * @param turns         轮数
     * @param intervalsMillis 间隔序列（轮数−1；升序副本）
     * @param medianMillis  中位间隔（turns&lt;2 哨兵 −1）
     * @param p95Millis     p95 间隔（最近秩；turns&lt;2 哨兵 −1）
     */
    public record InterArrivalReport(int turns, List<Long> intervalsMillis,
                                     long medianMillis, long p95Millis) {
    }

    /** 审计入口：按轮序的时间戳（毫秒）。 */
    public static InterArrivalReport analyze(List<Long> turnEpochMillis) {
        List<Long> data = turnEpochMillis == null ? List.of() : turnEpochMillis;
        if (data.size() < 2) {
            return new InterArrivalReport(data.size(), List.of(), -1L, -1L);
        }
        List<Long> intervals = new ArrayList<>(data.size() - 1);
        for (int i = 1; i < data.size(); i++) {
            intervals.add(data.get(i) - data.get(i - 1));
        }
        List<Long> sorted = new ArrayList<>(intervals);
        sorted.sort(Long::compare);
        int n = sorted.size();
        long median = n % 2 == 1
                ? sorted.get(n / 2)
                : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
        int p95Index = (int) Math.ceil(0.95 * n) - 1;
        return new InterArrivalReport(data.size(), List.copyOf(intervals),
                median, sorted.get(Math.max(0, p95Index)));
    }
}
