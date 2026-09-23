package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4030 / T6062：EIP-1559 基础费合同——满涨空跌、恰目标不变、
 * floor 语义、地板止跌、钳制不变量、确定性回放、畸形 fail-fast。
 */
class Eip1559BaseFeeTest {

    private static final long TARGET = 1000L;
    private static final long INITIAL = 8_000_000_000L;
    private static final long MIN_FEE = 1_000_000_000L;

    @Test
    void fullBlockShouldRiseOneEighthAndEmptyShouldFall() {
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        assertThat(market.advance(2 * TARGET)).isEqualTo(9_000_000_000L);   // 满块 +1/8
        assertThat(market.advance(0)).isEqualTo(7_875_000_000L);            // 空块 −9e9/8（乘法调节非对称）
        assertThat(market.advance(TARGET)).isEqualTo(7_875_000_000L);       // 恰目标不变
        assertThat(market.lastDelta()).isEqualTo(0L);
    }

    @Test
    void halfFullBlockShouldRiseHalfStep() {
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        assertThat(market.advance(TARGET + TARGET / 2)).isEqualTo(8_500_000_000L);   // 1.5× 半步涨
    }

    @Test
    void fallShouldBeFloorNotTruncated() {
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, 7L, 0L);
        assertThat(market.advance(0)).isEqualTo(6L);   // −7/8 floor → −1（截断会回零不变）
        assertThat(market.lastDelta()).isEqualTo(-1L);
    }

    @Test
    void floorShouldStopTheFall() {
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        long fee = INITIAL;
        for (int i = 0; i < 40; i++) {
            fee = market.advance(0);
        }
        assertThat(fee).isEqualTo(MIN_FEE);            // 跌到地板止跌
        assertThat(market.advance(0)).isEqualTo(MIN_FEE);
        assertThat(market.lastDelta()).isEqualTo(0L);
    }

    @Test
    void singleStepShouldNeverExceedOneEighth() {
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, INITIAL, 0L);
        for (long used = 0; used <= 2 * TARGET; used += 250) {
            long before = market.currentBaseFee();
            long after = market.advance(used);
            long change = Math.abs(after - before);
            assertThat(change).as("用量 %s 单步钳制", used).isLessThanOrEqualTo(before / 8);
        }
    }

    @Test
    void sameSequenceShouldReplaySameTrajectory() {
        long[] load = {2000, 0, 1000, 2000, 1500};
        Eip1559BaseFee first = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        Eip1559BaseFee second = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        for (long used : load) {
            assertThat(first.advance(used)).isEqualTo(second.advance(used));
        }
        assertThat(first.currentBaseFee()).isEqualTo(second.currentBaseFee());
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new Eip1559BaseFee(0, INITIAL, MIN_FEE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Eip1559BaseFee(TARGET, INITIAL, INITIAL + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Eip1559BaseFee(TARGET, INITIAL, -1))
                .isInstanceOf(IllegalArgumentException.class);
        Eip1559BaseFee market = new Eip1559BaseFee(TARGET, INITIAL, MIN_FEE);
        assertThatThrownBy(() -> market.advance(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> market.advance(2 * TARGET + 1))   // 超弹性违约
                .isInstanceOf(IllegalArgumentException.class);
    }
}
