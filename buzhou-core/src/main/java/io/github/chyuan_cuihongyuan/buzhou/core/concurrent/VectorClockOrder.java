package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Map;

/**
 * 向量时钟偏序比较（spec 1840 / T2881 / impl 1441）——Dynamo 向量时钟 /
 * Lamport happens-before 思想：无中心时钟下判两个事件的因果序——**一方
 * 各分量 ≤ 另一方且至少一分量严格小**即因果先序（BEFORE/AFTER）；互相
 * 各有领先分量即**并发**（CONCURRENT——无因果关系的真冲突，冲突解的
 * 判定前提）。时间戳比较给不出并发判定，向量时钟给得出。
 *
 * <p>纯函数零状态、只判序不合并（时钟合并归宿主）。
 */
public final class VectorClockOrder {

    private VectorClockOrder() {
    }

    /** 因果三态：BEFORE 因果先 / AFTER 因果后 / CONCURRENT 并发冲突。 */
    public enum CausalOrder {

        /** a 因果先于 b（a 各分量 ≤ b 且至少一严格小）。 */
        BEFORE,

        /** a 因果后于 b。 */
        AFTER,

        /** 并发——无因果关系，冲突解候选。 */
        CONCURRENT
    }

    /**
     * 偏序比较入口。null 按空表；分量负值 fail-fast；语义：对两时钟并集
     * 键域逐分量比（缺席分量按 0——时钟稀疏表示合法）。
     */
    public static CausalOrder compare(Map<String, Long> a, Map<String, Long> b) {
        Map<String, Long> left = a == null ? Map.of() : a;
        Map<String, Long> right = b == null ? Map.of() : b;
        validate(left, "a");
        validate(right, "b");
        boolean leftStrictlyLess = false;
        boolean rightStrictlyLess = false;
        for (String key : unionKeys(left, right)) {
            long lv = left.getOrDefault(key, 0L);
            long rv = right.getOrDefault(key, 0L);
            if (lv < rv) {
                leftStrictlyLess = true;
            } else if (lv > rv) {
                rightStrictlyLess = true;
            }
        }
        if (leftStrictlyLess && rightStrictlyLess) {
            return CausalOrder.CONCURRENT;
        }
        if (leftStrictlyLess) {
            return CausalOrder.BEFORE;
        }
        if (rightStrictlyLess) {
            return CausalOrder.AFTER;
        }
        // 全相等：同一时点——按因果先序的退化处理（BEFORE 语义上「不后于」）
        return CausalOrder.BEFORE;
    }

    private static java.util.Set<String> unionKeys(Map<String, Long> left,
                                                   Map<String, Long> right) {
        java.util.Set<String> keys = new java.util.HashSet<>(left.keySet());
        keys.addAll(right.keySet());
        return keys;
    }

    private static void validate(Map<String, Long> clock, String name) {
        for (Map.Entry<String, Long> e : clock.entrySet()) {
            if (e.getValue() == null || e.getValue() < 0) {
                throw new IllegalArgumentException(String.format(
                        "时钟 %s 分量非法：key=%s, value=%s（要求非 null 且 ≥ 0）",
                        name, e.getKey(), e.getValue()));
            }
        }
    }
}
