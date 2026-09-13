package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 嵌入 L2 归一化装饰器（spec 804 / T1109，sentence-transformers
 * {@code normalize_embeddings=True} 借鉴）：包装任意 {@link EmbeddingModel}，
 * 输出向量逐条归一到单位 L2 范数——cosine 相似度退化为点积（检索加速、
 * 尺度跨供应商一致），下游缓存/向量桶不再受原始范数漂移影响。
 *
 * <p>仅覆写 {@link #call(EmbeddingRequest)}（与 721 Chunking 同模式——default
 * 方法经 call 路由自动受益；{@link #embed(Document)} 同款直通委托保持 721
 * 先例）。已归一（‖v‖≈1，ε={@value #EPSILON} 内）跳过重算但计数；零向量
 * 不动（归一无定义——如实计数 zeroNorm）；维度不变。计数面只读。
 */
public final class NormalizingEmbeddingModel implements EmbeddingModel {

    /** 判定「已归一」的范数容差。 */
    public static final double EPSILON = 1e-4;

    private final EmbeddingModel delegate;
    private final AtomicLong normalized = new AtomicLong();
    private final AtomicLong alreadyUnit = new AtomicLong();
    private final AtomicLong zeroNorm = new AtomicLong();

    public NormalizingEmbeddingModel(EmbeddingModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        EmbeddingResponse response = delegate.call(request);
        List<Embedding> results = response.getResults();
        List<Embedding> out = new ArrayList<>(results.size());
        for (Embedding embedding : results) {
            float[] vector = embedding.getOutput();
            if (vector == null || vector.length == 0) {
                out.add(embedding);
                continue;
            }
            double norm = 0;
            for (float v : vector) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm < EPSILON) {
                zeroNorm.incrementAndGet();
                out.add(new Embedding(vector, embedding.getIndex()));
            } else if (Math.abs(norm - 1.0) < EPSILON) {
                alreadyUnit.incrementAndGet();
                out.add(new Embedding(vector.clone(), embedding.getIndex()));
            } else {
                normalized.incrementAndGet();
                float[] copy = new float[vector.length];
                for (int i = 0; i < vector.length; i++) {
                    copy[i] = (float) (vector[i] / norm);
                }
                out.add(new Embedding(copy, embedding.getIndex()));
            }
        }
        return new EmbeddingResponse(out);
    }

    @Override
    public float[] embed(Document document) {
        float[] vector = delegate.embed(document);
        return vector == null ? null : normalizeOne(vector);
    }

    private float[] normalizeOne(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (vector.length == 0 || norm < EPSILON) {
            zeroNorm.incrementAndGet();
            return vector;
        }
        if (Math.abs(norm - 1.0) < EPSILON) {
            alreadyUnit.incrementAndGet();
            return vector.clone();
        }
        normalized.incrementAndGet();
        float[] copy = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            copy[i] = (float) (vector[i] / norm);
        }
        return copy;
    }

    /** 已重归一计数（范数越界 ε）。 */
    public long normalizedCount() {
        return normalized.get();
    }

    /** 输入已单位化计数（跳过重算——审计口径非快路径承诺）。 */
    public long alreadyUnitCount() {
        return alreadyUnit.get();
    }

    /** 零/近零向量计数（归一无定义——原样透传）。 */
    public long zeroNormCount() {
        return zeroNorm.get();
    }
}
