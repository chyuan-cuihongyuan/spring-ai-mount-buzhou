package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1910 / T3022：RED——三区曲线、边界含下、畸形。 */
class RandomEarlyDropTest {

    private static final int MIN_TH = 50;
    private static final int MAX_TH = 100;
    private static final double MAX_P = 0.1;

    /** 三区曲线：25→0；75→线性 0.05；120→顶格 0.1。 */
    @Test
    void threeZoneCurve() {
        assertThat(RandomEarlyDrop.dropProbability(25, MIN_TH, MAX_TH, MAX_P))
                .isCloseTo(0.0, within(1e-12));
        assertThat(RandomEarlyDrop.dropProbability(75, MIN_TH, MAX_TH, MAX_P))
                .isCloseTo(0.05, within(1e-12));
        assertThat(RandomEarlyDrop.dropProbability(120, MIN_TH, MAX_TH, MAX_P))
                .isCloseTo(0.1, within(1e-12));
    }

    /** 边界：恰 minTh 概率仍 0（BELOW 含 minTh）；恰 maxTh 达 maxP（ABOVE）。 */
    @Test
    void boundariesInclusive() {
        assertThat(RandomEarlyDrop.dropProbability(MIN_TH, MIN_TH, MAX_TH, MAX_P))
                .isCloseTo(0.0, within(1e-12));
        assertThat(RandomEarlyDrop.dropProbability(MAX_TH, MIN_TH, MAX_TH, MAX_P))
                .isCloseTo(MAX_P, within(1e-12));
        assertThat(RandomEarlyDrop.zone(MIN_TH, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.BELOW);
        assertThat(RandomEarlyDrop.zone(MIN_TH + 1, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.LINEAR);
        assertThat(RandomEarlyDrop.zone(MAX_TH, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.ABOVE);
    }

    /** 分区读数三区。 */
    @Test
    void zoneReadout() {
        assertThat(RandomEarlyDrop.zone(25, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.BELOW);
        assertThat(RandomEarlyDrop.zone(75, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.LINEAR);
        assertThat(RandomEarlyDrop.zone(120, MIN_TH, MAX_TH))
                .isEqualTo(RandomEarlyDrop.Zone.ABOVE);
    }

    /** 畸形入参 fail-fast：minTh ≥ maxTh、maxP 越界、负队列。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RandomEarlyDrop.dropProbability(10, 100, 50, MAX_P))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ≤ minTh < maxTh");
        assertThatThrownBy(() -> RandomEarlyDrop.dropProbability(10, 50, 100, 1.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxP 须在 (0,1]");
        assertThatThrownBy(() -> RandomEarlyDrop.zone(-1, 50, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queueSize 不能为负");
    }
}
