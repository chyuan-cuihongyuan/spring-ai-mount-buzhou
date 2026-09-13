package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 721 / T1042–T1043：嵌入超限分批——切块数/顺序/索引重排/直通/
 * 构造校验/异常透传。
 */
class ChunkingEmbeddingModelTest {

    /** 委托桩：记录每次请求的条数，返回向量=文本长度标记。 */
    private static class CountingDelegate implements EmbeddingModel {
        final List<Integer> batchSizes = new ArrayList<>();
        boolean boom = false;

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            if (boom) {
                throw new IllegalStateException("provider 崩了");
            }
            List<String> texts = request.getInstructions();
            batchSizes.add(texts.size());
            List<Embedding> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                results.add(new Embedding(new float[]{texts.get(i).length()}, i));
            }
            return new EmbeddingResponse(results);
        }

        @Override
        public float[] embed(Document document) {
            return new float[]{document.getText().length()};
        }
    }

    @Test
    void oversizedRequestIsSplitInOrderWithReindexedOutputs() {
        CountingDelegate delegate = new CountingDelegate();
        ChunkingEmbeddingModel chunking = new ChunkingEmbeddingModel(delegate, 2);
        EmbeddingResponse response = chunking.call(new EmbeddingRequest(
                List.of("a", "bb", "ccc", "dddd", "eeeee"), null));

        assertThat(delegate.batchSizes).containsExactly(2, 2, 1); // 5 条切块
        assertThat(response.getResults()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(response.getResults().get(i).getIndex()).isEqualTo(i); // 全局重排
        }
        assertThat(response.getResults().get(0).getOutput()).containsExactly(1f);
        assertThat(response.getResults().get(4).getOutput()).containsExactly(5f);
    }

    @Test
    void withinBudgetIsSinglePassThrough() {
        CountingDelegate delegate = new CountingDelegate();
        ChunkingEmbeddingModel chunking = new ChunkingEmbeddingModel(delegate, 8);
        chunking.call(new EmbeddingRequest(List.of("a", "bb", "ccc"), null));
        assertThat(delegate.batchSizes).containsExactly(3); // 直通
        // 单条也直通（不包一层）
        chunking.call(new EmbeddingRequest(List.of("a"), null));
        assertThat(delegate.batchSizes).containsExactly(3, 1);
    }

    @Test
    void validationAndExceptionPropagation() {
        assertThatThrownBy(() -> new ChunkingEmbeddingModel(new CountingDelegate(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        CountingDelegate boom = new CountingDelegate();
        boom.boom = true;
        ChunkingEmbeddingModel chunking = new ChunkingEmbeddingModel(boom, 2);
        assertThatThrownBy(() -> chunking.call(new EmbeddingRequest(
                List.of("a", "bb"), null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new ChunkingEmbeddingModel(new CountingDelegate(), 2)
                .embed(new Document("abc"))).containsExactly(3f);
    }

    @Test
    void defaultEmbedRoutesThroughCallForLists() {
        AtomicInteger calls = new AtomicInteger();
        CountingDelegate delegate = new CountingDelegate() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                calls.incrementAndGet();
                return super.call(request);
            }
        };
        ChunkingEmbeddingModel chunking = new ChunkingEmbeddingModel(delegate, 1);
        List<float[]> vectors = chunking.embed(List.of("a", "b"));
        assertThat(vectors).hasSize(2);
        assertThat(calls.get()).isEqualTo(2); // max=1 逐条
    }
}
