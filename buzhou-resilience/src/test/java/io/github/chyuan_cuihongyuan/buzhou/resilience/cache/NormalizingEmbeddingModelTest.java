package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 804 / T1110：L2 归一装饰器回归——单位范数/点积等价余弦/已归一跳算/
 * 零向量透传/维度与 index 保持/计数口径/fail-fast。
 */
class NormalizingEmbeddingModelTest {

    /** 固定向量桩：逐条返回预设向量。 */
    private static EmbeddingModel stub(float[]... vectors) {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> out = new java.util.ArrayList<>();
                int i = 0;
                for (String ignored : request.getInstructions()) {
                    out.add(new Embedding(vectors[Math.min(i, vectors.length - 1)], i++));
                }
                return new EmbeddingResponse(out);
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return vectors[0];
            }
        };
    }

    private static double norm(float[] v) {
        double n = 0;
        for (float x : v) {
            n += x * x;
        }
        return Math.sqrt(n);
    }

    @Test
    void outputIsUnitNormAndIndexPreserved() {
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(
                stub(new float[]{3, 4}, new float[]{1, 1, 1}));
        EmbeddingResponse response = model.call(new EmbeddingRequest(List.of("a", "b"), null));

        assertThat(norm(response.getResults().get(0).getOutput())).isCloseTo(1.0, within(1e-6));
        assertThat(response.getResults().get(0).getOutput()[0]).isCloseTo(0.6f, within(0.001f));
        assertThat(response.getResults().get(1).getOutput()[0]).isCloseTo(0.5774f, within(0.001f));
        assertThat(response.getResults().get(0).getIndex()).isZero();
        assertThat(response.getResults().get(1).getIndex()).isEqualTo(1);
        assertThat(model.normalizedCount()).isEqualTo(2);
        assertThat(model.zeroNormCount()).isZero();
    }

    @Test
    void cosineEqualsDotProductAfterNormalization() {
        float[] a = {2, 0, 1};
        float[] b = {0, 3, 4};
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(stub(a, b));
        EmbeddingResponse response = model.call(new EmbeddingRequest(List.of("a", "b"), null));
        float[] na = response.getResults().get(0).getOutput();
        float[] nb = response.getResults().get(1).getOutput();

        double dot = 0;
        for (int i = 0; i < na.length; i++) {
            dot += na[i] * nb[i];
        }
        assertThat(dot).isCloseTo(io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider.cosine(a, b),
                within(1e-6));
    }

    @Test
    void alreadyUnitSkipsRenormalizeButCounts() {
        float[] unit = {(float) Math.sqrt(0.5), (float) Math.sqrt(0.5)};
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(stub(unit));
        EmbeddingResponse response = model.call(new EmbeddingRequest(List.of("a"), null));
        assertThat(response.getResults().get(0).getOutput()).containsExactly(unit);
        assertThat(model.alreadyUnitCount()).isEqualTo(1);
        assertThat(model.normalizedCount()).isZero();
    }

    @Test
    void zeroVectorPassesThrough() {
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(stub(new float[]{0, 0, 0}));
        EmbeddingResponse response = model.call(new EmbeddingRequest(List.of("a"), null));
        assertThat(response.getResults().get(0).getOutput()).containsExactly(0f, 0f, 0f);
        assertThat(model.zeroNormCount()).isEqualTo(1);
    }

    @Test
    void embedDocumentDelegatesAndNormalizes() {
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(stub(new float[]{0, 5}));
        float[] out = model.embed(new org.springframework.ai.document.Document("x"));
        assertThat(norm(out)).isCloseTo(1.0, within(1e-6));
        assertThat(out[1]).isEqualTo(1.0f);
    }

    @Test
    void inputVectorsNotMutated() {
        float[] original = {3, 4};
        NormalizingEmbeddingModel model = new NormalizingEmbeddingModel(stub(original));
        model.call(new EmbeddingRequest(List.of("a"), null));
        assertThat(original).containsExactly(3f, 4f); // 输入副本语义
    }

    @Test
    void failFastOnNullDelegate() {
        assertThatThrownBy(() -> new NormalizingEmbeddingModel(null))
                .isInstanceOf(NullPointerException.class);
    }
}
