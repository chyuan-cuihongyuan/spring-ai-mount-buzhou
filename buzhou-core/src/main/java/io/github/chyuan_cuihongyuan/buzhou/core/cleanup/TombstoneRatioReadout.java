package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

/**
 * 墓碑占比读面（spec 1850 / T2901 / impl 1451）——LSM-Tree tombstone /
 * Cassandra compaction 思想：删除先落**墓碑**（删除标记）后靠 compaction
 * 物理回收——墓碑占比是两张账：**空间账**（多少存储背着死数据）与**读
 * 放大账**（每次读要跳过墓碑——有效数据密度 1/(1−占比)，占比 0.5 时每
 * 读两个键跳一个墓碑）。占比过阈即该压实——「删了但没真删」的账面化。
 *
 * <p>纯函数零状态、只读不压实（compaction 归宿主）。
 */
public final class TombstoneRatioReadout {

    /** 建议压实的默认占比阈（Cassandra compaction 经验量级）。 */
    public static final double DEFAULT_COMPACT_THRESHOLD = 0.2d;

    private TombstoneRatioReadout() {
    }

    /**
     * 占比读面。契约：liveEntries/tombstones ≥ 0、threshold ∈ [0,1] 非
     * NaN（fail-fast）；语义：ratio = tombstones/(live+tombstones)
     *（全空 0——没有数据就没有墓碑语义）。
     */
    public static Ratio ratioOf(long liveEntries, long tombstones) {
        if (liveEntries < 0 || tombstones < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：live=%d, tombstones=%d", liveEntries, tombstones));
        }
        long total = liveEntries + tombstones;
        double ratio = total == 0 ? 0d : (double) tombstones / total;
        return new Ratio(liveEntries, tombstones, ratio);
    }

    /** 占比快照 + 派生读数。 */
    public record Ratio(long liveEntries, long tombstones, double ratio) {

        /** 读放大倍数 = 1/(1−ratio)（占比 ≥1 时无穷——全墓碑，读什么都
         * 跳不完；全空 1.0）。 */
        public double readAmplification() {
            if (liveEntries + tombstones == 0) {
                return 1.0d;
            }
            if (ratio >= 1.0d) {
                return Double.POSITIVE_INFINITY;
            }
            return 1d / (1d - ratio);
        }

        /** 是否过压实阈。 */
        public boolean shouldCompact(double threshold) {
            if (Double.isNaN(threshold) || threshold < 0 || threshold > 1) {
                throw new IllegalArgumentException("threshold 须在 [0,1]：" + threshold);
            }
            return ratio >= threshold;
        }
    }
}
