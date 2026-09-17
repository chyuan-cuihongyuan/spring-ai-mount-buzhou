package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3019 / T5040：TTL 抖动合同——按键确定性（跨调用跨次同值）、
 * 带宽夹持、双侧铺开（既有早到期也有晚到期）、零抖动精确、下限
 * 兜底 1ms、参数 fail-fast、键分散度。
 */
class TtlJitterTest {

    @Test
    void sameKeyShouldAlwaysMapToSameTtl() {
        long first = TtlJitter.jitteredTtlMillis(1_000, 0.2, "session-42");
        long second = TtlJitter.jitteredTtlMillis(1_000, 0.2, "session-42");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void allKeysShouldStayWithinBand() {
        for (int i = 0; i < 1_000; i++) {
            long ttl = TtlJitter.jitteredTtlMillis(1_000, 0.2, "key-" + i);
            assertThat(ttl).isBetween(800L, 1_200L);
        }
    }

    @Test
    void jitterShouldSpreadBothSidesOfBase() {
        int below = 0;
        int above = 0;
        for (int i = 0; i < 1_000; i++) {
            long ttl = TtlJitter.jitteredTtlMillis(1_000, 0.5, "spread-" + i);
            if (ttl < 1_000) {
                below++;
            } else if (ttl > 1_000) {
                above++;
            }
        }
        assertThat(below).isGreaterThanOrEqualTo(100);
        assertThat(above).isGreaterThanOrEqualTo(100);
    }

    @Test
    void zeroJitterShouldBeExact() {
        assertThat(TtlJitter.jitteredTtlMillis(3_600, 0, "any-key")).isEqualTo(3_600);
    }

    @Test
    void tinyBaseShouldFloorAtOneMillis() {
        for (int i = 0; i < 200; i++) {
            assertThat(TtlJitter.jitteredTtlMillis(1, 0.9, "tiny-" + i))
                    .isGreaterThanOrEqualTo(TtlJitter.MIN_TTL_MILLIS);
        }
    }

    @Test
    void differentKeysShouldProduceVariedTtls() {
        long distinct = java.util.stream.IntStream.range(0, 100)
                .mapToLong(i -> TtlJitter.jitteredTtlMillis(1_000, 0.3, "vary-" + i))
                .distinct()
                .count();
        assertThat(distinct).isGreaterThanOrEqualTo(10);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> TtlJitter.jitteredTtlMillis(0, 0.2, "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TtlJitter.jitteredTtlMillis(100, 1.0, "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TtlJitter.jitteredTtlMillis(100, -0.1, "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TtlJitter.jitteredTtlMillis(100, 0.2, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
