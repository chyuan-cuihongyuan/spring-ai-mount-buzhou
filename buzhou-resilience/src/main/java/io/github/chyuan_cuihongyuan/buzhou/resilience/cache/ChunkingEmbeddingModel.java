package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 嵌入超限分批装饰器（spec 721 / T1042，OpenAI embeddings 批量上限思想）：
 * 单请求输入数组超供应商 cap 即 400——本装饰器按 maxBatchSize 切块顺序调用
 * delegate、输出全局 index 重排拼接；≤max 直通零拷贝。
 *
 * <p>仅覆写 {@link #call(EmbeddingRequest)}：embed(String/List/Document) 等
 * default 方法经 call 路由自动受益。纯确定性（无时序窗口——并发合并是另一题）。
 */
public final class ChunkingEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;
    private final int maxBatchSize;

    public ChunkingEmbeddingModel(EmbeddingModel delegate, int maxBatchSize) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (maxBatchSize < 1) {
            throw new IllegalArgumentException("maxBatchSize 必须 >= 1（当前 " + maxBatchSize + "）");
        }
        this.maxBatchSize = maxBatchSize;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        if (instructions == null || instructions.size() <= maxBatchSize) {
            return delegate.call(request);
        }
        List<Embedding> merged = new ArrayList<>(instructions.size());
        int globalIndex = 0;
        for (int from = 0; from < instructions.size(); from += maxBatchSize) {
            List<String> chunk = instructions.subList(from, Math.min(from + maxBatchSize, instructions.size()));
            EmbeddingResponse part = delegate.call(new EmbeddingRequest(chunk, request.getOptions()));
            for (Embedding embedding : part.getResults()) {
                merged.add(new Embedding(embedding.getOutput(), globalIndex++));
            }
        }
        return new EmbeddingResponse(merged);
    }

    @Override
    public float[] embed(Document document) {
        return delegate.embed(document);
    }
}
