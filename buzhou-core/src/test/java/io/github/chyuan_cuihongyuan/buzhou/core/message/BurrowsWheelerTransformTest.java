package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BurrowsWheelerTransformTest {

    @Test
    void shouldMatchHandAnchors() {
        // "ab"：旋转 ab/ba → 排序 ab,ba → 末列 b,a；原串行位 0
        BurrowsWheelerTransform.BwtResult ab =
                BurrowsWheelerTransform.transform("ab".getBytes(StandardCharsets.US_ASCII));
        assertThat(ab.lastColumn()).containsExactly('b', 'a');
        assertThat(ab.primaryIndex()).isZero();
        // "aab"：旋转 aab/aba/baa → 排序 aab,aba,baa → 末列 b,a,a；行位 0
        BurrowsWheelerTransform.BwtResult aab =
                BurrowsWheelerTransform.transform("aab".getBytes(StandardCharsets.US_ASCII));
        assertThat(aab.lastColumn()).containsExactly('b', 'a', 'a');
        assertThat(aab.primaryIndex()).isZero();
        // 空串
        BurrowsWheelerTransform.BwtResult empty = BurrowsWheelerTransform.transform(new byte[0]);
        assertThat(empty.lastColumn()).isEmpty();
    }

    @Test
    void shouldClusterRepeatedContexts() {
        // 聚簇圣像：重复模式串的末列相邻同字符对显著多于原串
        byte[] data = "abcabcabcabcabcabc".getBytes(StandardCharsets.US_ASCII);
        BurrowsWheelerTransform.BwtResult result = BurrowsWheelerTransform.transform(data);
        int clustered = adjacentEqualPairs(result.lastColumn());
        int original = adjacentEqualPairs(data);
        assertThat(clustered).isGreaterThan(original);
    }

    private static int adjacentEqualPairs(byte[] bytes) {
        int pairs = 0;
        for (int i = 1; i < bytes.length; i++) {
            if (bytes[i] == bytes[i - 1]) {
                pairs++;
            }
        }
        return pairs;
    }

    @Test
    void shouldRoundTripExactly() {
        String[] texts = {"banana", "mississippi", "abababab", "aaaaaaaaaa", "zyxwvu",
                "the quick brown fox jumps over the lazy dog"};
        for (String text : texts) {
            byte[] data = text.getBytes(StandardCharsets.US_ASCII);
            BurrowsWheelerTransform.BwtResult result = BurrowsWheelerTransform.transform(data);
            assertThat(BurrowsWheelerTransform.inverse(result.lastColumn(), result.primaryIndex()))
                    .as("文本 %s 往返", text).containsExactly(data);
        }
        Random random = new Random(19);
        for (int t = 0; t < 60; t++) {
            int n = random.nextInt(300);
            byte[] data = new byte[n];
            // 受限字母表（偏斜）+ 全谱两类各半
            if (t % 2 == 0) {
                for (int i = 0; i < n; i++) {
                    data[i] = (byte) ('a' + random.nextInt(4));
                }
            } else {
                random.nextBytes(data);
            }
            BurrowsWheelerTransform.BwtResult result = BurrowsWheelerTransform.transform(data);
            assertThat(BurrowsWheelerTransform.inverse(result.lastColumn(), result.primaryIndex()))
                    .as("随机 %d 往返", t).containsExactly(data);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        byte[] data = "determinism".getBytes(StandardCharsets.US_ASCII);
        BurrowsWheelerTransform.BwtResult first = BurrowsWheelerTransform.transform(data);
        BurrowsWheelerTransform.BwtResult second = BurrowsWheelerTransform.transform(data);
        assertThat(first.lastColumn()).containsExactly(second.lastColumn());
        assertThat(first.primaryIndex()).isEqualTo(second.primaryIndex());
        assertThatThrownBy(() -> BurrowsWheelerTransform.transform(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BurrowsWheelerTransform.inverse(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BurrowsWheelerTransform.inverse(new byte[]{'a', 'b'}, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("行位越域");
    }
}
