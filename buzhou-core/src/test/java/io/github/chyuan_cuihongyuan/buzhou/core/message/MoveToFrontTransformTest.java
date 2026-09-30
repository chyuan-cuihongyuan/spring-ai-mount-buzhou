package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveToFrontTransformTest {

    @Test
    void shouldMatchHandAnchors() {
        // 逆序三符号 {2,1,0}：每步都查到「仍在原位」→ 全 2
        assertThat(MoveToFrontTransform.encode(new byte[]{2, 1, 0})).containsExactly(2, 2, 2);
        // 顺序 {0,1,2}：初始即序 → 位次 0,1,2
        assertThat(MoveToFrontTransform.encode(new byte[]{0, 1, 2})).containsExactly(0, 1, 2);
        // 重复字节：首查后恒表首 → 0,0,0
        assertThat(MoveToFrontTransform.encode(new byte[]{7, 7, 7, 7})).containsExactly(7, 0, 0, 0);
        assertThat(MoveToFrontTransform.encode(new byte[0])).isEmpty();
    }

    @Test
    void shouldSkewClusteredInputTowardLowRanks() {
        // 局部性圣像：BWT 聚簇样式的重复模式串 → 小位次占主导（平均位次显著 < 64）
        byte[] clustered = "aaaabbbbccccddddaaaabbbbccccdddd".getBytes(StandardCharsets.US_ASCII);
        byte[] ranks = MoveToFrontTransform.encode(clustered);
        long sum = 0;
        for (byte r : ranks) {
            sum += r & 0xFF;
        }
        assertThat(sum / ranks.length).isLessThan(64);
        // 对照：乱序全谱输入平均位次高
        byte[] shuffled = new byte[clustered.length];
        new Random(3).nextBytes(shuffled);
        byte[] shuffledRanks = MoveToFrontTransform.encode(shuffled);
        long sum2 = 0;
        for (byte r : shuffledRanks) {
            sum2 += r & 0xFF;
        }
        assertThat(sum2 / shuffledRanks.length).isGreaterThan(sum / ranks.length);
    }

    @Test
    void shouldRoundTripExactly() {
        String[] texts = {"banana", "mississippi", "aaaaaaaa", "the quick brown fox"};
        for (String text : texts) {
            byte[] data = text.getBytes(StandardCharsets.US_ASCII);
            assertThat(MoveToFrontTransform.decode(MoveToFrontTransform.encode(data)))
                    .as("文本 %s 往返", text).containsExactly(data);
        }
        Random random = new Random(29);
        for (int t = 0; t < 60; t++) {
            int n = random.nextInt(500);
            byte[] data = new byte[n];
            random.nextBytes(data);
            assertThat(MoveToFrontTransform.decode(MoveToFrontTransform.encode(data)))
                    .as("随机 %d 往返", t).containsExactly(data);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        byte[] data = "deterministic".getBytes(StandardCharsets.US_ASCII);
        assertThat(MoveToFrontTransform.encode(data))
                .containsExactly(MoveToFrontTransform.encode(data));
        assertThatThrownBy(() -> MoveToFrontTransform.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoveToFrontTransform.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
