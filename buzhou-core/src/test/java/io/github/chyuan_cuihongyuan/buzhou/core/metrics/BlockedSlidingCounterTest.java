package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5040 / T6182：分块滑窗计数器合同——小窗逐位钉住、
 * 500 位扫描真值恒在[下界,上界]、块数上界、零流为零、
 * fail-fast。
 */
class BlockedSlidingCounterTest {

    private static final long WINDOW = 10;

    private static final long BLOCK_COUNT = 2;

    private static final long ORACLE_WINDOW = 100;

    private static final long ORACLE_BLOCKS = 10;

    private static final int SWEEP_LENGTH = 500;

    private static final int HIT_PERIOD = 7;

    @Test
    void smallWindowShouldPinEstimatesAtScriptedPosition() {
        BlockedSlidingCounter counter = new BlockedSlidingCounter(WINDOW, BLOCK_COUNT);
        for (int i = 1; i <= 11; i++) {
            counter.add(i % 3 == 0);
        }
        assertThat(counter.position()).isEqualTo(11);
        assertThat(counter.lowerEstimate()).isEqualTo(2L);
        assertThat(counter.upperBound()).isEqualTo(3L);
        assertThat(counter.blockCount()).isEqualTo(3);
        assertThat(counter.blockSize()).isEqualTo(5L);
    }

    @Test
    void sweepShouldKeepTruthInsideBoundsAtEveryPosition() {
        BlockedSlidingCounter counter = new BlockedSlidingCounter(ORACLE_WINDOW, ORACLE_BLOCKS);
        for (int i = 1; i <= SWEEP_LENGTH; i++) {
            boolean hit = i % HIT_PERIOD == 0;
            counter.add(hit);
            long truth = 0;
            for (int j = (int) Math.max(1, i - ORACLE_WINDOW + 1); j <= i; j++) {
                if (j % HIT_PERIOD == 0) {
                    truth++;
                }
            }
            assertThat(counter.lowerEstimate())
                    .as("位置 %d 下界", i).isLessThanOrEqualTo(truth);
            assertThat(counter.upperBound())
                    .as("位置 %d 上界", i).isGreaterThanOrEqualTo(truth);
            assertThat(counter.upperBound() - counter.lowerEstimate())
                    .as("位置 %d 界宽", i).isLessThanOrEqualTo(ORACLE_BLOCKS);
        }
    }

    @Test
    void zeroStreamShouldStayAtZero() {
        BlockedSlidingCounter counter = new BlockedSlidingCounter(WINDOW, BLOCK_COUNT);
        for (int i = 1; i <= 30; i++) {
            counter.add(false);
        }
        assertThat(counter.lowerEstimate()).isZero();
        assertThat(counter.upperBound()).isZero();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new BlockedSlidingCounter(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlockedSlidingCounter(10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlockedSlidingCounter(-5, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
