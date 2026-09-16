package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2054 / T3210：n-gram 合同——滑窗序、去重保首现序、频次口径、
 * 短文本整段、词窗短语、空文本、畸形 fail-fast。
 */
class NgramExtractorTest {

    @Test
    void charNgramsShouldSlideInOrder() {
        assertThat(NgramExtractor.charNgrams("abcd", 2, false))
                .containsExactly("ab", "bc", "cd"); // 滑窗序
    }

    @Test
    void distinctModeShouldKeepFirstSeenOrder() {
        assertThat(NgramExtractor.charNgrams("abab", 2, true))
                .containsExactly("ab", "ba"); // 重复 ab 去掉保首现序
        assertThat(NgramExtractor.charNgrams("abab", 2, false))
                .containsExactly("ab", "ba", "ab"); // 频次口径保留
    }

    @Test
    void textShorterThanNShouldReturnWholeText() {
        assertThat(NgramExtractor.charNgrams("ab", 3, true))
                .containsExactly("ab"); // 不足窗——整段
    }

    @Test
    void emptyTextShouldReturnEmpty() {
        assertThat(NgramExtractor.charNgrams("", 2, true)).isEmpty();
        assertThat(NgramExtractor.wordNgrams("   ", 2, true)).isEmpty();
    }

    @Test
    void wordNgramsShouldBuildPhrases() {
        assertThat(NgramExtractor.wordNgrams("the quick brown fox", 2, false))
                .containsExactly("the quick", "quick brown", "brown fox");
        assertThat(NgramExtractor.wordNgrams("a b c", 3, false))
                .containsExactly("a b c");
    }

    @Test
    void fewerTokensThanNShouldReturnTokens() {
        assertThat(NgramExtractor.wordNgrams("hello world", 3, true))
                .containsExactly("hello", "world"); // 词数不足——原词
    }

    @Test
    void singleCharGramsAreCharacters() {
        assertThat(NgramExtractor.charNgrams("abc", 1, false))
                .containsExactly("a", "b", "c");
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> NgramExtractor.charNgrams(null, 2, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NgramExtractor.charNgrams("abc", 0, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NgramExtractor.wordNgrams(null, 2, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NgramExtractor.wordNgrams("a b", -1, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
