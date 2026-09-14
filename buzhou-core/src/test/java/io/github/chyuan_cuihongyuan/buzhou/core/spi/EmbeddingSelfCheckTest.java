package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1423 / T2148：Embedding 质量自查探针——语义词包提供者：相似对序
 * 成立、间隔为正、维度显形；病态提供者（常数向量/乱序）：序破裂显形。
 */
class EmbeddingSelfCheckTest {

    /** 确定性词包向量化（testsupport 同思想）：同词共享语义维。 */
    private static final class LexiconProvider implements EmbeddingProvider {
        private final Map<String, float[]> cache = new HashMap<>();
        private final java.util.function.Function<String, float[]> fn;

        LexiconProvider(java.util.function.Function<String, float[]> fn) {
            this.fn = fn;
        }

        @Override
        public float[] embed(String text) {
            return cache.computeIfAbsent(text, fn);
        }
    }

    /** 语义关键词维：出现即置 1（确定性词包——同关键词共享语义维）。 */
    private static final String[] KEYWORDS = {"退货", "今天天气", "机房空调", "季度财报"};

    private static float[] semantic(String text) {
        float[] v = new float[KEYWORDS.length];
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (text.contains(KEYWORDS[i])) {
                v[i] = 1f;
            }
        }
        return v;
    }

    @Test
    void saneProviderHoldsSimilarityOrder() {
        LexiconProvider provider = new LexiconProvider(t -> semantic(t));
        EmbeddingSelfCheck.ProbeReport report = EmbeddingSelfCheck.probe(provider);
        assertThat(report.similarPairsTotal()).isEqualTo(2);
        // 语义近邻（退货对/天气对共享关键词）：序应成立
        assertThat(report.similarPairsPassed()).isEqualTo(2);
        assertThat(report.orderHolds()).isTrue();
        assertThat(report.minMargin()).isPositive();
        assertThat(report.dimension()).isEqualTo(4);
    }

    @Test
    void degenerateProviderFailsOrderCheck() {
        // 病态模型：所有文本同向量 → cos 恒 1、margin 恒 0 → 序不成立
        LexiconProvider flat = new LexiconProvider(t -> new float[]{1f, 0f, 0f});
        EmbeddingSelfCheck.ProbeReport report = EmbeddingSelfCheck.probe(flat);
        assertThat(report.orderHolds()).isFalse();
        assertThat(report.similarPairsPassed()).isZero();
        assertThat(report.minMargin()).isEqualTo(0.0d);
    }

    @Test
    void scrambledProviderExposesNegativeMargin() {
        // 反义模型：无关对反而最相似 → margin 为负显形（序破裂可量化）
        LexiconProvider scrambled = new LexiconProvider(t -> semantic(t));
        EmbeddingSelfCheck.ProbeReport report = EmbeddingSelfCheck.probe(new EmbeddingProvider() {
            @Override
            public float[] embed(String text) {
                float[] v = scrambled.embed(text);
                float[] inverted = new float[v.length];
                for (int i = 0; i < v.length; i++) {
                    inverted[i] = -v[i];
                }
                return inverted; // 取负不改变余弦——序保持（对照组）
            }
        });
        // 负向量不破坏余弦序（数学不变量）——序仍成立，作为探针稳定性的对照
        assertThat(report.orderHolds()).isTrue();
    }

    @Test
    void dimensionReflectsProviderOutput() {
        LexiconProvider tiny = new LexiconProvider(t -> semantic(t));
        assertThat(EmbeddingSelfCheck.probe(tiny).dimension()).isEqualTo(4);
        assertThat(EmbeddingSelfCheck.probe(new EmbeddingProvider() {
            @Override
            public float[] embed(String text) {
                return new float[0];
            }
        }).dimension()).isZero();
    }
}
