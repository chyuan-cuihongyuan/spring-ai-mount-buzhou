package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 评测项轮换消序读面（L 会话 1700 系 R2 = effort #1701 / spec 1701 /
 * 票 T2603 + T2604 / impl 1301）——OpenAI Evals / HELM 的种子化评测顺序
 * 思想：评测项顺序本身携带信号（前项污染/位置敏感），跨 run 换序才能把
 * 「顺序效应」从能力波动里摊出去。
 *
 * <p>纯函数零状态：{@code permutation(n, runIndex)} 给确定性种子化
 * Fisher–Yates 置换——种子用算法规范固定的 {@link Random}（LCG，跨 JVM
 * 重现，区别于 ThreadLocalRandom）；{@code shuffled} 按置换重排新列表
 * （多重集守恒）。runIndex 递增 → 每轮不同序。
 *
 * @since 1.0.0
 */
public final class EvalOrderRotator {

    private EvalOrderRotator() {
    }

    /**
     * @param runIndex 轮次序号（同轮恒定 → 顺序可复现）
     */
    public record OrderPlan(int runIndex, List<Integer> permutation) {
    }

    /** 确定性置换：runIndex 派生种子的 Fisher–Yates（n≥0）。 */
    public static OrderPlan permutation(int n, int runIndex) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Random random = new Random(0x1700L + runIndex * 31L + n);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        List<Integer> permutation = new ArrayList<>(n);
        for (int v : order) {
            permutation.add(v);
        }
        return new OrderPlan(runIndex, List.copyOf(permutation));
    }

    /** 按置换重排的新列表（多重集不变；null/空/单项安全）。 */
    public static <T> List<T> shuffled(List<T> items, int runIndex) {
        List<T> data = items == null ? List.of() : items;
        int n = data.size();
        if (n < 2) {
            return List.copyOf(data);
        }
        List<Integer> permutation = permutation(n, runIndex).permutation();
        List<T> rotated = new ArrayList<>(n);
        for (int index : permutation) {
            rotated.add(data.get(index));
        }
        return List.copyOf(rotated);
    }
}
