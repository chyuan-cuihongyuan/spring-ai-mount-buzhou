package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 混合技能排序器（spec 605 / T860，weaviate/Qdrant hybrid search 的 RRF 融合借鉴；
 * opt-in）：语义（{@link SemanticSkillRanker}）与词法（{@link LexicalSkillRanker}）
 * 两路排序按 Reciprocal Rank Fusion 融合——{@code score = Σ w/(k + rank)}，
 * 免两路分数量纲对齐问题；精确词命中（型号/错误码/专有名词）补语义判别盲区，
 * 同义问法仍由语义面兜底。
 *
 * <p><b>降级</b>：语义路嵌入失败（{@link SemanticSkillRanker#bypassCount} 前后差）
 * → 纯词法序 + {@link #semanticFallbackCount()} 观测；两路都退化 → 原序。
 */
public final class HybridSkillRanker implements SkillRanker {

    /** RRF 平滑常数（Elasticsearch/标准实现同款 k=60）。 */
    private static final int RRF_K = 60;

    private final SemanticSkillRanker semantic;
    private final LexicalSkillRanker lexical;
    private final double semanticWeight;
    private final double lexicalWeight;
    private final java.util.concurrent.atomic.AtomicLong semanticFallbacks = new java.util.concurrent.atomic.AtomicLong();
    /** spec 744 / T1090：RRF 融合完成次数（两路齐备才计）。 */
    private final java.util.concurrent.atomic.AtomicLong fusedCount = new java.util.concurrent.atomic.AtomicLong();

    public HybridSkillRanker(SemanticSkillRanker semantic, LexicalSkillRanker lexical) {
        this(semantic, lexical, 1.0, 1.0);
    }

    /** 加权构造（权重 &gt; 0；1:1 等权为常用态）。 */
    public HybridSkillRanker(SemanticSkillRanker semantic, LexicalSkillRanker lexical,
            double semanticWeight, double lexicalWeight) {
        if (semantic == null || lexical == null) {
            throw new IllegalArgumentException("两路 ranker 均不可为空");
        }
        if (!(semanticWeight > 0) || !(lexicalWeight > 0)) {
            throw new IllegalArgumentException("权重必须为正（当前 " + semanticWeight + ":" + lexicalWeight + "）");
        }
        this.semantic = semantic;
        this.lexical = lexical;
        this.semanticWeight = semanticWeight;
        this.lexicalWeight = lexicalWeight;
    }

    /** 语义路降级次数（嵌入失败退化纯词法——观测哨兵）。 */
    public long semanticFallbackCount() {
        return semanticFallbacks.get();
    }

    /** spec 744 / T1090：融合权重读数（语义路）——声明生效确认面（638 同型）。 */
    public double semanticWeight() {
        return semanticWeight;
    }

    /** spec 744 / T1090：融合权重读数（词法路）。 */
    public double lexicalWeight() {
        return lexicalWeight;
    }

    /** spec 744 / T1090：两路信号齐备完成 RRF 融合的次数（单路降级不计）。 */
    public long fusedCount() {
        return fusedCount.get();
    }

    /** RRF 融合排序；hint 无效 → 原样返回。 */
    @Override
    public List<SkillMetadata> rank(List<SkillMetadata> candidates, String queryHint) {
        if (candidates == null || candidates.size() <= 1
                || queryHint == null || queryHint.isBlank()) {
            return candidates;
        }
        long before = semantic.bypassCount();
        List<SkillMetadata> semanticOrder = semantic.rank(candidates, queryHint);
        boolean semanticBypassed = semantic.bypassCount() > before;
        List<SkillMetadata> lexicalOrder = lexical.rank(candidates, queryHint);
        if (semanticBypassed) {
            semanticFallbacks.incrementAndGet();
            return lexicalOrder; // 语义降级：纯词法序
        }
        // RRF：两路名次倒数加权求和（名次 1 起）；scores 下标 = 原始候选集下标
        double[] scores = new double[candidates.size()];
        accumulate(semanticOrder, semanticWeight, scores, candidates);
        accumulate(lexicalOrder, lexicalWeight, scores, candidates);
        fusedCount.incrementAndGet(); // spec 744：两路齐备完成融合

        record Scored(int index, double score) {
        }
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            scored.add(new Scored(i, scores[i]));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed()
                .thenComparingInt(Scored::index)); // 并列保语义前的稳定序
        List<SkillMetadata> ranked = new ArrayList<>(candidates.size());
        for (Scored s : scored) {
            ranked.add(candidates.get(s.index()));
        }
        return ranked;
    }

    private static void accumulate(List<SkillMetadata> order, double weight, double[] scores,
            List<SkillMetadata> source) {
        for (int rank = 0; rank < order.size(); rank++) {
            scores[indexOfIdentity(source, order.get(rank))] += weight / (RRF_K + rank + 1);
        }
    }

    /** 引用身份定位原始下标（两路排序均重排同一候选集的引用）。 */
    private static int indexOfIdentity(List<SkillMetadata> source, SkillMetadata meta) {
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i) == meta) {
                return i;
            }
        }
        throw new IllegalStateException("混合排序候选集不一致（两路必须来自同一列表实例元素）");
    }
}
