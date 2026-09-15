package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 1717 / T2636：VirtualKeyShareStats 直测——份额降序/HHI 集中度/reset。
 */
class VirtualKeyShareStatsTest {

    @Test
    void emptyCarriesNegativeHhi() {
        var stats = new VirtualKeyShareStats();
        assertThat(stats.census().total()).isZero();
        assertThat(stats.census().hhi()).isEqualTo(-1d);
    }

    @Test
    void evenShareAcrossKeysMinimizesHhi() {
        var stats = new VirtualKeyShareStats();
        stats.record("k1");
        stats.record("k2");
        stats.record("k3");
        stats.record("k4");
        var census = stats.census();
        assertThat(census.total()).isEqualTo(4);
        assertThat(census.hhi()).isCloseTo(0.25d, within(1e-9));
        assertThat(census.shareDesc()).containsKeys("k1", "k2", "k3", "k4");
    }

    @Test
    void dominatedKeyPushesHhiToOne() {
        var stats = new VirtualKeyShareStats();
        for (int i = 0; i < 9; i++) {
            stats.record("dominant");
        }
        stats.record("minor");
        var census = stats.census();
        assertThat(census.hhi()).isGreaterThan(0.8d);
        assertThat(census.shareDesc().entrySet().iterator().next().getKey()).isEqualTo("dominant");
        stats.record(null);
        assertThat(stats.census().shareDesc()).containsKey("_anonymous_");
    }

    @Test
    void singleKeyIsFullConcentration() {
        var stats = new VirtualKeyShareStats();
        stats.record("only");
        assertThat(stats.census().hhi()).isCloseTo(1d, within(1e-9));
    }
}
