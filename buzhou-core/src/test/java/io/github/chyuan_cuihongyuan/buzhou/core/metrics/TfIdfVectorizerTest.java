package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TfIdfVectorizerTest {

    @Test
    void shouldMatchSmoothedFormulaAnchor() {
        // 锚：两文档 ["a","b","a"] / ["c"]；idf = ln((1+2)/(1+df))+1
        TfIdfVectorizer vf = TfIdfVectorizer.of(List.of(
                List.of("a", "b", "a"), List.of("c")));
        assertThat(vf.vocabulary()).containsExactly("a", "b", "c");
        assertThat(vf.idf("a")).isCloseTo(Math.log(3.0 / 2.0) + 1.0, within(1e-12));
        assertThat(vf.idf("c")).isCloseTo(Math.log(3.0 / 2.0) + 1.0, within(1e-12));
        double[] v1 = vf.vector(List.of("a", "b", "a"));
        // tf(a)=2/3, tf(b)=1/3 → v = [2/3·idf(a), 1/3·idf(a), 0]
        assertThat(v1[0]).isCloseTo(2.0 / 3.0 * vf.idf("a"), within(1e-12));
        assertThat(v1[1]).isCloseTo(1.0 / 3.0 * vf.idf("b"), within(1e-12));
        assertThat(v1[2]).isZero();
        // 共现词压权圣像：全文档共有词 idf 最低
        TfIdfVectorizer vf2 = TfIdfVectorizer.of(List.of(
                List.of("common", "rare1"), List.of("common", "rare2")));
        assertThat(vf2.idf("common")).isLessThan(vf2.idf("rare1"));
        assertThat(vf2.idf("common")).isCloseTo(Math.log(3.0 / 3.0) + 1.0, within(1e-12));
    }

    @Test
    void shouldBehaveSanelyUnderCosine() {
        TfIdfVectorizer vf = TfIdfVectorizer.of(List.of(
                List.of("machine", "learning", "model", "training"),
                List.of("machine", "learning", "model", "inference"),
                List.of("cooking", "recipe", "kitchen")));
        double[] a = vf.vector(List.of("machine", "learning", "model", "training"));
        double[] b = vf.vector(List.of("machine", "learning", "model", "inference"));
        double[] c = vf.vector(List.of("cooking", "recipe", "kitchen"));
        assertThat(TfIdfVectorizer.cosineSimilarity(a, a)).isCloseTo(1.0, within(1e-12));
        // 同主题 > 跨主题；跨主题正交（无公共词）
        assertThat(TfIdfVectorizer.cosineSimilarity(a, b))
                .isGreaterThan(TfIdfVectorizer.cosineSimilarity(a, c));
        assertThat(TfIdfVectorizer.cosineSimilarity(a, c)).isCloseTo(0.0, within(1e-12));
        // 词表外词忽略（向量位序契约不破）
        double[] withUnknown = vf.vector(List.of("machine", "zzz-unknown"));
        assertThat(withUnknown).hasSize(vf.vocabulary().size());
        assertThat(TfIdfVectorizer.cosineSimilarity(a, a)).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        TfIdfVectorizer first = TfIdfVectorizer.of(List.of(List.of("x", "y"), List.of("y", "z")));
        TfIdfVectorizer second = TfIdfVectorizer.of(List.of(List.of("x", "y"), List.of("y", "z")));
        assertThat(first.vocabulary()).isEqualTo(second.vocabulary());
        assertThat(first.vector(List.of("x", "y"))).containsExactly(second.vector(List.of("x", "y")));
        assertThatThrownBy(() -> TfIdfVectorizer.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TfIdfVectorizer.of(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TfIdfVectorizer.of(java.util.Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TfIdfVectorizer.cosineSimilarity(new double[2], new double[3]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
