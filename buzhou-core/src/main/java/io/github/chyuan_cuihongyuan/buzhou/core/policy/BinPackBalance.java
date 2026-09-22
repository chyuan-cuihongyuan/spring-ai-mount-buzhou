package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Arrays;

/**
 * 装箱平衡（spec 1878 / T2957 / impl 1479）——Kubernetes/Mesos 的
 * bin-packing 调度语义：批任务按体积降序、逐个放进首个装得下的节点
 * （first-fit-decreasing）。最优装箱 NP-hard，FFD 拿
 * ≤ 11/9 OPT + 常数的近似——装箱数与浪费率事前可算，扩容预算与
 * 缩容收益有账可依。
 *
 * <p>纯函数零状态；同体积保持原序（确定性可回放）；只算不落位
 * （调度归执行层）。
 */
public final class BinPackBalance {

    private BinPackBalance() {
    }

    /** 装箱结果：用箱数 + 逐箱载荷（载荷为 0 的箱不出现——箱数即长度）。 */
    public record PackResult(int binsUsed, long[] loads) {
    }

    /**
     * first-fit-decreasing 装箱：体积降序（同体积保原序），逐个放入
     * 首个剩余容量足够的箱，放不下开新箱。契约：容量 ≥ 1、体积 ≥ 0、
     * items 非 null（空表合法 → 0 箱，fail-fast）。
     */
    public static PackResult pack(long[] itemSizes, long binCapacity) {
        if (itemSizes == null) {
            throw new IllegalArgumentException("itemSizes 不能为 null");
        }
        if (binCapacity < 1) {
            throw new IllegalArgumentException("binCapacity 不能小于 1：" + binCapacity);
        }
        for (long size : itemSizes) {
            if (size < 0) {
                throw new IllegalArgumentException("体积不能为负：" + size);
            }
            if (size > binCapacity) {
                throw new IllegalArgumentException(
                        "单件体积超过箱容量：" + size + " > " + binCapacity);
            }
        }
        int n = itemSizes.length;
        if (n == 0) {
            return new PackResult(0, new long[0]);
        }
        // 降序排序（稳定：同体积保原序——装箱顺序记录原下标重排）
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Long.compare(itemSizes[b], itemSizes[a]));
        long[] remaining = new long[n]; // 最多 n 箱（每件一箱）
        int bins = 0;
        long[] loads = new long[n];
        for (int idx : order) {
            long size = itemSizes[idx];
            int target = -1;
            for (int b = 0; b < bins; b++) {
                if (remaining[b] >= size) {
                    target = b;
                    break;
                }
            }
            if (target < 0) {
                target = bins++;
                remaining[target] = binCapacity;
            }
            remaining[target] -= size;
            loads[target] += size;
        }
        return new PackResult(bins, Arrays.copyOf(loads, bins));
    }

    /**
     * 装箱浪费率：1 − Σ载荷/(箱数×容量)（0 = 无碎片）。契约：容量 ≥ 1、
     * loads 非空且逐项 ≥ 0（空表哨兵 0.0，fail-fast）。
     */
    public static double wasteRatio(long[] loads, long binCapacity) {
        if (binCapacity < 1) {
            throw new IllegalArgumentException("binCapacity 不能小于 1：" + binCapacity);
        }
        if (loads == null || loads.length == 0) {
            return 0.0;
        }
        long total = 0;
        for (long load : loads) {
            if (load < 0) {
                throw new IllegalArgumentException("载荷不能为负：" + load);
            }
            total += load;
        }
        long capacity = (long) loads.length * binCapacity;
        return (double) (capacity - total) / capacity;
    }
}
