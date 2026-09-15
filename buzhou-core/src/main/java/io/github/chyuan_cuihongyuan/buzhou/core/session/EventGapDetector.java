package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.List;

/**
 * 会话事件时间间隙检测（L 会话 1700 系 R11 = effort #1710 / spec 1710 /
 * 票 T2621 + T2622 / impl 1310）——Flink event-time gap 思想：会话事件流
 * 的时间间隙是「卡住/断流/挂起」的第一信号——{@link TurnSequenceAudit}
 * 管「序号对不对」，本面管「时间断没断」。
 *
 * <p>纯函数零状态：吃按事件序排列的时间戳与间隙阈值，吐间隙数与最大间隙。
 * 间隙 = 相邻事件时间差 &gt; 阈值（严格大于）。&lt;2 事件无间隙可言。
 *
 * @since 1.0.0
 */
public final class EventGapDetector {

    private EventGapDetector() {
    }

    /**
     * @param events           事件数（入参长度）
     * @param thresholdMillis  阈值（回显，审计自洽）
     * @param gapCount         超阈值间隙数
     * @param largestGapMillis 最大相邻间隙（events&lt;2 哨兵 −1）
     */
    public record GapReport(int events, long thresholdMillis,
                            int gapCount, long largestGapMillis) {
    }

    /** 检测入口：按事件序的时间戳（毫秒）。 */
    public static GapReport analyze(List<Long> eventEpochMillis, long thresholdMillis) {
        List<Long> data = eventEpochMillis == null ? List.of() : eventEpochMillis;
        if (data.size() < 2) {
            return new GapReport(data.size(), thresholdMillis, 0, -1L);
        }
        int gapCount = 0;
        long largest = 0L;
        for (int i = 1; i < data.size(); i++) {
            long gap = data.get(i) - data.get(i - 1);
            if (gap > thresholdMillis) {
                gapCount++;
            }
            largest = Math.max(largest, Math.abs(gap));
        }
        return new GapReport(data.size(), thresholdMillis, gapCount, largest);
    }
}
