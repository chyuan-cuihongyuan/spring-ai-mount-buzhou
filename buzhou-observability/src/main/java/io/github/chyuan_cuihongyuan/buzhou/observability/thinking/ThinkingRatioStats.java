package io.github.chyuan_cuihongyuan.buzhou.observability.thinking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 思考占比读面（L 会话 1700 系 R32 = effort #1731 / spec 1731 /
 * 票 T2663 + T2664 / impl 1331）——OpenAI o1 / DeepSeek-R1 的推理预算
 * 遥测思想：推理模型输出的思考段占比（thinking chars / total chars）
 * 是成本与延迟的直接驱动——占比多少、逐笔如何漂移，{@link ExtractedThinking}
 * 抽出了思考段，本面给占比账。
 *
 * <p>实例面线程安全：`record(thinkingChars, totalChars)`（totalChars≤0
 * 忽略）累计 +`report()` 吐样本数/总思考/总字符/累计占比/最近一笔占比。
 * 纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class ThinkingRatioStats {

    private final AtomicLong samples = new AtomicLong();
    private final AtomicLong totalThinkingChars = new AtomicLong();
    private final AtomicLong totalChars = new AtomicLong();
    private final AtomicLong lastRatioMilli = new AtomicLong(-1);

    /** 记一笔思考占比（totalChars≤0 忽略；thinking&gt;total 按截断诚实记账）。 */
    public void record(long thinkingChars, long totalChars) {
        if (totalChars <= 0) {
            return;
        }
        long thinking = Math.min(Math.max(thinkingChars, 0), totalChars);
        samples.incrementAndGet();
        totalThinkingChars.addAndGet(thinking);
        this.totalChars.addAndGet(totalChars);
        lastRatioMilli.set(thinking * 1000 / totalChars);
    }

    /**
     * @param samples           样本数
     * @param totalThinkingChars 思考字符累计
     * @param totalChars        总字符累计
     * @param cumulativeRatio   累计占比 0..1（无样本哨兵 −1）
     * @param lastRatio         最近一笔占比 0..1（无样本哨兵 −1）
     */
    public record ThinkingRatio(long samples, long totalThinkingChars, long totalChars,
                                double cumulativeRatio, double lastRatio) {
    }

    /** 快照。 */
    public ThinkingRatio report() {
        long chars = totalChars.get();
        if (chars == 0) {
            return new ThinkingRatio(0, 0, 0, -1d, -1d);
        }
        double cumulative = (double) totalThinkingChars.get() / chars;
        return new ThinkingRatio(samples.get(), totalThinkingChars.get(), chars,
                cumulative, lastRatioMilli.get() / 1000d);
    }
}
