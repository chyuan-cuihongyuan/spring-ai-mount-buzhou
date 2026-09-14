package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.List;

/**
 * 评估数据集质量审计（spec 1437 / T2171 兄弟轮编号见台账：R38 = effort #1438 /
 * 票 T2177 + T2178 / impl 1090）——Cleanlab 数据质量（近重复 H 847 之外的
 * 另一轴：**退化条目**）思想：空 input、空 expected、超短 input（< 最小信息量）
 * 的条目让评估分数虚高或虚低——「这个数据集还能信吗」从逐条翻看变成一行审计。
 *
 * <p>纯函数零状态：吃 {@link EvalItem} 列表（数据集全量），输出退化计数 +
 * 输入长度统计（P50/P95 直读不排序堆——小样本线性选择）+ 质量比。
 * 与 DatasetNearDuplicateStats（重复轴）、DatasetExpectations（期望格式轴）
 * 三者辨义：本轴是**退化/信息量**。
 */
public final class DatasetQualityAudit {

    /** 短输入阈值（字符数；低于此视为信息量不足）。 */
    public static final int SHORT_INPUT_THRESHOLD = 8;

    private DatasetQualityAudit() {
    }

    /**
     * @param totalItems   条目总数
     * @param emptyInputs  空/空白 input 计数
     * @param emptyExpecteds 空/空白 expected 计数
     * @param shortInputs  短 input 计数（非空但 &lt;{@link #SHORT_INPUT_THRESHOLD} 字符）
     * @param inputLengthP50 输入长度 P50（字符；空集 0）
     * @param inputLengthP95 输入长度 P95（字符；空集 0）
     */
    public record QualityReport(int totalItems, int emptyInputs, int emptyExpecteds,
                                int shortInputs, int inputLengthP50,
                                int inputLengthP95) {

        /** 退化比 = (空 input + 空 expected) / total（0 条目哨兵 -1）。 */
        public double degenerateRatio() {
            return totalItems == 0 ? -1d
                    : (double) (emptyInputs + emptyExpecteds) / totalItems;
        }
    }

    /** 审计入口：数据集条目列表。 */
    public static QualityReport analyze(List<EvalItem> items) {
        if (items == null || items.isEmpty()) {
            return new QualityReport(0, 0, 0, 0, 0, 0);
        }
        int emptyInputs = 0;
        int emptyExpecteds = 0;
        int shortInputs = 0;
        int[] lengths = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            EvalItem item = items.get(i);
            String input = item.input();
            String expected = item.expected();
            boolean emptyIn = input == null || input.isBlank();
            boolean emptyEx = expected == null || expected.isBlank();
            if (emptyIn) {
                emptyInputs++;
            } else if (input.length() < SHORT_INPUT_THRESHOLD) {
                shortInputs++;
            }
            if (emptyEx) {
                emptyExpecteds++;
            }
            lengths[i] = emptyIn ? 0 : input.length();
        }
        java.util.Arrays.sort(lengths);
        return new QualityReport(items.size(), emptyInputs, emptyExpecteds,
                shortInputs,
                percentile(lengths, 0.50), percentile(lengths, 0.95));
    }

    /** 升序秩插值分位（R-7 近似；空数组 0）。 */
    private static int percentile(int[] sorted, double q) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = (int) Math.ceil(q * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
