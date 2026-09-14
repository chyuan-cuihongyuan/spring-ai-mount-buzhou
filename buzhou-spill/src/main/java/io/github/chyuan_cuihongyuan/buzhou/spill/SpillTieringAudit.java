package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spill 冷热分层访问审计（spec 1438 / T2183 / impl 1092）——MinIO tiering /
 * S3 lifecycle（访问频率决定分层——热数据留快层、冷数据下沉）思想：
 * {@link ReadAuditTrail} 记录了「谁被读过」，本审计把读事件按 uri 聚合，
 * 与**存量 handle 总数**对照，量化三类水位：从未被回读（写完即死的冷数据）、
 * 单次回读（一次性消费）、多次回读（热点）。冷占比高 = spill TTL/清理
 * 策略可以更激进；热点集中 = 值得预热。
 *
 * <p>纯函数零状态：输入为存量 uri 总数（{@code DiskSpillStore} 侧枚举）
 * 与读事件明细（{@link ReadAuditTrail.ReadRecord} 列表，窗口截断口径显式——
 * trail 有界 128，未进窗的读不参与「多次」判定）。只读不裁决（分层动作
 * 归宿主）。
 */
public final class SpillTieringAudit {

    /** 热点阈值：被回读 ≥ 2 次记热点。 */
    public static final int HOT_READ_THRESHOLD = 2;

    private SpillTieringAudit() {
    }

    /**
     * @param totalHandles    存量 handle uri 总数（含从未回读者）
     * @param neverReadCount  从未被回读的 handle 数（冷数据——TTL/清理候选）
     * @param singleReadCount 单次回读数（一次性消费）
     * @param multiReadCount  多次回读数（≥{@link #HOT_READ_THRESHOLD} 次的热点）
     */
    public record TieringReport(long totalHandles, long neverReadCount,
                                long singleReadCount, long multiReadCount) {

        /** 热点占比 = multiReadCount/totalHandles（空库 -1 哨兵）。 */
        public double hotRatio() {
            return totalHandles == 0 ? -1d : (double) multiReadCount / totalHandles;
        }

        /** 冷占比 = neverReadCount/totalHandles（空库 -1 哨兵）。 */
        public double coldRatio() {
            return totalHandles == 0 ? -1d : (double) neverReadCount / totalHandles;
        }
    }

    /**
     * 审计入口。
     *
     * @param totalHandleUris 存量 handle uri 全集大小（DiskSpillStore 枚举）
     * @param readRecords     读事件明细（ReadAuditTrail 快照；有界窗口口径）
     */
    public static TieringReport analyze(int totalHandleUris, List<ReadAuditTrail.ReadRecord> readRecords) {
        Map<String, Integer> readsByUri = new HashMap<>();
        if (readRecords != null) {
            for (ReadAuditTrail.ReadRecord r : readRecords) {
                readsByUri.merge(r.uri(), 1, Integer::sum);
            }
        }
        long never = 0;
        long single = 0;
        long multi = 0;
        // 精确口径：never = 存量全集 − 读事件覆盖过的 distinct uri
        long distinctRead = readsByUri.size();
        never = Math.max(0, (long) totalHandleUris - distinctRead);
        for (int count : readsByUri.values()) {
            if (count >= HOT_READ_THRESHOLD) {
                multi++;
            } else {
                single++;
            }
        }
        return new TieringReport(totalHandleUris, never, single, multi);
    }
}
