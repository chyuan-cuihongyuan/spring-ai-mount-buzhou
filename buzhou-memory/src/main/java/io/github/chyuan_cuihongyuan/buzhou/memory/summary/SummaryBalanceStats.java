package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.util.List;

/**
 * 摘要段落均衡读面（L 会话 1700 系 R28 = effort #1727 / spec 1727 /
 * 票 T2655 + T2656 / impl 1327）——文档结构均衡思想（Wikipedia 条目
 * 结构审查）：九段式摘要（{@link SummarySection}）某段独大/某段空壳时
 * 摘要质量退化——段落字符份额的失衡度显形偏科段。
 *
 * <p>纯函数零状态：`analyze(sectionLengths)` → `BalanceReport(sections/
 * totalChars/largestIndex/smallestIndex/imbalance)`。imbalance = 最大份额
 * ×段数（1=完美均衡，=段数即单段独大）；段数&lt;2 哨兵 −1。
 *
 * @since 1.0.0
 */
public final class SummaryBalanceStats {

    private SummaryBalanceStats() {
    }

    /**
     * @param sections      段数
     * @param totalChars    总字符数
     * @param largestIndex  最大段下标（入参序）
     * @param smallestIndex 最小段下标
     * @param imbalance     失衡度 maxShare×k（1=均衡，k=单段独大；段数&lt;2 −1）
     */
    public record BalanceReport(int sections, long totalChars,
                                int largestIndex, int smallestIndex, double imbalance) {
    }

    /** 审计入口：按段序的字符长度列表。 */
    public static BalanceReport analyze(List<Integer> sectionLengths) {
        List<Integer> data = sectionLengths == null ? List.of() : sectionLengths;
        int k = data.size();
        if (k < 2) {
            return new BalanceReport(k,
                    data.stream().mapToLong(Integer::longValue).sum(), -1, -1, -1d);
        }
        long total = data.stream().mapToLong(Integer::longValue).sum();
        int largest = 0;
        int smallest = 0;
        for (int i = 1; i < k; i++) {
            if (data.get(i) > data.get(largest)) {
                largest = i;
            }
            if (data.get(i) < data.get(smallest)) {
                smallest = i;
            }
        }
        double imbalance = total == 0
                ? 1d
                : (double) data.get(largest) / total * k;
        return new BalanceReport(k, total, largest, smallest, imbalance);
    }
}
