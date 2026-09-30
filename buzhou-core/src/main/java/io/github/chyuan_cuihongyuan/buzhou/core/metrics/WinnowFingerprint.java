package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Winnowing 指纹（spec 9018 / W9037 / impl 2371）——winnowing
 * 思想（Schleimer 2003——斯坦福 MOSS 抄袭检测同源）：**k-gram
 * 滚动哈希后，宽 w 窗口逐一取最小哈希（并列取最右——确定），
 * 被选位置即文档指纹**—— guarantee：任何 ≥k+w−1 的匹配段必含
 * 至少一枚公共指纹；全 k-gram 保留（指纹爆炸）与随机采样
 * （短匹配全漏）的中间形态。确定性多项式滚动哈希（固定基/
 * 模——同文同指纹完全确定）；位置去重升序；k/w 非正、
 * data 短于 k（空指纹）fail-fast/诚实退化。
 *
 * <p>与 MinHashSketch（metrics 域）同域不同面：集合近似 Jaccard
 * vs 序列位置指纹（保序可定位）；与 SimHashFingerprint 不同面：
 * 全文近重复 vs 局部匹配检测。
 */
public final class WinnowFingerprint {

    private static final long BASE = 257;
    private static final long MOD = (1L << 31) - 1;

    private WinnowFingerprint() {
    }

    /**
     * 指纹位置序列（k-gram 起始位，升序去重）。
     *
     * @throws IllegalArgumentException null 输入、k/w 非正
     */
    public static List<Integer> fingerprints(byte[] data, int k, int w) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        if (k < 1 || w < 1) {
            throw new IllegalArgumentException("k/w 为正（实际 " + k + "/" + w + "）");
        }
        int n = data.length;
        if (n < k) {
            return List.of();
        }
        int grams = n - k + 1;
        long[] hashes = new long[grams];
        long power = 1;
        for (int i = 1; i < k; i++) {
            power = power * BASE % MOD;
        }
        long hash = 0;
        for (int i = 0; i < k; i++) {
            hash = (hash * BASE + (data[i] & 0xFF)) % MOD;
        }
        hashes[0] = hash;
        for (int i = k; i < n; i++) {
            hash = ((hash - (data[i - k] & 0xFF) * power % MOD + MOD) % MOD * BASE
                    + (data[i] & 0xFF)) % MOD;
            hashes[i - k + 1] = hash;
        }
        TreeSet<Integer> selected = new TreeSet<>();
        for (int start = 0; start + w <= grams; start++) {
            int best = start;
            for (int j = start + 1; j < start + w; j++) {
                if (hashes[j] <= hashes[best]) {
                    best = j;
                }
            }
            selected.add(best);
        }
        return new ArrayList<>(selected);
    }
}
