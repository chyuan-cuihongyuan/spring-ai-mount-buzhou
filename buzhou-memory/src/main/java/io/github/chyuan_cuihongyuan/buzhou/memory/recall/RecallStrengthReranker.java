package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import io.github.chyuan_cuihongyuan.buzhou.memory.recall.MemoryStrengthScore.Weights;

/**
 * 检索结果强度重排器（spec 2018 / T3137 / impl 1569）——mem0 思想的
 * 管线落地：recall 召回按相关度排，但同相关度下「新近热访高重要」的
 * 记忆更该靠前——最终序 = 相关度与记忆强度的加权融合（开闭装饰，
 * 不侵入 RecallSearch 本体；元数据由调用方逐 Hit 供给）。
 *
 * <p>纯函数零状态、确定性；排序稳定（stream sorted 稳定——同融合分
 * 保原相关度序）。
 */
public final class RecallStrengthReranker {

    /** 默认相关度权重（0.7——相关度为主，强度微调同分段次序）。 */
    public static final double DEFAULT_RELEVANCE_WEIGHT = 0.7d;

    /** 单条命中的强度元数据（由调用方台账供给）。 */
    public record StrengthMeta(long millisSinceLastAccess, long accessCount, double importance) {
    }

    private RecallStrengthReranker() {
    }

    /**
     * 重排：finalScore = relevanceWeight × hit.score + (1−relevanceWeight)
     * × MemoryStrengthScore.score(meta)。TIME 模式 hit（score 恒 1）同样
     * 适用（退化为强度序）。metaOf 返 null 视为零强度（旧冷记忆降权）。
     * 契约：hits 非 null、relevanceWeight ∈ [0,1]（fail-fast）。
     *
     * @param hits            召回结果（相关度序）
     * @param metaOf          逐命中强度元数据供给（null = 全零强度）
     * @param weights         三分量权重（null = 默认 0.5/0.3/0.2）
     * @param relevanceWeight 相关度权重
     */
    public static List<RecallSearch.Hit> rerank(List<RecallSearch.Hit> hits,
                                                Function<RecallSearch.Hit, StrengthMeta> metaOf,
                                                Weights weights,
                                                double relevanceWeight) {
        if (hits == null) {
            throw new IllegalArgumentException("hits 不能为 null");
        }
        if (!(relevanceWeight >= 0) || relevanceWeight > 1 || Double.isNaN(relevanceWeight)) {
            throw new IllegalArgumentException("relevanceWeight 须在 [0,1]：" + relevanceWeight);
        }
        record Fused(RecallSearch.Hit hit, double score) {
        }
        return hits.stream()
                .map(hit -> new Fused(hit, fuse(hit, metaOf, weights, relevanceWeight)))
                .sorted(Comparator.comparingDouble((Fused f) -> f.score).reversed())
                .map(f -> f.hit)
                .toList();
    }

    /** 默认权重便捷口径。 */
    public static List<RecallSearch.Hit> rerank(List<RecallSearch.Hit> hits,
                                                Function<RecallSearch.Hit, StrengthMeta> metaOf) {
        return rerank(hits, metaOf, null, DEFAULT_RELEVANCE_WEIGHT);
    }

    private static double fuse(RecallSearch.Hit hit,
                               Function<RecallSearch.Hit, StrengthMeta> metaOf,
                               Weights weights, double relevanceWeight) {
        StrengthMeta meta = metaOf == null ? null : metaOf.apply(hit);
        double strength = meta == null ? 0.0d
                : MemoryStrengthScore.score(meta.millisSinceLastAccess(),
                        meta.accessCount(), meta.importance(), weights);
        return relevanceWeight * hit.score() + (1.0d - relevanceWeight) * strength;
    }
}
