package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 技能目录语义排序器（spec 59 §A / T265 / effort#19，默认关闭）：候选技能按
 * 「name + description」与当前问法的 embedding cosine 相似度降序——目录注入预算内保
 * 最相关技能（LiteLLM semantic routing 的相似度判定思想；Claude Code skills 的
 * 按需加载纪律下预算语义不变）。
 *
 * <p><b>向量缓存</b>：技能向量按 name 缓存（文本变更即失效）——常驻技能零重复嵌入；
 * 问法向量每轮一次。嵌入文本 = {@code name + "\n" + description}（技能自描述本体）。
 *
 * <p><b>诚实边界</b>（与语义缓存 spec 55 同口径）：排序判别力归嵌入模型——框架只保证
 * 排序稳定（并列保原序）、预算语义、失败回退。任一嵌入调用失败 → 整体回退原序 +
 * {@link #bypassCount()} 可观测（注入链路不因排序降级而断）。
 */
public final class SemanticSkillRanker {

    private final EmbeddingModel embeddingModel;
    private final ConcurrentHashMap<String, CachedVector> vectors = new ConcurrentHashMap<>();
    private final AtomicLong bypassed = new AtomicLong();

    private record CachedVector(String text, float[] vector) {
    }

    public SemanticSkillRanker(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** 降级回退次数（嵌入失败走原序——观测哨兵：非零持续增长 = 嵌入面异常）。 */
    public long bypassCount() {
        return bypassed.get();
    }

    /**
     * 排序（cosine 降序、并列保原序稳定）：hint 为 null/空或嵌入失败 → 原样返回。
     */
    public List<SkillMetadata> rank(List<SkillMetadata> candidates, String queryHint) {
        if (candidates == null || candidates.size() <= 1
                || queryHint == null || queryHint.isBlank()) {
            return candidates;
        }
        float[] queryVector;
        try {
            queryVector = embed(queryHint);
            if (isDegenerate(queryVector)) {
                return candidates; // 零向量：相似度无意义，原序（诚实降级）
            }
        } catch (RuntimeException e) {
            bypassed.incrementAndGet();
            return candidates;
        }
        record Scored(int index, double similarity) {
        }
        List<Scored> scored = new java.util.ArrayList<>(candidates.size());
        try {
            for (int i = 0; i < candidates.size(); i++) {
                SkillMetadata meta = candidates.get(i);
                float[] v = vectorFor(meta);
                scored.add(new Scored(i, isDegenerate(v) ? Double.NEGATIVE_INFINITY : cosine(queryVector, v)));
            }
        } catch (RuntimeException e) {
            bypassed.incrementAndGet();
            return candidates; // 技能向量失败：整体回退（半排半原的混合序不可预期，禁用）
        }
        scored.sort(Comparator.comparingDouble(Scored::similarity).reversed()
                .thenComparingInt(Scored::index)); // 相似度降序 + 原序稳定并列
        List<SkillMetadata> ranked = new java.util.ArrayList<>(candidates.size());
        for (Scored s : scored) {
            ranked.add(candidates.get(s.index()));
        }
        return ranked;
    }

    private float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /** 技能向量（缓存命中零嵌入；name 同键文本变更失效重嵌）。 */
    private float[] vectorFor(SkillMetadata meta) {
        String text = meta.name() + "\n" + (meta.description() == null ? "" : meta.description());
        String key = meta.name() == null ? "" : meta.name();
        CachedVector cached = vectors.get(key);
        if (cached != null && cached.text().equals(text)) {
            return cached.vector();
        }
        float[] vector = embed(text);
        vectors.put(key, new CachedVector(text, vector));
        return vector;
    }

    private static boolean isDegenerate(float[] v) {
        if (v == null || v.length == 0) {
            return true;
        }
        double norm = 0;
        for (float f : v) {
            norm += (double) f * f;
        }
        return norm == 0.0;
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            return Double.NEGATIVE_INFINITY; // 维度错配（嵌入模型变更）：防御性沉底
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
