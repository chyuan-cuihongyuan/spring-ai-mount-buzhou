package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 事实合并决策分布（spec 829 / T1159，mem0 冲突解决统计扩散；829 对账管线
 * 的分布面）：摘要对账时每段三分决策——CREATED（新段首建）/ KEPT（沿用
 * 旧文）/ SUPERSEDED（新文替换旧文）——各段决策偏好与替换率分布。
 * 「改写激进还是保守」从感觉变比例。
 *
 * <p>纯记账：{@code SummarySection}×{@code Decision} 皆闭集（9×3 天然有界）；
 * synchronized 计数；snapshot 只出被触碰过的段（声明序）。喂点=对账管线
 * 装配侧（EVENT_RECONCILED 消费者），不改 reconcile 行为。
 */
public final class FactMergeDecisionDistribution {

    /** 三分决策。 */
    public enum Decision { CREATED, KEPT, SUPERSEDED }

    /** 单段行（未触碰的段不出现在报告中）。 */
    public record SectionRow(SummarySection section, long created, long kept, long superseded) {
    }

    /** 不可变报告。 */
    public record Report(long total, long created, long kept, long superseded,
                         double supersededRatio, List<SectionRow> sections) {
    }

    private final Map<SummarySection, long[]> bySection = new EnumMap<>(SummarySection.class);
    private long total;
    private long created;
    private long kept;
    private long superseded;

    /** 记录一次段级决策（null 忽略）。 */
    public synchronized void record(SummarySection section, Decision decision) {
        if (section == null || decision == null) {
            return;
        }
        long[] counts = bySection.computeIfAbsent(section, s -> new long[3]);
        switch (decision) {
            case CREATED -> { counts[0]++; created++; }
            case KEPT -> { counts[1]++; kept++; }
            case SUPERSEDED -> { counts[2]++; superseded++; }
        }
        total++;
    }

    /** 只读快照（段按枚举声明序；supersededRatio 总替换率）。 */
    public synchronized Report snapshot() {
        List<SectionRow> rows = new ArrayList<>(bySection.size());
        for (Map.Entry<SummarySection, long[]> e : bySection.entrySet()) {
            long[] c = e.getValue();
            rows.add(new SectionRow(e.getKey(), c[0], c[1], c[2]));
        }
        double ratio = total == 0 ? 0 : (double) superseded / total;
        return new Report(total, created, kept, superseded, ratio, List.copyOf(rows));
    }
}
