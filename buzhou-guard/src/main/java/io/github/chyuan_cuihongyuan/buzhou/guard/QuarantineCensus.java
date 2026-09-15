package io.github.chyuan_cuihongyuan.buzhou.guard;

import java.util.List;

/**
 * 隔离区普查（spec 1842 / T2885 / impl 1443）——邮件隔离区 / 恶意样本
 * 沙箱思想：可疑未定罪的内容（命中疑似规则的输入/工具输出）**暂存待审**
 * 而非即杀——误报有申诉出口、真阳有龄期上限。审查积压读数：待审最老
 * 龄期（该催审查了）与待审占比（隔离区拥堵度）——隔离区无人审 = 缓冲
 * 变黑洞。
 *
 * <p>纯函数零状态、只读不放行（审查动作归宿主）。
 */
public final class QuarantineCensus {

    private QuarantineCensus() {
    }

    /** 隔离条目契约：itemId 非空白、age ≥ 0。 */
    public record Quarantined(String itemId, String reason, long ageMillis,
                              boolean reviewed) {

        public Quarantined {
            if (itemId == null || itemId.isBlank() || ageMillis < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法隔离条目：id=%s, age=%d（要求 id 非空白且 age ≥ 0）",
                        itemId, ageMillis));
            }
        }
    }

    /**
     * @param items          条目总数
     * @param pendingReview  待审数（reviewed=false）
     * @param reviewedCount  已审数
     * @param oldestPendingAgeMillis 待审最老旧（无待审 -1 哨兵）
     */
    public record Census(int items, long pendingReview, long reviewedCount,
                         long oldestPendingAgeMillis) {

        /** 待审占比（空隔离区 -1 哨兵）。 */
        public double pendingRatio() {
            return items == 0 ? -1d : (double) pendingReview / items;
        }
    }

    /** 普查入口。null 按空表。 */
    public static Census census(List<Quarantined> entries) {
        List<Quarantined> window = entries == null ? List.of() : entries;
        long pending = 0;
        long reviewed = 0;
        long oldestPending = -1;
        for (Quarantined q : window) {
            if (q.reviewed()) {
                reviewed++;
            } else {
                pending++;
                oldestPending = Math.max(oldestPending, q.ageMillis());
            }
        }
        return new Census(window.size(), pending, reviewed, oldestPending);
    }
}
