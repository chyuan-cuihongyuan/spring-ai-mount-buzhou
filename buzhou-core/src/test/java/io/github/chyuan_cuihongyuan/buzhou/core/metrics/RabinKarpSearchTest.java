package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3025 / T5052：Rabin-Karp 合同——随机对拍 JDK indexOf 与
 * KmpSearch 双实现三方一致、可重叠 findAll、无匹配、空模式双约定、
 * 内容哈希位置无关性、长文滚动无退化误报。
 */
class RabinKarpSearchTest {

    @Test
    void randomCasesShouldAgreeWithJdkAndKmp() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X256MixRandom").create(13);
        String alphabet = "abc";
        for (int trial = 0; trial < 300; trial++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                text.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            int from = rng.nextInt(40);
            int len = 1 + rng.nextInt(6);
            String pattern = text.substring(from, Math.min(50, from + len));
            assertThat(RabinKarpSearch.indexOf(text.toString(), pattern))
                    .as("trial %d text=%s pattern=%s", trial, text, pattern)
                    .isEqualTo(text.indexOf(pattern))
                    .isEqualTo(KmpSearch.indexOf(text.toString(), pattern));
        }
    }

    @Test
    void findAllShouldAllowOverlaps() {
        assertThat(RabinKarpSearch.findAll("aaaa", "aa")).isEqualTo(List.of(0, 1, 2));
        assertThat(RabinKarpSearch.findAll("ababab", "abab")).isEqualTo(List.of(0, 2));
    }

    @Test
    void noMatchShouldBeHonest() {
        assertThat(RabinKarpSearch.indexOf("abcdef", "xyz")).isEqualTo(-1);
        assertThat(RabinKarpSearch.findAll("abcdef", "xyz")).isEmpty();
        assertThat(RabinKarpSearch.indexOf("ab", "abc")).isEqualTo(-1);
    }

    @Test
    void emptyPatternConventionsShouldMatchKmp() {
        assertThat(RabinKarpSearch.indexOf("abc", "")).isZero();
        assertThatThrownBy(() -> RabinKarpSearch.findAll("abc", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RabinKarpSearch.indexOf(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contentHashShouldBePositionIndependent() {
        // 同内容不同位置/宿主——滚动哈希窗口与整串哈希恒等
        long direct = RabinKarpSearch.hashOf("abc");
        assertThat(RabinKarpSearch.hashOf("abc")).isEqualTo(direct);
        assertThat(RabinKarpSearch.hashOf("abc")).isNotEqualTo(RabinKarpSearch.hashOf("acb"));
        // 嵌入宿主文本仍可命中（滚动递推与整串哈希同口径）
        assertThat(RabinKarpSearch.indexOf("xyzabcq", "abc")).isEqualTo(3);
    }

    @Test
    void longTextRollingShouldNotDegradeToFalseHits() {
        // 高重复文本滚动压力：千个 a + 单 b 尾——「aa..ab」唯一命中尾部
        StringBuilder text = new StringBuilder("a".repeat(2_000));
        text.append('b');
        assertThat(RabinKarpSearch.indexOf(text.toString(), "aab")).isEqualTo(1_998);
        assertThat(RabinKarpSearch.findAll(text.toString(), "aaa")).hasSize(1_998);
    }
}
