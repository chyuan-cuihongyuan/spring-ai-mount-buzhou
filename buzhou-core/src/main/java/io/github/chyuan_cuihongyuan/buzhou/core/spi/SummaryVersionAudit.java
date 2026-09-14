package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * impl-694 续 / spec 952：摘要版本链缺口审计（Kafka log gap / consumer offset
 * 对账思想——版本链非连续 = 有摘要被外部删除或写入失败，审计连续性信号）。
 *
 * <p>纯函数零 IO；不改动 {@link SummaryStore} 接口（default 方法会破坏既有
 * 实现二进制兼容——诚实划界）。k 次输入由宿主从 store 读出后喂入。
 */
public final class SummaryVersionAudit {

    private SummaryVersionAudit() {
    }

    /**
     * 按版本升序扫描摘要链，返回缺失的版本号（相邻 version 之间跳过的号，
     * 升序）。例如输入 versions = [1, 2, 5] → 缺失 [3, 4]。
     *
     * @param summaries 摘要列表（非 null；version ≤ 0 / 同 version 重复 fail-fast）
     */
    public static List<Long> gaps(List<StructuredSummary> summaries) {
        if (summaries == null) {
            throw new IllegalArgumentException("summaries 必须非空");
        }
        List<StructuredSummary> sorted = new ArrayList<>(summaries);
        sorted.sort(Comparator.comparingLong(StructuredSummary::version));

        List<Long> missing = new ArrayList<>();
        long expected = 1;
        for (StructuredSummary summary : sorted) {
            long version = summary.version();
            if (version <= 0) {
                throw new IllegalArgumentException("version 必须为正，收到 " + version);
            }
            if (seenDuplicate(sorted, version)) {
                throw new IllegalArgumentException("version 重复：" + version
                        + "（同 version 多条 = 存储唯一性被破坏）");
            }
            while (expected < version) {
                missing.add(expected);
                expected++;
            }
            expected = version + 1;
        }
        return List.copyOf(missing);
    }

    private static boolean seenDuplicate(List<StructuredSummary> sorted, long version) {
        int count = 0;
        for (StructuredSummary s : sorted) {
            if (s.version() == version) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
