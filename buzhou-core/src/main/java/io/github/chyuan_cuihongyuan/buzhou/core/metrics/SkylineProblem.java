package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 天际线轮廓（spec 8049 / V8099 / impl 2351）——
 * 离散事件扫线经典思想（城市轮廓 LC218 同源）：**左端+高度
 * 入堆、右端出堆（TreeMap 计数延迟删除），关键高度变化才
 * 落点** O(n log n)——逐对建筑两两求并 O(n²)（楼数放大）
 * 的病解。关键点序列 [x,height] 含终尾归零点；同 x 取序
 * 固定（同输入同轮廓完全确定）；left<right/height>0/端点
 * 越域 fail-fast；整型坐标域明示。
 *
 * <p>与 SweepLineIntervals（spec 3013）同族不同面：区间
 * 并发峰值计数 vs 轮廓关键点几何。
 */
public final class SkylineProblem {

    private SkylineProblem() {
    }

    /** 轮廓关键点（[x,height] 交替；含终尾归零点；null/越域 fail-fast）。 */
    public static List<long[]> skyline(int[][] buildings) {
        if (buildings == null) {
            throw new IllegalArgumentException("楼表非空引用");
        }
        List<int[]> events = new ArrayList<>();
        for (int[] building : buildings) {
            int left = building[0];
            int right = building[1];
            int height = building[2];
            if (left >= right) {
                throw new IllegalArgumentException("left<right（" + left + "≥" + right + "）");
            }
            if (height <= 0) {
                throw new IllegalArgumentException("高度为正（实际 " + height + "）");
            }
            events.add(new int[]{left, height});
            events.add(new int[]{right, -height});
        }
        events.sort((a, b) -> {
            if (a[0] != b[0]) {
                return Integer.compare(a[0], b[0]);
            }
            return Integer.compare(b[1], a[1]);
        });
        TreeMap<Integer, Integer> heights = new TreeMap<>();
        heights.put(0, 1);
        List<long[]> keypoints = new ArrayList<>();
        int previousHeight = 0;
        for (int[] event : events) {
            int x = event[0];
            int height = event[1];
            if (height > 0) {
                heights.merge(height, 1, Integer::sum);
            } else {
                int removal = -height;
                int count = heights.get(removal);
                if (count == 1) {
                    heights.remove(removal);
                } else {
                    heights.put(removal, count - 1);
                }
            }
            int currentHeight = heights.lastKey();
            if (currentHeight != previousHeight) {
                if (!keypoints.isEmpty() && keypoints.get(keypoints.size() - 1)[0] == x) {
                    keypoints.get(keypoints.size() - 1)[1] = currentHeight;
                } else {
                    keypoints.add(new long[]{x, currentHeight});
                }
                previousHeight = currentHeight;
            }
        }
        return List.copyOf(keypoints);
    }
}
