package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinnowFingerprintTest {

    @Test
    void shouldGuaranteeWindowHitsOnRandomText() {
        // 核心圣像：任何宽 w 的 gram 窗至少含一枚被选位置（guarantee 定理）
        Random random = new Random(88);
        for (int t = 0; t < 40; t++) {
            int n = 20 + random.nextInt(200);
            int k = 1 + random.nextInt(5);
            int w = 1 + random.nextInt(6);
            byte[] data = new byte[n];
            if (t % 2 == 0) {
                for (int i = 0; i < n; i++) {
                    data[i] = (byte) ('a' + random.nextInt(4));
                }
            } else {
                random.nextBytes(data);
            }
            List<Integer> prints = WinnowFingerprint.fingerprints(data, k, w);
            int grams = n - k + 1;
            for (int start = 0; start + w <= grams; start++) {
                boolean hit = false;
                for (int p : prints) {
                    if (p >= start && p < start + w) {
                        hit = true;
                        break;
                    }
                }
                assertThat(hit).as("图 %d 窗 [%d,%d) 必命中", t, start, start + w).isTrue();
            }
        }
    }

    @Test
    void shouldBeStableUnderLocalEditsAndDeterministic() {
        // 局部改动只影响邻近指纹（远段指纹不变）；确定性双跑
        String text = "the quick brown fox jumps over the lazy dog and runs away fast";
        byte[] data = text.getBytes(StandardCharsets.US_ASCII);
        List<Integer> prints = WinnowFingerprint.fingerprints(data, 5, 4);
        assertThat(prints).isSorted();
        assertThat(prints.size()).isLessThan(data.length - 5 + 1); // 远小于全 gram
        assertThat(prints).isEqualTo(WinnowFingerprint.fingerprints(data, 5, 4));
        // 中段改一字：头部/尾部指纹稳定（k+w 邻域外）
        byte[] edited = data.clone();
        edited[data.length / 2] = (byte) 'X';
        List<Integer> editedPrints = WinnowFingerprint.fingerprints(edited, 5, 4);
        long stableHead = prints.stream().filter(p -> p < data.length / 2 - 12
                && editedPrints.contains(p)).count();
        assertThat(stableHead).isGreaterThan(0);
    }

    @Test
    void shouldHandleDegenerateShapesAndFailFast() {
        assertThat(WinnowFingerprint.fingerprints(new byte[0], 3, 2)).isEmpty();
        assertThat(WinnowFingerprint.fingerprints("ab".getBytes(StandardCharsets.US_ASCII), 3, 2))
                .isEmpty();
        // w=1：全 gram 被选（无采样）
        byte[] data = "abcd".getBytes(StandardCharsets.US_ASCII);
        assertThat(WinnowFingerprint.fingerprints(data, 2, 1)).containsExactly(0, 1, 2);
        assertThatThrownBy(() -> WinnowFingerprint.fingerprints(null, 3, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WinnowFingerprint.fingerprints(data, 0, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WinnowFingerprint.fingerprints(data, 3, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
