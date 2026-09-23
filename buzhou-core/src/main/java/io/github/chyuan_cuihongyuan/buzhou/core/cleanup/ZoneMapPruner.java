package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.List;

/**
 * 区块 min/max 剪枝（spec 4012 / T6025 / impl 2113）——列存区块
 * 统计跳读思想（DuckDB zone map / Parquet row group 统计 / ORC
 * min-max 索引同款）：每块记 (min, max, nullCount) 三统计，查询
 * 先对统计做**谓词裁剪**——区间不交即整块免读（O(块数) 判定换
 * O(块体积) IO）。数据局部性（相邻值聚块）是剪枝率的地基——
 * 无序数据 min≈max 全域交叠、剪枝率退化为 0。
 *
 * <p>边界语义：区间重叠判定双侧**含等**（min≤high ∧ max≥low——
 * 擦边块保守读，漏读即错读）。与 SweepLineIntervals（区间计数）
 * 正交：本件管「读不读」，彼件管「叠了几层」。
 */
public final class ZoneMapPruner {

    /** 区块统计（id + min/max + 空值数）。 */
    public record Zone(String id, long min, long max, long nullCount) {
    }

    private final List<Zone> zones;

    /** 定构（zones 非 null；id 非空、min≤max、nullCount≥0 否则 fail-fast）。 */
    public ZoneMapPruner(List<Zone> zones) {
        if (zones == null) {
            throw new IllegalArgumentException("zones 非 null");
        }
        for (Zone z : zones) {
            if (z == null || z.id() == null || z.id().isEmpty()) {
                throw new IllegalArgumentException("zone 与其 id 非空");
            }
            if (z.min() > z.max()) {
                throw new IllegalArgumentException("min≤max 违反：" + z.id());
            }
            if (z.nullCount() < 0) {
                throw new IllegalArgumentException("nullCount≥0 违反：" + z.id());
            }
        }
        this.zones = List.copyOf(zones);
    }

    /** 与 [low, high] 可能相交的块（双侧含等——擦边保守读）。 */
    public List<Zone> zonesOverlapping(long low, long high) {
        if (low > high) {
            throw new IllegalArgumentException("low≤high：" + low + ">" + high);
        }
        return zones.stream().filter(z -> z.min() <= high && z.max() >= low).toList();
    }

    /** 含精确值的块（min≤v≤max）。 */
    public List<Zone> zonesMatching(long value) {
        return zonesOverlapping(value, value);
    }

    /** 含空值的块（IS NULL 语义必须读）。 */
    public List<Zone> zonesWithNulls() {
        return zones.stream().filter(z -> z.nullCount() > 0).toList();
    }

    /** 剪枝账：被跳过块数。 */
    public long zonesSkipped(long low, long high) {
        return zones.size() - zonesOverlapping(low, high).size();
    }

    /** 剪枝率（0–1；无块 NaN 诚实）。 */
    public double pruningRatio(long low, long high) {
        if (zones.isEmpty()) {
            return Double.NaN;
        }
        return (double) zonesSkipped(low, high) / zones.size();
    }

    /** 总块数读数。 */
    public int zoneCount() {
        return zones.size();
    }
}
