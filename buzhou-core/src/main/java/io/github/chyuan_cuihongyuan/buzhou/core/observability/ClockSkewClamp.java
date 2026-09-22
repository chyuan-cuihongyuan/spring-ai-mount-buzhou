package io.github.chyuan_cuihongyuan.buzhou.core.observability;

/**
 * 时钟偏斜校正（spec 1886 / T2973 / impl 1487）——Zipkin/Brave 的
 * span 偏斜钳位：子 span 开始于父之前物理不可能（采集端时钟漂移
 * 所致）。规则：负偏斜平移起点、时长保持；终点越出父终点则收缩
 * 时长（下限 0）；父区间为基准只动子。
 *
 * <p>纯函数零状态、确定性。
 */
public final class ClockSkewClamp {

    private ClockSkewClamp() {
    }

    /** 校正结果：钳位后区间 + 应用的平移偏斜量（毫秒，0 = 未平移）。 */
    public record ClampedSpan(long begin, long end, long skewApplied) {
    }

    /**
     * 钳位子区间到父区间内：①childBegin &lt; parentBegin → 平移
     * （时长保持）；②终点 &gt; parentEnd → 收缩到 parentEnd（时长
     * 下限 0）。契约：子/父区间各自 end ≥ begin（fail-fast）。
     */
    public static ClampedSpan clamp(long childBegin, long childEnd,
                                    long parentBegin, long parentEnd) {
        if (childEnd < childBegin) {
            throw new IllegalArgumentException(String.format(
                    "子区间倒置：%d > %d", childBegin, childEnd));
        }
        if (parentEnd < parentBegin) {
            throw new IllegalArgumentException(String.format(
                    "父区间倒置：%d > %d", parentBegin, parentEnd));
        }
        long skew = 0;
        long begin = childBegin;
        long end = childEnd;
        if (begin < parentBegin) {
            skew = parentBegin - begin;
            begin = parentBegin;
            end = childEnd + skew;
        }
        if (end > parentEnd) {
            end = parentEnd;
        }
        if (end < begin) {
            end = begin;
        }
        return new ClampedSpan(begin, end, skew);
    }

    /**
     * 偏斜读数：childBegin − parentBegin（负 = 子早于父——时钟漂移
     * 信号本身）。
     */
    public static long skewMillis(long childBegin, long parentBegin) {
        return childBegin - parentBegin;
    }
}
