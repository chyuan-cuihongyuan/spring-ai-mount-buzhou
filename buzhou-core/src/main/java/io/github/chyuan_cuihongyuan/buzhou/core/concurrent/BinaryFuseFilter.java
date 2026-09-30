package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * 二进制熔合过滤器（spec 10003 / X10007 / impl 2406）——Lim–Graf–lemire
 * 2024 思想（「三段熔合窗口+逆剥离构造」——xorfilter 项目/布隆替代
 * 同源）：**静态键集一次构造：每键散列出同段三窗口位点（段起点+
 * 三局部偏移，窗口带重叠=熔合），8 位指纹=三位点异或；构造走 3-
 * 均匀超图剥离（度 1 位点入栈逆序反解），剥离失败换种子重试**——
 * 布隆逐键 10 位 vs 本构 ~9 位且零假阴性查询只读三位点。段长表
 * （512/2048/4096 三档）与 1.13 槽/键密度沿论文；指纹 0 保留改 1；
 * 重复键/空集/剥离百试不结 fail-fast；同键集同种子序列完全确定。
 */
public final class BinaryFuseFilter {

    /** 指纹位宽（8 位——fp 0 保留，有效域 1..255）。 */
    private static final int FINGERPRINT_BITS = 8;

    /** 黄金比例混洗常数（SplitMix64 终结化同款家族）。 */
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private static final int MIX_SHIFT_A = 32;
    private static final int MIX_SHIFT_B = 29;
    private static final int MIX_SHIFT_C = 32;

    /** 构造种子重试上界（文献经验 <10 次必结）。 */
    private static final int MAX_SEED_TRIES = 100;

    /** 段长三档表（论文表：64K 内 512 / 1M 内 2048 / 以上 4096）。 */
    private static final int SEGMENT_LENGTH_SMALL = 512;
    private static final int SEGMENT_LENGTH_MEDIUM = 2048;
    private static final int SEGMENT_LENGTH_LARGE = 4096;
    private static final int MEDIUM_SET_THRESHOLD = 65536;
    private static final int LARGE_SET_THRESHOLD = 1 << 20;

    /** 槽密度系数（论文 1.13 槽/键——3-XORSAT 剥离可解下界之上）。 */
    private static final double SLOT_DENSITY = 1.13;

    private final byte[] fingerprints;
    private final int segmentLength;
    private final int segmentCount;
    private final long seed;
    private final int sizeOfKeys;

    /**
     * 由键集静态构造（集合语义：重复键 fail-fast）。
     *
     * @throws IllegalArgumentException null/空/重复键或剥离不结
     */
    public BinaryFuseFilter(long[] keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("键集非空且非 null（实际 "
                    + (keys == null ? "null" : "空") + "）");
        }
        long[] distinct = keys.clone();
        Arrays.sort(distinct);
        for (int i = 1; i < distinct.length; i++) {
            if (distinct[i] == distinct[i - 1]) {
                throw new IllegalArgumentException("键集无重复（实际重复 " + distinct[i] + "）");
            }
        }
        int n = keys.length;
        this.segmentLength = n <= MEDIUM_SET_THRESHOLD ? SEGMENT_LENGTH_SMALL
                : n <= LARGE_SET_THRESHOLD ? SEGMENT_LENGTH_MEDIUM : SEGMENT_LENGTH_LARGE;
        this.segmentCount = Math.max(1, (int) Math.round(SLOT_DENSITY * n / segmentLength));
        int arrayLength = (segmentCount + 2) * segmentLength;
        byte[] solution = null;
        long chosenSeed = 0;
        for (long trial = 0; trial < MAX_SEED_TRIES; trial++) {
            byte[] candidate = tryConstruct(keys, arrayLength, trial);
            if (candidate != null) {
                solution = candidate;
                chosenSeed = trial;
                break;
            }
        }
        if (solution == null) {
            throw new IllegalStateException("剥离构造百试不结（n=" + n + "）");
        }
        this.fingerprints = solution;
        this.seed = chosenSeed;
        this.sizeOfKeys = n;
    }

    /** 近似成员判定（零假阴性；8 位指纹基率 ~0.39% 假阳性）。 */
    public boolean mightContain(long key) {
        long hash = mix(key, seed);
        int p0 = position(hash, 0);
        int p1 = position(hash, 1);
        int p2 = position(hash, 2);
        int value = (fingerprints[p0] & 0xFF) ^ (fingerprints[p1] & 0xFF) ^ (fingerprints[p2] & 0xFF);
        return value == fingerprint(hash);
    }

    /** 已收录键数。 */
    public int size() {
        return sizeOfKeys;
    }

    /** 混洗（SplitMix64 终结化同族三轮，键+种子双射确定）。 */
    private static long mix(long key, long seed) {
        long z = key + seed * GOLDEN_GAMMA + GOLDEN_GAMMA;
        z = (z ^ (z >>> MIX_SHIFT_A)) * GOLDEN_GAMMA;
        z = (z ^ (z >>> MIX_SHIFT_B)) * GOLDEN_GAMMA;
        return z ^ (z >>> MIX_SHIFT_C);
    }

    /** 8 位指纹（0 保留改 1）。 */
    private int fingerprint(long hash) {
        long post = (hash >>> 17) * GOLDEN_GAMMA;
        int fp = (int) ((post >>> 56) & 0xFF);
        return fp == 0 ? 1 : fp;
    }

    /** 第 slot 个窗口位点（段起点 + slot×段长 + 段内偏移——熔合重叠窗）。 */
    private int position(long hash, int slot) {
        int segment = (int) (((hash >>> MIX_SHIFT_A) * segmentCount) >>> MIX_SHIFT_A);
        int offsetSlot = slot == 0 ? 0 : slot == 1 ? 16 : 32;
        int local = (int) (hash >>> offsetSlot) & (segmentLength - 1);
        return segment * segmentLength + slot * segmentLength + local;
    }

    /**
     * 单种子尝试：3-均匀超图剥离（度 1 位点入栈）——成功返回已反解
     * 指纹数组，失败返回 null。
     */
    private byte[] tryConstruct(long[] keys, int arrayLength, long trialSeed) {
        int n = keys.length;
        long[] hashOf = new long[n];
        int[][] posOf = new int[n][3];
        int[] aloneCount = new int[arrayLength];
        int[] xorList = new int[arrayLength];
        for (int i = 0; i < n; i++) {
            hashOf[i] = mix(keys[i], trialSeed);
            for (int slot = 0; slot < 3; slot++) {
                posOf[i][slot] = position(hashOf[i], slot);
                aloneCount[posOf[i][slot]]++;
                xorList[posOf[i][slot]] ^= i;
            }
        }
        int[] stackKey = new int[n];
        int[] stackSlot = new int[n];
        int stackTop = 0;
        int[] alonePositions = new int[arrayLength];
        int aloneTop = 0;
        for (int p = 0; p < arrayLength; p++) {
            if (aloneCount[p] == 1) {
                alonePositions[aloneTop++] = p;
            }
        }
        boolean[] removed = new boolean[n];
        while (aloneTop > 0) {
            int p = alonePositions[--aloneTop];
            if (aloneCount[p] != 1) {
                continue;
            }
            int key = xorList[p];
            if (removed[key]) {
                continue;
            }
            stackKey[stackTop] = key;
            stackSlot[stackTop] = p;
            stackTop++;
            removed[key] = true;
            for (int slot = 0; slot < 3; slot++) {
                int q = posOf[key][slot];
                aloneCount[q]--;
                xorList[q] ^= key;
                if (aloneCount[q] == 1) {
                    alonePositions[aloneTop++] = q;
                }
            }
        }
        if (stackTop < n) {
            return null;
        }
        byte[] solution = new byte[arrayLength];
        for (int i = stackTop - 1; i >= 0; i--) {
            int key = stackKey[i];
            int tied = stackSlot[i];
            int t = fingerprint(hashOf[key]);
            for (int slot = 0; slot < 3; slot++) {
                t ^= solution[posOf[key][slot]] & 0xFF;
            }
            solution[tied] = (byte) t;
        }
        return solution;
    }
}
