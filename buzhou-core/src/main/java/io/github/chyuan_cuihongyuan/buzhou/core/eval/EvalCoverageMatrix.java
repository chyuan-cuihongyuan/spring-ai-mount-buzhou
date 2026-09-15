package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 评测集覆盖矩阵读面（L 会话 1700 系 R3 = effort #1702 / spec 1702 /
 * 票 T2605 + T2606 / impl 1302）——JaCoCo / Stryker 覆盖矩阵思想 +
 * scikit-learn 信息熵：「测了什么」先于「测得怎样」——评测集对能力标签的
 * 计数分布与均衡度（归一化香农熵）显形偏科。
 *
 * <p>纯函数零状态：吃每个评测项的标签集，吐标签→用例数矩阵、零覆盖漏测
 * 清单（相对宿主声明的宇宙）、归一化熵 0..1（1=完美均衡）。
 *
 * @since 1.0.0
 */
public final class EvalCoverageMatrix {

    private EvalCoverageMatrix() {
    }

    /**
     * @param itemCount      评测项个数
     * @param labelCounts    标签→覆盖该标签的用例数（字典序）
     * @param distinctLabels 出现过的不同标签数
     */
    public record CoverageReport(int itemCount, Map<String, Integer> labelCounts,
                                 int distinctLabels) {

        /** 宇宙中零覆盖的标签（字典序）——漏测清单。 */
        public List<String> missingFrom(Set<String> universe) {
            List<String> missing = new ArrayList<>();
            for (String label : new TreeSet<>(universe)) {
                if (!labelCounts.containsKey(label)) {
                    missing.add(label);
                }
            }
            return List.copyOf(missing);
        }

        /** 归一化香农熵 0..1（按 ln k 归一；k≤1 记 0）——1=均衡，趋 0=偏科。 */
        public double shannonEntropy() {
            int k = labelCounts.size();
            if (k <= 1 || itemCount == 0) {
                return 0d;
            }
            double entropy = 0d;
            for (int count : labelCounts.values()) {
                double p = (double) count / itemCount;
                entropy -= p * Math.log(p);
            }
            return entropy / Math.log(k);
        }
    }

    /** 矩阵入口：每项一个标签集（空集合法——无标签项计入 item 数）。 */
    public static CoverageReport build(List<Set<String>> itemLabelSets) {
        Map<String, Integer> counts = new TreeMap<>();
        int items = 0;
        for (Set<String> labels : itemLabelSets == null ? List.<Set<String>>of() : itemLabelSets) {
            items++;
            for (String label : labels) {
                counts.merge(label, 1, Integer::sum);
            }
        }
        return new CoverageReport(items, Map.copyOf(counts), counts.size());
    }
}
