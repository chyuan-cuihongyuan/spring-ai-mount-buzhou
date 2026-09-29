package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseRemainderTest {

    @Test
    void shouldSolveSunziClassic() {
        long solution = ChineseRemainder.crt(new long[]{2, 3, 2}, new long[]{3, 5, 7});
        assertThat(solution).isEqualTo(23);
    }

    @Test
    void shouldMatchBruteForceOracleOnRandomPairs() {
        Random random = new Random(8041);
        for (int round = 0; round < 500; round++) {
            long modulusA = 2 + random.nextInt(50);
            long modulusB = 2 + random.nextInt(50);
            if (gcd(modulusA, modulusB) != 1) {
                continue;
            }
            long remainderA = random.nextInt((int) modulusA);
            long remainderB = random.nextInt((int) modulusB);
            long solution = ChineseRemainder.crt(new long[]{remainderA, remainderB},
                    new long[]{modulusA, modulusB});
            assertThat(solution % modulusA).as("round=%d modA", round).isEqualTo(remainderA);
            assertThat(solution % modulusB).as("round=%d modB", round).isEqualTo(remainderB);
            assertThat(solution).isGreaterThanOrEqualTo(0);
            assertThat(solution).isLessThan(modulusA * modulusB);
        }
    }

    @Test
    void shouldFailFastOnNonCoprimeAndBadInputs() {
        assertThatThrownBy(() -> ChineseRemainder.crt(new long[]{1, 1}, new long[]{4, 6}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("互质");
        assertThatThrownBy(() -> ChineseRemainder.crt(null, new long[]{3}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChineseRemainder.crt(new long[]{1}, new long[]{}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChineseRemainder.crt(new long[]{1}, new long[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long tmp = a % b;
            a = b;
            b = tmp;
        }
        return a;
    }
}
