package io.github.chyuan_cuihongyuan.buzhou.memory.recall;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 检索命中排名读面（L 会话 1700 系 R26 = effort #1725 / spec 1725 /
 * 票 T2651 + T2652 / impl 1325）——Elasticsearch rank_eval / RecSys MRR
 * 思想：检索器（MultiQueryRetriever/RecallSearch）返回排序列表，**最终被
 * 使用的条目排在第几位**才是真排名质量——rank=1 完美，越靠后说明前排
 * 噪声越多，miss（没用任何检索结果）单独计。
 *
 * <p>实例面线程安全：`record(rank)`（1 基；≤0 视为 miss）逐次记账 +
 * `report()` 吐 MRR（mean reciprocal rank）/Hit@1/Hit@3/miss 占比。
 * 纯读面 opt-in，不改检索器。
 *
 * @since 1.0.0
 */
public final class RetrievalRankStats {

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong reciprocalSumMilli = new AtomicLong();
    private final AtomicLong hitAt1 = new AtomicLong();
    private final AtomicLong hitAt3 = new AtomicLong();

    /** 记一次检索使用排名（rank≥1 命中；≤0 = miss 未用任何结果）。 */
    public void record(int rank) {
        if (rank >= 1) {
            hits.incrementAndGet();
            reciprocalSumMilli.addAndGet(1_000_000L / rank);
            if (rank == 1) {
                hitAt1.incrementAndGet();
            }
            if (rank <= 3) {
                hitAt3.incrementAndGet();
            }
        } else {
            misses.incrementAndGet();
        }
    }

    /**
     * @param hits      命中数（用了检索结果）
     * @param misses    未用结果数
     * @param mrr       平均倒数排名 Mean Reciprocal Rank（无命中哨兵 −1）
     * @param hitAt1    rank=1 次数
     * @param hitAt3    rank≤3 次数
     */
    public record RankReport(long hits, long misses, double mrr,
                             long hitAt1, long hitAt3) {
    }

    /** 快照。 */
    public RankReport report() {
        long hitCount = hits.get();
        double mrr = hitCount == 0
                ? -1d
                : reciprocalSumMilli.get() / 1_000_000.0d / hitCount;
        return new RankReport(hitCount, misses.get(), mrr,
                hitAt1.get(), hitAt3.get());
    }
}
