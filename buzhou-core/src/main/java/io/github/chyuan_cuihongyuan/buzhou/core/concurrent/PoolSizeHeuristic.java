package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 连接池容量启发（spec 1888 / T2977 / impl 1489）——HikariCP wiki
 * 公式：pool size = cores × 2 + effective_spindles。反直觉结论：
 * 连接数超过线程可驱动数只会更慢（上下文切换 + 锁争用 + 数据库端
 * 内存）——部署期静态基线，与运行时自适应调优互补。
 *
 * <p>纯函数零状态、确定性。
 */
public final class PoolSizeHeuristic {

    private PoolSizeHeuristic() {
    }

    /**
     * 单节点最优池容量：cores×2 + spindles。SSD/纯网络存储磁轴数
     * ≈ 0。契约：cores ≥ 1、spindles ≥ 0（fail-fast）。
     */
    public static int optimalSize(int cores, int spindles) {
        if (cores < 1) {
            throw new IllegalArgumentException("cores 不能小于 1：" + cores);
        }
        if (spindles < 0) {
            throw new IllegalArgumentException("spindles 不能为负：" + spindles);
        }
        return cores * 2 + spindles;
    }

    /**
     * 全局预算按节点均衡拆分：floor 均分，余数摊给前几个节点（+1，
     * 不偏科）。契约：nodes ≥ 1、total ≥ nodes（每节点至少 1，
     * fail-fast）。
     */
    public static int[] splitBudget(int total, int nodes) {
        if (nodes < 1) {
            throw new IllegalArgumentException("nodes 不能小于 1：" + nodes);
        }
        if (total < nodes) {
            throw new IllegalArgumentException(String.format(
                    "total 须 ≥ nodes（每节点至少 1）：%d < %d", total, nodes));
        }
        int[] sizes = new int[nodes];
        int base = total / nodes;
        int remainder = total % nodes;
        for (int i = 0; i < nodes; i++) {
            sizes[i] = base + (i < remainder ? 1 : 0);
        }
        return sizes;
    }

    /**
     * 饱和度读数：active/size（≥ 0.9 预警候选惯例）。size=0 哨兵
     * 0.0。契约：active ≥ 0（fail-fast）。
     */
    public static double saturationRatio(int active, int size) {
        if (active < 0) {
            throw new IllegalArgumentException("active 不能为负：" + active);
        }
        if (size == 0) {
            return 0.0;
        }
        if (size < 0) {
            throw new IllegalArgumentException("size 不能为负：" + size);
        }
        return (double) active / size;
    }
}
