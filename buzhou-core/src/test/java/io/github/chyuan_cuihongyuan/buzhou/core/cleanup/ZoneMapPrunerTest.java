package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.ZoneMapPruner.Zone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4012 / T6026：区块剪枝合同——点查命中、区间重叠、擦边保守、
 * 空值面、畸形 fail-fast。
 */
class ZoneMapPrunerTest {

    private static ZoneMapPruner threeZones() {
        return new ZoneMapPruner(List.of(
                new Zone("z1", 0, 10, 0),
                new Zone("z2", 20, 30, 2),
                new Zone("z3", 40, 50, 0)));
    }

    @Test
    void pointQueryShouldHitOnlyContainingZone() {
        ZoneMapPruner pruner = threeZones();
        assertThat(pruner.zonesMatching(5)).extracting(Zone::id).containsExactly("z1");
        assertThat(pruner.zonesMatching(25)).extracting(Zone::id).containsExactly("z2");
        assertThat(pruner.zonesMatching(15)).isEmpty();   // 沟里无块
        assertThat(pruner.zonesMatching(10)).extracting(Zone::id).containsExactly("z1");   // 边界含
    }

    @Test
    void rangeQueryShouldPruneDisjointZones() {
        ZoneMapPruner pruner = threeZones();
        assertThat(pruner.zonesOverlapping(5, 25)).extracting(Zone::id)
                .containsExactly("z1", "z2");
        assertThat(pruner.zonesSkipped(5, 25)).isEqualTo(1);
        assertThat(pruner.pruningRatio(5, 25)).isEqualTo(1.0 / 3);
        assertThat(pruner.zonesOverlapping(100, 200)).isEmpty();   // 全剪
        assertThat(pruner.pruningRatio(100, 200)).isEqualTo(1.0);
    }

    @Test
    void touchingRangesShouldConservativelyInclude() {
        ZoneMapPruner pruner = threeZones();
        // [31,35] 与 z2 的 max=30 擦边（不含）与 z3 的 min=40 擦边（不含）——全剪
        assertThat(pruner.zonesOverlapping(31, 35)).isEmpty();
        // [30,40] 双侧恰触边——含等语义两块都读（保守不漏）
        assertThat(pruner.zonesOverlapping(30, 40)).extracting(Zone::id)
                .containsExactly("z2", "z3");
    }

    @Test
    void nullBearingZonesShouldSurface() {
        ZoneMapPruner pruner = threeZones();
        assertThat(pruner.zonesWithNulls()).extracting(Zone::id).containsExactly("z2");
        assertThat(pruner.zoneCount()).isEqualTo(3);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new ZoneMapPruner(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZoneMapPruner(List.of(new Zone("bad", 10, 0, 0))))
                .isInstanceOf(IllegalArgumentException.class);   // min>max
        assertThatThrownBy(() -> new ZoneMapPruner(List.of(new Zone("bad", 0, 10, -1))))
                .isInstanceOf(IllegalArgumentException.class);   // nullCount<0
        assertThatThrownBy(() -> new ZoneMapPruner(List.of(new Zone(null, 0, 10, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        ZoneMapPruner pruner = threeZones();
        assertThatThrownBy(() -> pruner.zonesOverlapping(30, 20))
                .isInstanceOf(IllegalArgumentException.class);   // low>high
    }
}
