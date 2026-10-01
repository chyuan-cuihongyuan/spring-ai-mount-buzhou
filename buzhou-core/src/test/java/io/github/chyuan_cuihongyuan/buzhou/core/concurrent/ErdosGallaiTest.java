package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 11020 / Y11041：ErdosGallai 合同验证——手锚+Havel–Hakimi 归约神像
 * 交叉互证+确定性+fail-fast。
 */
class ErdosGallaiTest {

    /** Havel–Hakimi 归约神像：降序排列后消首元、对其后 d 个各减一。 */
    private static boolean havelHakimi(int[] degrees) {
        int[] work = degrees.clone();
        while (true) {
            Arrays.sort(work);
            int d = work[work.length - 1];
            if (d == 0) {
                return true;
            }
            work[work.length - 1] = 0;
            for (int i = 0; i < d; i++) {
                int index = work.length - 2 - i;
                work[index]--;
                if (work[index] < 0) {
                    return false;
                }
            }
        }
    }

    @Test
    void shouldBeGraphical_whenTriangleDegrees() {
        assertThat(ErdosGallai.isGraphical(new int[]{2, 2, 2})).isTrue();
        assertThat(ErdosGallai.isGraphical(new int[]{1, 1})).isTrue();
        assertThat(ErdosGallai.isGraphical(new int[]{0})).isTrue();
    }

    @Test
    void shouldBeFalse_whenOddSum() {
        assertThat(ErdosGallai.isGraphical(new int[]{3, 3, 1})).isFalse();
    }

    @Test
    void shouldBeFalse_whenPrefixInequalityFails() {
        // [2,2,0]：k=1 前缀 2 > 0+min(2,1)+min(0,1)=1——不可图
        assertThat(ErdosGallai.isGraphical(new int[]{2, 2, 0})).isFalse();
    }

    @Test
    void shouldMatchHavelHakimi_whenRandomSequences() {
        Random random = new Random(11020L);
        int agreements = 0;
        for (int trial = 0; trial < 200; trial++) {
            int[] degrees = new int[5];
            for (int i = 0; i < 5; i++) {
                degrees[i] = random.nextInt(5);
            }
            boolean eg = ErdosGallai.isGraphical(degrees);
            boolean hh = havelHakimi(degrees);
            assertThat(eg).as("序列 %s EG=HH", Arrays.toString(degrees)).isEqualTo(hh);
            if (eg) {
                agreements++;
            }
        }
        assertThat(agreements).isGreaterThan(20);
    }

    @Test
    void shouldBeDeterministic_whenSameInputTwice() {
        int[] degrees = {3, 3, 2, 2, 2};
        assertThat(ErdosGallai.isGraphical(degrees))
                .isEqualTo(ErdosGallai.isGraphical(degrees));
    }

    @Test
    void shouldFailFast_whenNegativeDegree() {
        assertThatThrownBy(() -> ErdosGallai.isGraphical(new int[]{1, -1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("度非负");
        assertThatThrownBy(() -> ErdosGallai.isGraphical(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
