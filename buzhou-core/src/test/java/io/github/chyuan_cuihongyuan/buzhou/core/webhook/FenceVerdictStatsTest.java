package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.webhook.SequenceFence.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FenceVerdictStatsTest {

    private final SequenceFence fence = new SequenceFence();

    @Test
    void freshFenceHasAllZeroBuckets() {
        SequenceFence.FenceVerdictStats stats = fence.verdictStats();
        assertThat(stats.total()).isZero();
        assertThat(stats.byVerdict().get("CONTINUE")).isZero();
        assertThat(stats.byVerdict().get("GAP")).isZero();
        assertThat(stats.byVerdict().get("DUPLICATE")).isZero();
        assertThat(stats.byVerdict().get("RESET")).isZero();
        assertThat(stats.byVerdict().get("STALE")).isZero();
    }

    @Test
    void firstSeenBaselineCountsContinue() {
        fence.observe("sub-a", 1L, 1L);

        SequenceFence.FenceVerdictStats stats = fence.verdictStats();
        assertThat(stats.byVerdict().get("CONTINUE")).isEqualTo(1);
    }

    @Test
    void gapAndDuplicateAndResetCounted() {
        fence.observe("sub-a", null, 1L);
        fence.observe("sub-a", null, 3L); // GAP（跳号）
        fence.observe("sub-a", null, 3L); // DUPLICATE
        fence.observe("sub-a", null, 2L); // 同路径 seq 倒退 → RESET

        SequenceFence.FenceVerdictStats stats = fence.verdictStats();
        assertThat(stats.byVerdict().get("GAP")).isEqualTo(1);
        assertThat(stats.byVerdict().get("DUPLICATE")).isEqualTo(1);
        assertThat(stats.byVerdict().get("RESET")).isEqualTo(1);
    }

    @Test
    void explicitNewEpochCountsReset() {
        fence.observe("sub-a", 1L, 10L);
        fence.observe("sub-a", 2L, 11L); // 显式新纪元 → RESET

        SequenceFence.FenceVerdictStats stats = fence.verdictStats();
        assertThat(stats.byVerdict().get("RESET")).isEqualTo(1);
    }

    @Test
    void staleEpochCountedAndBaselineUnmoved() {
        fence.observe("sub-a", 2L, 10L);
        fence.observe("sub-a", 1L, 11L); // 旧纪元迟到 → STALE（基线不动）

        SequenceFence.FenceVerdictStats stats = fence.verdictStats();
        assertThat(stats.byVerdict().get("STALE")).isEqualTo(1);
        assertThat(stats.byVerdict().get("RESET")).isZero();
    }

    @Test
    void totalConservesAcrossAllVerdicts() {
        fence.observe("sub-a", 1L, 1L);
        fence.observe("sub-a", 1L, 3L); // GAP
        fence.observe("sub-a", 1L, 3L); // DUPLICATE
        fence.observe("sub-b", 1L, 10L);
        fence.observe("sub-b", 1L, 10L); // DUPLICATE

        long total = fence.verdictStats().byVerdict().values().stream()
                .mapToLong(Long::longValue).sum();
        assertThat(total).isEqualTo(5);
    }

    @Test
    void snapshotIsImmutable() {
        fence.observe("sub-a", 1L, 1L);

        var stats = fence.verdictStats();
        assertThatThrownBy(() -> stats.byVerdict().put("X", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
