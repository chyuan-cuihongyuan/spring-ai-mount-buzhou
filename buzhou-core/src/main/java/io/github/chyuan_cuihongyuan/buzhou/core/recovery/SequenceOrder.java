package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 回绕序号比较（spec 2062 / T3225 / impl 1613）——TCP sequence number
 * 回绕语义思想：long 序号在长期运行后回绕（Long.MAX_VALUE → MIN），
 * 朴素 a &lt; b 比较在回绕点反转（新序号被判旧）；回绕安全比较用
 * **有符号差**（b − a 的符号即先后——半环内恒真：|差| &lt; 2⁶² 安
 * 全域内，回绕点两侧差值符号仍正确）。投递序列号 / 事件纪元序 /
 * 日志偏移的回绕安全底座。
 *
 * <p>纯函数零状态；安全域 |b − a| &lt; 2⁶²（超过即真序不可分——返
 * INCOMPARABLE 诚实而非臆答）。
 */
public final class SequenceOrder {

    /** 三态比较：前 / 后 / 不可分（超半环——真序未知）。 */
    public enum Order {
        BEFORE,
        AFTER,
        INCOMPARABLE
    }

    /** 安全半环：|差| < 2⁶²（即 Long.MAX_VALUE÷2 邻域——保守弃用 2⁶³ 全域）。 */
    public static final long SAFE_HALF_RANGE = Long.MAX_VALUE / 2;

    private SequenceOrder() {
    }

    /** a 是否在 b 前（回绕安全）。 */
    public static boolean isBefore(long a, long b) {
        return compare(a, b) == Order.BEFORE;
    }

    /** a 是否在 b 后（回绕安全）。 */
    public static boolean isAfter(long a, long b) {
        return compare(a, b) == Order.AFTER;
    }

    /**
     * 三态比较：diff = b − a（回绕安全有符号差）——|diff| 超安全半环
     * 即 INCOMPARABLE（诚实边界：相距太远的序号真序未知，不臆答）。
     */
    public static Order compare(long a, long b) {
        long diff = b - a; // 溢出回绕即语义——差符号仍判先后
        if (diff == 0) {
            return Order.INCOMPARABLE; // 等值退化口径——同序不可分先后
        }
        if (diff > SAFE_HALF_RANGE || diff < -SAFE_HALF_RANGE) {
            return Order.INCOMPARABLE;
        }
        return diff > 0 ? Order.BEFORE : Order.AFTER; // b 在前差正 → a BEFORE b
    }

    /**
     * 回绕安全距离：b − a 折算到 [−半环, +半环] 邻域（正 = a 前 b 后
     * 的间隔）；INCOMPARABLE 域返回 Long.MIN_VALUE 哨兵。
     */
    public static long forwardDistance(long a, long b) {
        long diff = b - a;
        if (diff > SAFE_HALF_RANGE || diff < -SAFE_HALF_RANGE) {
            return Long.MIN_VALUE;
        }
        return diff;
    }
}
