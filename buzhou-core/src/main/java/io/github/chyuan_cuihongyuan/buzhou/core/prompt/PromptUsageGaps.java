package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 提示词使用缺口读数（spec 733 / T1064 族使用治理）：声明集 × 使用统计的
 * 差集——零使用提示词（清理候选）与孤儿统计（stats 有而 registry 无——
 * 注册表被清但统计残留的漂移信号）。
 *
 * <p>纯函数：两个既有面（PromptRegistry.names() × PromptUsageStats.snapshot()）
 * 的联合读数，不改任何一方。
 */
public final class PromptUsageGaps {

    /** 不可变报告（两清单字典序）。 */
    public record Report(List<String> unused, List<String> orphans) {
    }

    private PromptUsageGaps() {
    }

    /** 差集分析（任一参数 null fail-fast）。 */
    public static Report analyze(Set<String> declaredNames, List<PromptUsageStats.Row> rows) {
        Objects.requireNonNull(declaredNames, "declaredNames");
        Objects.requireNonNull(rows, "rows");
        Set<String> used = new TreeSet<>();
        Set<String> statsNames = new TreeSet<>();
        for (PromptUsageStats.Row row : rows) {
            if (row == null) {
                continue;
            }
            statsNames.add(row.name());
            if (declaredNames.contains(row.name()) && row.count() > 0) {
                used.add(row.name());
            }
        }
        List<String> unused = new ArrayList<>(declaredNames);
        unused.removeAll(used);
        java.util.Collections.sort(unused);
        List<String> orphans = new ArrayList<>(statsNames);
        orphans.removeAll(declaredNames);
        return new Report(List.copyOf(unused), List.copyOf(orphans));
    }
}
