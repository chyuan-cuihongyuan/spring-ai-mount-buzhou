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
    // —— spec 1115 / impl 866：操作读面（搜索引擎索引/查询双计数思想；静态面理由
    // 同 R46–R117 先例）。coverageStats 为内容维度，本读面为操作维度。
    private static final java.util.concurrent.atomic.AtomicLong INDEX_CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong LOCATE_CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong SKIPPED_INVALID =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong CHUNKS_INDEXED =
            new java.util.concurrent.atomic.AtomicLong();

    /** 语义切片索引操作分布快照（spec 1115）。 */
    public record ChunkIndexOpStats(long indexCalls, long locateCalls,
                                    long skippedInvalid, long chunksIndexed) {
    }

    /** 只读快照。 */
    public static ChunkIndexOpStats stats() {
        return new ChunkIndexOpStats(INDEX_CALLS.get(), LOCATE_CALLS.get(),
                SKIPPED_INVALID.get(), CHUNKS_INDEXED.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        INDEX_CALLS.set(0);
        LOCATE_CALLS.set(0);
        SKIPPED_INVALID.set(0);
        CHUNKS_INDEXED.set(0);
    }

    public void index(String uri, List<int[]> boundaries, String content) {
        INDEX_CALLS.incrementAndGet();
        if (provider == null || uri == null || boundaries == null || content == null) {
            SKIPPED_INVALID.incrementAndGet();
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
        CHUNKS_INDEXED.addAndGet(chunks.size());
        byUri.put(uri, chunks);
    }

    /** 语义定位：跨全部已索引制品取 top-k（按 minScore 过滤，≤0 不滤）。 */
    public List<Hit> locate(String query, int k, double minScore) {
        LOCATE_CALLS.incrementAndGet();
        if (provider == null || query == null || query.isBlank()) {
            SKIPPED_INVALID.incrementAndGet();
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

    /**
     * 索引覆盖读面（spec 1447 / T2187）：已索引 uri 数 / 切片总数 / 单 uri
     * 最大切片数（最厚制品——切片数失衡=分块策略倾斜信号）。
     */
    public CoverageStats coverageStats() {
        int uris = byUri.size();
        int totalChunks = 0;
        int maxChunksPerUri = 0;
        String largestUri = null;
        for (Map.Entry<String, List<Chunk>> e : byUri.entrySet()) {
            int size = e.getValue().size();
            totalChunks += size;
            if (size > maxChunksPerUri) {
                maxChunksPerUri = size;
                largestUri = e.getKey();
            }
        }
        return new CoverageStats(uris, totalChunks, maxChunksPerUri, largestUri);
    }

    /**
     * @param indexedUries    已索引 uri 数
     * @param totalChunks     切片总数
     * @param maxChunksPerUri 单 uri 最大切片数（切片失衡信号）
     * @param largestUri      切片最多的 uri（null = 空索引）
     */
    public record CoverageStats(int indexedUries, int totalChunks,
                                int maxChunksPerUri, String largestUri) {
    }

    private static String excerptOf(String text) {
        String squeezed = text.strip().replaceAll("\\s+", " ");
        return squeezed.length() <= 120 ? squeezed : squeezed.substring(0, 120) + "…";
    }
}
