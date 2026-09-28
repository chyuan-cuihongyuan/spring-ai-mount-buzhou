package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 约瑟夫环（spec 7040 / U7281 / impl 2292）——Josephus 问题
 * 经典思想（罗马-犹太战争传说面）：**n 人围环计数，第 k
 * 个出列，循环至剩一**——逐个模拟 O(nk) 的朴素面保留
 * （出列序完整可审计——不只算幸存者），k=1 退化为顺序
 * 出列。确定性纯函数；n≥1、k≥1；越域 fail-fast。
 *
 * <p>与 JosephusPermutation 同族不同面（独立面）：与
 * FisherYatesShuffle（policy）不同面：随机洗牌 vs 确定性
 * 计数出列。
 */
public final class JosephusPermutation {

    private JosephusPermutation() {
    }

    /** 出列序（完整 n 个；n≥1、k≥1；越域 fail-fast）。 */
    public static List<Integer> permutation(int n, int k) {
        if (n < 1 || k < 1) {
            throw new IllegalArgumentException("n,k 须为正: " + n + "," + k);
        }
        List<Integer> circle = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            circle.add(i);
        }
        List<Integer> out = new ArrayList<>(n);
        int index = 0;
        while (!circle.isEmpty()) {
            index = (index + k - 1) % circle.size();
            out.add(circle.remove(index));
        }
        return out;
    }

    /** 幸存者（出列序最后一个）。 */
    public static int survivor(int n, int k) {
        return permutation(n, k).get(n - 1);
    }
}
