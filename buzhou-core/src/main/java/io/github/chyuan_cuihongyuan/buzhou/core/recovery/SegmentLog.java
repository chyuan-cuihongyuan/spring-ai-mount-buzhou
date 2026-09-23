package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Segment Log 分段日志（spec 5028 / T6157 / impl 2179）——
 * Kafka 分段日志思想：`append` 分配递增 LSN，段满
 * （segmentCapacity 条）滚动新段；段数超 maxSegments 淘汰
 * 最旧段（droppedCount 诚实可见）——追加日志单文件无限增长
 * （清理只能整文件）与无保留上限（磁盘耗尽）的病解；
 * `readAll` 存活记录跨段按 LSN 序拼接。确定性无时间依赖。
 *
 * <p>与 SessionArchiver（归档）同族不同面：归档导出 vs 分段
 * 保留窗口。
 */
public final class SegmentLog {

    private final int segmentCapacity;
    private final int maxSegments;
    private final Deque<Deque<String>> segments = new ArrayDeque<>();
    private long totalAppended;
    private long droppedCount;

    /** 定构（容量参数 ≤0 fail-fast）。 */
    public SegmentLog(int segmentCapacity, int maxSegments) {
        if (segmentCapacity <= 0 || maxSegments <= 0) {
            throw new IllegalArgumentException("segmentCapacity>0 且 maxSegments>0："
                    + segmentCapacity + "/" + maxSegments);
        }
        this.segmentCapacity = segmentCapacity;
        this.maxSegments = maxSegments;
        segments.addLast(new ArrayDeque<>());
    }

    /** 追加记录（段满滚动；null/空 record fail-fast）。 */
    public long append(String record) {
        if (record == null || record.isEmpty()) {
            throw new IllegalArgumentException("record 非空");
        }
        Deque<String> current = segments.peekLast();
        if (current.size() >= segmentCapacity) {
            segments.addLast(new ArrayDeque<>());
            trimSegments();
            current = segments.peekLast();
        }
        current.addLast(record);
        totalAppended++;
        return totalAppended;
    }

    /** 存活记录全读（跨段按 LSN 序）。 */
    public List<String> readAll() {
        List<String> all = new ArrayList<>();
        for (Deque<String> segment : segments) {
            all.addAll(segment);
        }
        return List.copyOf(all);
    }

    /** 段数读数。 */
    public int segmentCount() {
        return segments.size();
    }

    /** 已淘汰记录数读数（保留上限的诚实代价）。 */
    public long droppedCount() {
        return droppedCount;
    }

    /** 已追加总数读数。 */
    public long totalAppended() {
        return totalAppended;
    }

    /** 存活窗口最早 LSN 读数（= dropped+1；全保留时为 1）。 */
    public long firstSurvivingLsn() {
        return droppedCount + 1;
    }

    /** 段数超上限时淘汰最旧段。 */
    private void trimSegments() {
        while (segments.size() > maxSegments) {
            Deque<String> evicted = segments.pollFirst();
            droppedCount += evicted.size();
        }
    }
}
