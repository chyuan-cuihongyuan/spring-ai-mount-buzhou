package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 833 / T1168：危险工具命中分布回归——top 排序/lastSeen/溢出桶/脏入参/空真。
 */
class DangerousToolHitStatsTest {

    @Test
    void topSortsByHitsDescending() {
        DangerousToolHitStats stats = new DangerousToolHitStats();
        stats.record("deploy", "confirm", 100);
        stats.record("deploy", "confirm", 300);
        stats.record("wipe", "confirm", 200);
        stats.record("deploy", "", 400);

        var top = stats.top(10);
        assertThat(top.get(0).tool()).isEqualTo("deploy");
        assertThat(top.get(0).hits()).isEqualTo(3);
        assertThat(top.get(0).lastSeenMillis()).isEqualTo(400); // lastSeen max
        assertThat(top.get(0).requiredState()).isEqualTo("confirm");
        assertThat(top.get(1).tool()).isEqualTo("wipe");
        assertThat(stats.totalHits()).isEqualTo(4); // 4 次记录全入账（含空白 requiredState 那次）
        assertThat(stats.distinctTools()).isEqualTo(2);
    }

    @Test
    void overflowBucketBeyondCap() {
        DangerousToolHitStats stats = new DangerousToolHitStats();
        for (int i = 0; i < DangerousToolHitStats.MAX_TOOLS; i++) {
            stats.record("tool" + i, null, i);
        }
        stats.record("extra", null, 999);

        assertThat(stats.distinctTools()).isEqualTo(DangerousToolHitStats.MAX_TOOLS + 1); // 64 具名+溢出桶
        var overflow = stats.top(DangerousToolHitStats.MAX_TOOLS).stream()
                .filter(t -> t.tool().equals(DangerousToolHitStats.OVERFLOW))
                .findFirst().orElseThrow();
        assertThat(overflow.hits()).isEqualTo(1);
        assertThat(overflow.lastSeenMillis()).isEqualTo(999);
    }

    @Test
    void dirtyAndTopEdges() {
        DangerousToolHitStats stats = new DangerousToolHitStats();
        stats.record(null, null, 1);
        stats.record("  ", "x", 2);
        assertThat(stats.totalHits()).isZero();
        assertThat(stats.top(5)).isEmpty();
        assertThat(stats.top(0)).isEmpty();
        assertThat(stats.top(-1)).isEmpty();

        stats.record("t", "confirm", 1);
        assertThat(stats.top(1)).hasSize(1);
    }
}
