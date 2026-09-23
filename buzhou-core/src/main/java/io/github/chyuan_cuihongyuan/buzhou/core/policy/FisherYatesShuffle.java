package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * Fisher-Yates 无偏洗牌（spec 5002 / T6105 / impl 2153）——
 * Durstenfeld 变体思想（Knuth shuffle）：从尾向前，每步与
 * {@code [0, i]} 均匀取 j 交换（{@code nextInt(i + 1)} 含
 * 自身——无偏的关键，取开区间即经典有偏洗牌）——O(n) 原地、
 * n! 排列等概率。排序键取模（排列分布不均）与每次全量重建
 * （O(n²)）的病解。
 *
 * <p>种子注入确定性：同种子同排列。与 DeterministicHash 同族
 * 不同面：散列位置映射 vs 排列均匀化。
 */
public final class FisherYatesShuffle {

    private FisherYatesShuffle() {
    }

    /** 原地洗牌。 */
    public static <T> void shuffleInPlace(List<T> list, RandomGenerator random) {
        if (list == null || random == null) {
            throw new IllegalArgumentException("list 与 random 非 null");
        }
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);   // [0, i] 含自身——无偏关键
            T swapped = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swapped);
        }
    }

    /** 不可变变体（返回新洗牌列表；原列表不变）。 */
    public static <T> List<T> shuffled(List<T> list, long seed) {
        if (list == null) {
            throw new IllegalArgumentException("list 非 null");
        }
        List<T> copy = new ArrayList<>(list);
        shuffleInPlace(copy, new Random(seed));
        return List.copyOf(copy);
    }

    /** 索引排列变体（只产排列不触碰元素；sorted = 恒等）。 */
    public static int[] shuffledIndices(int size, long seed) {
        if (size < 0) {
            throw new IllegalArgumentException("size 需非负：" + size);
        }
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = i;
        }
        RandomGenerator random = new Random(seed);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swapped = indices[i];
            indices[i] = indices[j];
            indices[j] = swapped;
        }
        return indices;
    }
}
