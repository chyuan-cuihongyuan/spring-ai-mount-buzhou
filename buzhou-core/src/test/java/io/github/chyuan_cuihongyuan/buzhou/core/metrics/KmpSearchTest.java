package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3014 / T5030：KMP 合同——首配手算、可重叠 findAll、无匹配、
 * 空模式约定（indexOf 0 / findAll 拒绝）、模式长于文本、lps 手算
 * （CLRS ababaca）、随机对拍 JDK indexOf、每命中子串自证。
 */
class KmpSearchTest {

    @Test
    void firstMatchShouldMatchHandComputation() {
        assertThat(KmpSearch.indexOf("ababcabcabababd", "ababd")).isEqualTo(10);
        assertThat(KmpSearch.indexOf("hello", "ll")).isEqualTo(2);
        assertThat(KmpSearch.indexOf("mississippi", "issip")).isEqualTo(4);
    }

    @Test
    void findAllShouldAllowOverlaps() {
        assertThat(KmpSearch.findAll("aaaa", "aa")).isEqualTo(List.of(0, 1, 2));
        assertThat(KmpSearch.findAll("ababababa", "aba")).isEqualTo(List.of(0, 2, 4, 6));
    }

    @Test
    void noMatchShouldBeHonest() {
        assertThat(KmpSearch.indexOf("abcdef", "xyz")).isEqualTo(-1);
        assertThat(KmpSearch.findAll("abcdef", "xyz")).isEmpty();
    }

    @Test
    void emptyPatternShouldFollowConventions() {
        assertThat(KmpSearch.indexOf("abc", "")).isZero();
        assertThatThrownBy(() -> KmpSearch.findAll("abc", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KmpSearch.indexOf(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void patternLongerThanTextShouldMiss() {
        assertThat(KmpSearch.indexOf("ab", "abc")).isEqualTo(-1);
        assertThat(KmpSearch.findAll("ab", "abc")).isEmpty();
    }

    @Test
    void failureFunctionShouldMatchHandComputation() {
        // CLRS 经典：ababaca → lps = [0,0,1,2,3,0,1]
        assertThat(KmpSearch.failureFunction("ababaca")).isEqualTo(new int[] {0, 0, 1, 2, 3, 0, 1});
        assertThat(KmpSearch.failureFunction("aaaa")).isEqualTo(new int[] {0, 1, 2, 3});
        assertThat(KmpSearch.failureFunction("abcd")).isEqualTo(new int[] {0, 0, 0, 0});
    }

    @Test
    void randomCasesShouldAgreeWithJdkIndexOf() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(5);
        String alphabet = "ab";
        for (int trial = 0; trial < 200; trial++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 40; i++) {
                text.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            int from = rng.nextInt(30);
            int len = 1 + rng.nextInt(6);
            String pattern = text.substring(from, Math.min(40, from + len));
            assertThat(KmpSearch.indexOf(text.toString(), pattern))
                    .as("trial %d text=%s pattern=%s", trial, text, pattern)
                    .isEqualTo(text.indexOf(pattern));
        }
    }

    @Test
    void everyHitShouldBeAVerbatimMatch() {
        String text = "abcabcabc";
        String pattern = "abc";
        for (int hit : KmpSearch.findAll(text, pattern)) {
            assertThat(text.substring(hit, hit + pattern.length())).isEqualTo(pattern);
        }
    }
}
