package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 活动选择贪心（spec 7045 / U7291 / impl 2297）——最早结束
 * 时间贪心思想（算法导论经典，会议室排期同源）：**按结束
 * 时间升序，能兼容即选**——可证最优（交换论证：最优解的
 * 首活动可换为最早结束者）——全枚举子集 O(2^n) 的病解。
 * 并列结束按开始/编号 canonical（确定性）；单活动区间
 * 语义 [start,end)（start≥end fail-fast）。long 域。
 *
 * <p>与 IntervalTree（metrics）同族不同面：重叠查询结构 vs
 * 最大兼容子集选择。
 */
public final class ActivitySelectionGreedy {

    private ActivitySelectionGreedy() {
    }

    /** 最大兼容活动集（输入序号列表；null/倒置区间 fail-fast）。 */
    public static List<Integer> select(long[][] activities) {
        if (activities == null) {
            throw new IllegalArgumentException("活动集非空");
        }
        Integer[] order = new Integer[activities.length];
        for (int i = 0; i < activities.length; i++) {
            long[] activity = activities[i];
            if (activity == null || activity.length != 2) {
                throw new IllegalArgumentException("活动须为 [start,end]");
            }
            if (activity[0] >= activity[1]) {
                throw new IllegalArgumentException("区间倒置: " + Arrays.toString(activity));
            }
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            if (activities[a][1] != activities[b][1]) {
                return Long.compare(activities[a][1], activities[b][1]);
            }
            if (activities[a][0] != activities[b][0]) {
                return Long.compare(activities[a][0], activities[b][0]);
            }
            return Integer.compare(a, b);
        });
        List<Integer> selected = new ArrayList<>();
        long lastEnd = Long.MIN_VALUE;
        for (Integer index : order) {
            if (activities[index][0] >= lastEnd) {
                selected.add(index);
                lastEnd = activities[index][1];
            }
        }
        return selected;
    }

    /** 最大兼容数便捷面。 */
    public static int maxCount(long[][] activities) {
        return select(activities).size();
    }
}
