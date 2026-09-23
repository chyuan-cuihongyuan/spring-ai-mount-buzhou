package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.Collection;
import java.util.List;

/**
 * 推测执行裁决（spec 4026 / T6053 / impl 2127）——慢者复制竞争
 * 思想（Spark speculative execution）：同阶段任务里显著慢于中位
 （elapsed &gt; median×multiplier——用中位非均值，抗离群）且**进度
 * 仍落后**（progress &lt; threshold，Spark 默认 0.75）的任务启动
 * 副本竞争，先完成者胜——尾部等待被长尾任务的**机器性慢**（非
 * 数据性慢）拖死的病解；进度近完成的慢任务不折腾（副本大概率
 * 也赶不上，白烧资源）。
 *
 * <p>纯裁决件（真副本调度归执行器）。与 Hedged requests（网络
 * 对冲族）同思想不同面：彼管请求级延迟对冲、本管任务级进度
 * 中位对比。
 */
public final class SpeculativeStragglerPolicy {

    /** 任务读数（id + 已耗时 + 完成进度 0–1）。 */
    public record TaskStats(String id, long elapsedMillis, double progressFraction) {
    }

    private final double multiplier;
    private final double progressThreshold;

    /** 定构（multiplier≥1、progressThreshold∈(0,1] 否则 fail-fast）。 */
    public SpeculativeStragglerPolicy(double multiplier, double progressThreshold) {
        if (multiplier < 1.0 || progressThreshold <= 0 || progressThreshold > 1) {
            throw new IllegalArgumentException("multiplier≥1 / threshold∈(0,1]："
                    + multiplier + "/" + progressThreshold);
        }
        this.multiplier = multiplier;
        this.progressThreshold = progressThreshold;
    }

    /** 推测裁决：显著慢于同伴中位 ×multiplier 且进度落后于阈值。 */
    public boolean shouldSpeculate(TaskStats candidate, Collection<TaskStats> peers) {
        if (candidate == null || peers == null || peers.isEmpty()) {
            throw new IllegalArgumentException("candidate 非 null 且 peers 非空（中位要有参照系）");
        }
        if (candidate.elapsedMillis < 0 || candidate.progressFraction < 0
                || candidate.progressFraction > 1) {
            throw new IllegalArgumentException("elapsed≥0 / progress∈[0,1]");
        }
        return candidate.elapsedMillis > medianElapsed(peers) * multiplier
                && candidate.progressFraction < progressThreshold;
    }

    /** 同伴耗时中位（偶数取下中位——确定性）。 */
    static long medianElapsed(Collection<TaskStats> peers) {
        List<Long> elapsed = peers.stream().map(TaskStats::elapsedMillis).sorted().toList();
        return elapsed.get((elapsed.size() - 1) / 2);
    }

    /** multiplier 读数。 */
    public double multiplier() {
        return multiplier;
    }

    /** 进度阈值读数。 */
    public double progressThreshold() {
        return progressThreshold;
    }
}
