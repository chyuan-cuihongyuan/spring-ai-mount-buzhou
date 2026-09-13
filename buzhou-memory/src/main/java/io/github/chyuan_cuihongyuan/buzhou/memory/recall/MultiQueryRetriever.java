package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 检索多路改写融合器（spec 803 / T1107，LangChain MultiQueryRetriever 借鉴）：
 * 原查询经 {@code variants} 生成器展开为 N 个改写变体（同义/视角/规范化），
 * 每个变体独立跑基础检索得到各排名，再按 RRF（k=60，Elasticsearch/605 同款）
 * 跨变体融合——词序微变/单一视角漏召的文档由其他变体兜住。
 *
 * <p>与既有融合面正交：{@code HybridSkillRanker}/RecallSearch HYBRID 融合
 * <b>单查询的双信号</b>（文本×向量）；本类融合<b>多查询的各自排名</b>。
 * 去重键 = 消息 id（多变体命中同一消息分数累加——是奖励不是重复）。
 * 变体数封顶 {@value #MAX_VARIANTS}（有界纪律）；生成器返回空/异常时回退
 * 原查询单路（fail-open 语义，javadoc 声明口径）。
 */
public final class MultiQueryRetriever {

    /** RRF 平滑常数（605/ES 同款 k=60）。 */
    public static final int RRF_K = 60;
    /** 变体数封顶（超出截断——防生成器失控）。 */
    public static final int MAX_VARIANTS = 8;

    /** 融合结果：RRF 降序 fused + 执行口径。 */
    public record Result(List<RecallSearch.Hit> fused, int variantsExecuted, int uniqueHits) {
    }

    private final Function<String, List<String>> variants;
    private final BiFunction<String, Integer, List<RecallSearch.Hit>> base;

    /**
     * @param variants 原查询 → 改写变体列表（是否含原查询由生成器决定；null/空回退单路原查询）
     * @param base     单路检索：（变体文本, limit）→ 排名降序命中（列表序即名次）
     */
    public MultiQueryRetriever(Function<String, List<String>> variants,
                               BiFunction<String, Integer, List<RecallSearch.Hit>> base) {
        this.variants = Objects.requireNonNull(variants, "variants");
        this.base = Objects.requireNonNull(base, "base");
    }

    /** 执行多路检索融合（limit ≤ 0 归默认 10）。 */
    public Result retrieve(String query, int limit) {
        int effectiveLimit = limit <= 0 ? 10 : limit;
        if (query == null || query.isBlank()) {
            return new Result(List.of(), 0, 0);
        }
        List<String> expanded;
        try {
            List<String> produced = variants.apply(query);
            expanded = produced == null ? List.of(query)
                    : produced.stream().filter(v -> v != null && !v.isBlank()).limit(MAX_VARIANTS).toList();
        } catch (RuntimeException e) {
            expanded = List.of(query); // 生成器故障 fail-open：单路原查询
        }
        if (expanded.isEmpty()) {
            expanded = List.of(query);
        }

        Map<String, double[]> acc = new LinkedHashMap<>(); // messageId → {rrfSum, bestRank}
        Map<String, RecallSearch.Hit> hits = new LinkedHashMap<>();
        for (String variant : expanded) {
            List<RecallSearch.Hit> ranked;
            try {
                ranked = base.apply(variant, effectiveLimit);
            } catch (RuntimeException e) {
                continue; // 单路故障不拖垮整体
            }
            if (ranked == null) {
                continue;
            }
            for (int rank = 0; rank < ranked.size(); rank++) {
                RecallSearch.Hit hit = ranked.get(rank);
                if (hit == null || hit.message() == null || hit.message().id() == null) {
                    continue;
                }
                double contribution = 1.0 / (RRF_K + rank + 1);
                double[] agg = acc.computeIfAbsent(hit.message().id(), k -> new double[2]);
                agg[0] += contribution;
                agg[1] = Math.min(agg[1] == 0 ? Integer.MAX_VALUE : agg[1], rank + 1);
                hits.putIfAbsent(hit.message().id(), hit);
            }
        }

        record Fused(String id, double rrf) {
        }
        List<Fused> ordered = new ArrayList<>();
        acc.forEach((id, agg) -> ordered.add(new Fused(id, agg[0])));
        ordered.sort(Comparator.comparingDouble(Fused::rrf).reversed());

        List<RecallSearch.Hit> fused = new ArrayList<>();
        for (Fused f : ordered) {
            if (fused.size() >= effectiveLimit) {
                break;
            }
            RecallSearch.Hit original = hits.get(f.id);
            fused.add(new RecallSearch.Hit(original.message(), original.turn(), f.rrf, "multi-rrf"));
        }
        return new Result(List.copyOf(fused), expanded.size(), acc.size());
    }
}
