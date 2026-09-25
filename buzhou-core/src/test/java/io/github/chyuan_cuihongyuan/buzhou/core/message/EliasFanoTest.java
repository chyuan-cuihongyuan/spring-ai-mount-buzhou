package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6013：EliasFano 合同——单调序列高低位联合位图编码。
 * 随机密度 oracle 往返全等；低位宽密度依赖；存储上界；
 * fail-fast。
 */
class EliasFanoTest {

    private static long[] sortedRandom(int n, long universe, long seed) {
        Random rng = new Random(seed);
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = rng.nextLong(universe);
        }
        Arrays.sort(out);
        return out;
    }

    @Test
    void roundTripAcrossDensities() {
        long[][] cases = {
                sortedRandom(500, 1000, 1L),
                sortedRandom(500, 1_000_000, 2L),
                sortedRandom(200, Long.MAX_VALUE / 4, 3L),
                sortedRandom(1, 100, 4L),
                {7, 7, 7, 7},
                {0, 0, 1, 1, 2},
                {0},
                {Long.MAX_VALUE}
        };
        for (long[] input : cases) {
            EliasFano ef = EliasFano.encode(input);
            assertThat(ef.size()).isEqualTo(input.length);
            for (int i = 0; i < input.length; i++) {
                assertThat(ef.get(i)).as("case[0]=%d get(%d)", input[0], i).isEqualTo(input[i]);
            }
            assertThat(ef.decode()).containsExactly(input);
        }
    }

    @Test
    void lowerWidthFollowsDensity() {
        EliasFano step = EliasFano.encode(new long[]{0, 2, 4, 6});
        assertThat(step.lowerWidth()).as("U=6,n=4 → U/n=1 → 低位宽 1").isEqualTo(1);
        EliasFano sparse = EliasFano.encode(new long[]{1_000_000, 2_000_000, 3_000_000});
        assertThat(sparse.lowerWidth()).as("U/n≈10^6 → 低位宽 20").isEqualTo(20);
        EliasFano single = EliasFano.encode(new long[]{42});
        assertThat(single.lowerWidth()).as("U/n=42 → 低位宽 6").isEqualTo(6);
        EliasFano zero = EliasFano.encode(new long[]{0});
        assertThat(zero.lowerWidth()).as("全零宇宙零低位").isZero();
    }

    @Test
    void storageBeatsFlatArrayForDense() {
        EliasFano ef = EliasFano.encode(sortedRandom(1000, 2000, 6L));
        assertThat(ef.storedBits()).as("密集序列存储显著低于 64×n 直存")
                .isLessThan(1000L * 64 / 2);
    }

    @Test
    void sourceIsDefensivelyCopied() {
        long[] input = {1, 2, 3};
        EliasFano ef = EliasFano.encode(input);
        input[0] = 99;
        assertThat(ef.get(0)).isEqualTo(1);
        assertThat(ef.sourceCopy()).containsExactly(1, 2, 3).isNotSameAs(input);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> EliasFano.encode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EliasFano.encode(new long[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EliasFano.encode(new long[]{5, 3}))
                .isInstanceOf(IllegalArgumentException.class);
        EliasFano ef = EliasFano.encode(new long[]{1, 2, 3});
        assertThatThrownBy(() -> ef.get(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ef.get(3)).isInstanceOf(IllegalArgumentException.class);
    }
}
