package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1924 / T3050：空闲连接收割——入选、排序、边界、畸形。 */
class IdleConnectionReaperTest {

    /** 混合闲置：maxIdle 10s → 闲置 30s/20s 入选、闲置 11s 不选。 */
    @Test
    void mixedSelectionWithBoundary() {
        Map<String, Long> used = Map.of("stalest", 10_000L, "stale", 20_000L, "fresh", 35_000L);
        List<String> reaped = IdleConnectionReaper.reapCandidates(used, 40_000L, 10_000L);
        assertThat(reaped).containsExactly("stalest", "stale");
        assertThat(reaped).doesNotContain("fresh");
    }

    /** 闲置最久排前：有配额时先收最旧的。 */
    @Test
    void oldestFirstOrdering() {
        Map<String, Long> used = Map.of("p", 5_000L, "q", 1_000L, "r", 9_000L);
        List<String> reaped = IdleConnectionReaper.reapCandidates(used, 40_000L, 10_000L);
        assertThat(reaped).containsExactly("q", "p", "r");
    }

    /** 闲置时长读数与畸形 fail-fast。 */
    @Test
    void idleReadoutAndMalformed() {
        assertThat(IdleConnectionReaper.idleMillis(1_000, 1_500)).isEqualTo(500);
        assertThatThrownBy(() -> IdleConnectionReaper.reapCandidates(
                Map.of(), 100, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lastUsedByConn 不能为空");
        assertThatThrownBy(() -> IdleConnectionReaper.idleMillis(2_000, 1_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("now 早于 lastUsed");
        assertThatThrownBy(() -> IdleConnectionReaper.reapCandidates(
                Map.of("a", 1L), 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxIdleMillis 不能小于 1");
    }
}
