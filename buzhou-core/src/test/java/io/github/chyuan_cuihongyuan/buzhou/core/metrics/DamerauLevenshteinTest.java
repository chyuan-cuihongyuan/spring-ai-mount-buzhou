package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7022：DamerauLevenshtein 合同——四算子编辑距离。
 * 换位经典锚（teh/the、ca/abc）；无换位域与纯 Levenshtein
 * 全等；随机域性质界；fail-fast。
 */
class DamerauLevenshteinTest {

    @Test
    void transpositionClassics() {
        assertThat(DamerauLevenshtein.distance("teh", "the")).isEqualTo(1);
        assertThat(DamerauLevenshtein.distance("ca", "abc"))
                .as("OSA 限制版：先改后换位不可复用——值为 3（真 DL 为 2，诚实边界）")
                .isEqualTo(3);
        assertThat(DamerauLevenshtein.distance("flaw", "lawn")).isEqualTo(2);
        assertThat(DamerauLevenshtein.distance("kitten", "sitting")).isEqualTo(3);
    }

    @Test
    void emptyAndIdentity() {
        assertThat(DamerauLevenshtein.distance("", "abc")).isEqualTo(3);
        assertThat(DamerauLevenshtein.distance("abc", "")).isEqualTo(3);
        assertThat(DamerauLevenshtein.distance("", "")).isZero();
        assertThat(DamerauLevenshtein.distance("same", "same")).isZero();
        assertThat(DamerauLevenshtein.similarity("abc", "abc")).isEqualTo(1.0);
        assertThat(DamerauLevenshtein.similarity("", "")).isEqualTo(1.0);
    }

    @Test
    void transpositionFreeDomainMatchesPlainLevenshtein() {
        assertThat(DamerauLevenshtein.distance("abcdef", "azcedf")).isEqualTo(2);
        assertThat(DamerauLevenshtein.distance("sunday", "saturday")).isEqualTo(3);
        assertThat(DamerauLevenshtein.similarity("flaw", "lawn"))
                .as("四算子下 2/4=0.5").isEqualTo(0.5);
    }

    @Test
    void randomSmallDomainPropertyBounds() {
        Random rng = new Random(7022L);
        for (int round = 0; round < 300; round++) {
            String a = randomString(rng, 1 + rng.nextInt(8));
            String b = randomString(rng, 1 + rng.nextInt(8));
            int d = DamerauLevenshtein.distance(a, b);
            assertThat(d).as("%s→%s", a, b)
                    .isBetween(Math.abs(a.length() - b.length()),
                            Math.max(a.length(), b.length()));
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> DamerauLevenshtein.distance(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DamerauLevenshtein.distance("a", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String randomString(Random rng, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) ('a' + rng.nextInt(3)));
        }
        return sb.toString();
    }
}
