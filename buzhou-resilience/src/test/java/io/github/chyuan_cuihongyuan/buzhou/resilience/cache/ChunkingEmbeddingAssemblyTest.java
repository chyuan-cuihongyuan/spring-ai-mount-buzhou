package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 723 / T1046–T1047：分批嵌入 yml 装配——record 6 组件绑定+兼容构造+
 * 默认关+装配包装生效。
 */
class ChunkingEmbeddingAssemblyTest {

    @Test
    void recordBindingAndCompatConstructors() {
        // 全量绑定
        ResilienceProperties.SemanticCache full = new ResilienceProperties.SemanticCache(
                Boolean.TRUE, 0.9, 16, Duration.ofHours(1), 5000, 8);
        assertThat(full.maxWeightChars()).isEqualTo(5000);
        assertThat(full.embeddingMaxBatch()).isEqualTo(8);
        // 5 参兼容（embeddingMaxBatch = 关）
        ResilienceProperties.SemanticCache five = new ResilienceProperties.SemanticCache(
                Boolean.TRUE, 0.9, 16, Duration.ofHours(1), 5000);
        assertThat(five.embeddingMaxBatch()).isZero();
        // 4 参兼容（全关）
        ResilienceProperties.SemanticCache four = new ResilienceProperties.SemanticCache(
                Boolean.TRUE, 0.9, 16, Duration.ofHours(1));
        assertThat(four.embeddingMaxBatch()).isZero();
        assertThat(four.maxWeightChars()).isZero();
    }

    @Test
    void negativeBatchFailsFast() {
        assertThatThrownBy(() -> new ResilienceProperties.SemanticCache(
                Boolean.TRUE, 0.9, 16, Duration.ofHours(1), 0, -4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assemblyWrapsEmbeddingWhenBatchDeclared() {
        ResilienceProperties.SemanticCache sc = new ResilienceProperties.SemanticCache(
                Boolean.TRUE, 0.9, 16, Duration.ofHours(1), 0, 4);
        AtomicInteger delegateCalls = new AtomicInteger();
        org.springframework.ai.embedding.EmbeddingModel delegate =
                new org.springframework.ai.embedding.EmbeddingModel() {
                    @Override
                    public org.springframework.ai.embedding.EmbeddingResponse call(
                            org.springframework.ai.embedding.EmbeddingRequest request) {
                        delegateCalls.incrementAndGet();
                        List<org.springframework.ai.embedding.Embedding> results = new java.util.ArrayList<>();
                        for (int i = 0; i < request.getInstructions().size(); i++) {
                            results.add(new org.springframework.ai.embedding.Embedding(
                                    new float[]{1f}, i));
                        }
                        return new org.springframework.ai.embedding.EmbeddingResponse(results);
                    }

                    @Override
                    public float[] embed(org.springframework.ai.document.Document document) {
                        return new float[]{1f};
                    }
                };
        ChunkingEmbeddingModel wrapped = new ChunkingEmbeddingModel(delegate, sc.embeddingMaxBatch());
        // 2 条 max=4 → 单次直通（wrapper 不额外切块）
        org.springframework.ai.embedding.EmbeddingResponse response = wrapped.call(
                new org.springframework.ai.embedding.EmbeddingRequest(
                        java.util.List.of("a", "b"), null));
        assertThat(response.getResults()).hasSize(2);
        assertThat(delegateCalls).hasValue(1);
    }
}
