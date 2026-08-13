package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语义定位索引（wayfinder2 impl-18 / T46 / docs/spec/12 §spill-18，Letta archival 同构）：
 * durable/cold 层溢出时按既有切片边界异步 embed（hot-tail 不索引）；
 * {@link #locate} 返回 top-k chunk 条目（uri + offset + 摘要）——语义是<b>「定位」</b>、
 * byte/jsonpath/pagination 是<b>「取回」</b>，两段式组合（模型精读走 mode=bytes）。
 * 默认关（依赖 EmbeddingProvider；未注入即不可用）。
 */
public final class SemanticChunkIndex {

    /** 单个可定位切片：原文内偏移 + 长度 + 向量。 */
    public record Chunk(String uri, int offset, int length, String excerpt, float[] vector) {
    }

    /** locate 命中条目。 */
    public record Hit(String uri, int offset, int length, String excerpt, double score) {
    }

    private final EmbeddingProvider provider;
    private final Map<String, List<Chunk>> byUri = new ConcurrentHashMap<>();

    public SemanticChunkIndex(EmbeddingProvider provider) {
        this.provider = provider;
    }

    public boolean available() {
        return provider != null;
    }

    /** 索引一个溢出制品的切片（按既有边界；异步调用方负责线程）。 */
    public void index(String uri, List<int[]> boundaries, String content) {
        if (provider == null || uri == null || boundaries == null || content == null) {
            return;
        }
        List<Chunk> chunks = new ArrayList<>();
        for (int[] boundary : boundaries) {
            int start = Math.max(0, boundary[0]);
            int end = Math.min(content.length(), boundary[1]);
            if (end <= start) {
                continue;
            }
            String text = content.substring(start, end);
            chunks.add(new Chunk(uri, start, end - start,
                    excerptOf(text), provider.embed(text)));
        }
        byUri.put(uri, chunks);
    }

    /** 语义定位：跨全部已索引制品取 top-k（按 minScore 过滤，≤0 不滤）。 */
    public List<Hit> locate(String query, int k, double minScore) {
        if (provider == null || query == null || query.isBlank()) {
            return List.of();
        }
        float[] queryVector = provider.embed(query);
        return byUri.values().stream().flatMap(List::stream)
                .map(chunk -> new Hit(chunk.uri(), chunk.offset(), chunk.length(),
                        chunk.excerpt(), EmbeddingProvider.cosine(queryVector, chunk.vector())))
                .filter(hit -> minScore <= 0 || hit.score() >= minScore)
                .sorted(Comparator.comparingDouble(Hit::score).reversed())
                .limit(Math.max(1, k))
                .toList();
    }

    private static String excerptOf(String text) {
        String squeezed = text.strip().replaceAll("\\s+", " ");
        return squeezed.length() <= 120 ? squeezed : squeezed.substring(0, 120) + "…";
    }
}
