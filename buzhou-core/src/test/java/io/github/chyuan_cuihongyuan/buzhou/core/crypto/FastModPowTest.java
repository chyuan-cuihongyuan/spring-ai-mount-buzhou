package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7037：FastModPow 合同——平方-乘模幂。费马小定理
 * 钉子；随机 vs JDK BigInteger 圣像；零/一指数；fail-fast。
 */
class FastModPowTest {

    @Test
    void fermatLittleTheoremAnchors() {
        assertThat(FastModPow.modPow(2, 256, 257)).isEqualTo(1L);
        assertThat(FastModPow.modPow(3, 256, 257)).isEqualTo(1L);
        assertThat(FastModPow.modPow(5, 10, 13)).isEqualTo(12L);
        assertThat(FastModPow.modPow(7, 0, 13)).isEqualTo(1L);
        assertThat(FastModPow.modPow(123, 1, 1000)).isEqualTo(123L);
    }

    @Test
    void randomCasesMatchBigIntegerOracle() {
        Random rng = new Random(7037L);
        for (int round = 0; round < 200; round++) {
            long base = rng.nextInt(1000);
            long exponent = rng.nextInt(200);
            long mod = 1 + rng.nextInt(1_000_000);
            long expected = java.math.BigInteger.valueOf(base)
                    .modPow(java.math.BigInteger.valueOf(exponent),
                            java.math.BigInteger.valueOf(mod))
                    .longValue();
            assertThat(FastModPow.modPow(base, exponent, mod))
                    .as("%d^%d mod %d", base, exponent, mod).isEqualTo(expected);
        }
    }

    @Test
    void safeDomainAndFailFast() {
        assertThatThrownBy(() -> FastModPow.modPow(2, 10, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FastModPow.modPow(2, -1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FastModPow.modPow(2, 10, FastModPow.MOD_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
