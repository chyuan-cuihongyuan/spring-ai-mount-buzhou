package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

/**
 * SimHash 近重复指纹（spec 2038 / T3179 / impl 1589）——Charikar SimHash
 * 思想：文本 → 64 位指纹——特征（词元）逐位散列加权投票（每 bit 位上
 * 权重和的符号即该位取值），**相似文本指纹的汉明距离小**（近重复
 * 判定 O(1) 位比较而非 O(n) 文本比对）；确定性（同文本同指纹可回放）。
 *
 * <p>诚实边界：小词元集的平局 bit（票和恰 0）对增量词元敏感——距离
 * 噪声大（经验上 ≥16 词元较稳；短文本宜用相对判定或指纹精确比对）。
 *
 * <p>纯函数零状态；正文分词口径由调用方（本件收词元清单）。
 */
public final class SimHashFingerprint {

    /** 默认近重复汉明距离阈值（≤3 惯例）。 */
    public static final int DEFAULT_NEAR_DISTANCE = 3;

    private SimHashFingerprint() {
    }

    /** 词元清单 → 64 位指纹：每词元 1 权重，逐 bit 加权投票。契约：tokens 非空（空文本无指纹语义）。 */
    public static long fingerprint(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("tokens 不能为空");
        }
        long[] bitSums = new long[Long.SIZE];
        for (String token : tokens) {
            if (token == null) {
                throw new IllegalArgumentException("token 不能为 null");
            }
            long h = hash64(token);
            for (int bit = 0; bit < Long.SIZE; bit++) {
                if ((h >>> bit & 1L) == 1L) {
                    bitSums[bit] += 1;
                } else {
                    bitSums[bit] -= 1;
                }
            }
        }
        long fingerprint = 0L;
        for (int bit = 0; bit < Long.SIZE; bit++) {
            if (bitSums[bit] > 0) {
                fingerprint |= 1L << bit;
            }
        }
        return fingerprint;
    }

    /** 汉明距离（两指纹的不同 bit 数）。 */
    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    /** 近重复判定：汉明距离 ≤ threshold。契约：threshold ≥ 0。 */
    public static boolean isNearDuplicate(long a, long b, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold 须 ≥ 0：" + threshold);
        }
        return hammingDistance(a, b) <= threshold;
    }

    /** FNV-1a 64 + splitmix64 终结（与 HLL/频率素描同款确定性散列）。 */
    private static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
