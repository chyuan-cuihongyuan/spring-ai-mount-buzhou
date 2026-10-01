package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;
import java.util.Random;

/**
 * 水塘抽样（spec 11013 / Y11027 / impl 2466）——Vitter 1985 Algorithm R 思想
 * （Spark/Kafka 同源）：**前 k 直接入塘、i≥k 以 k/i 概率替换随机塘位**——
 * 未知总量流的单遍均匀 k 抽样（每元素入选概率恰 k/n）。输出升序（确定性
 * 契约面）；Random(seed) 种子驱动可复算。
 *
 * <p>k≥1 且 k≤n；越界 fail-fast；同种子复算确定。
 */
public final class ReservoirSampling {

    private ReservoirSampling() {
    }

    /**
     * 均匀抽取 k 个索引（升序）。
     *
     * @param populationSize 总量 n（≥k）
     * @param sampleSize 样本量 k（≥1）
     * @param seed 随机种子
     * @throws IllegalArgumentException k&lt;1/k&gt;n
     */
    public static int[] sample(int populationSize, int sampleSize, long seed) {
        if (sampleSize < 1) {
            throw new IllegalArgumentException("样本量 ≥1（实际 " + sampleSize + "）");
        }
        if (sampleSize > populationSize) {
            throw new IllegalArgumentException("k≤n（k=" + sampleSize
                    + " n=" + populationSize + "）");
        }
        Random random = new Random(seed);
        int[] reservoir = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            reservoir[i] = i;
        }
        for (int i = sampleSize; i < populationSize; i++) {
            int slot = random.nextInt(i + 1);
            if (slot < sampleSize) {
                reservoir[slot] = i;
            }
        }
        Arrays.sort(reservoir);
        return reservoir;
    }
}
