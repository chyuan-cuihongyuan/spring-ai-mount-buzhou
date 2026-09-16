package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 确定性 64 位散列公共件（spec 2057 / T3215 / impl 1608）——收敛
 * P 系六件（HLL/频率素描/布谷鸟/一致性哈希环/SimHash）各自内联的
 * 同款实现：FNV-1a 64 基 + splitmix64 终结混合（雪崩充分）——同输入
 * 同输出、无随机、无种子、跨实例跨进程稳定（回放与合并语义的前提）。
 *
 * <p>纯静态函数；非加密口径（分布质量用，安全性归 guard 域）。
 */
public final class DeterministicHash {

    /** FNV-1a 64 偏移基（标准常数）。 */
    static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;

    /** FNV-1a 64 质数（标准常数）。 */
    static final long FNV_PRIME = 0x100000001b3L;

    private DeterministicHash() {
    }

    /** FNV-1a 64 + splitmix64 终结混合：确定性、雪崩充分。契约：s 非 null。 */
    public static long hash64(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s 不能为 null");
        }
        long h = FNV_OFFSET_BASIS;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= FNV_PRIME;
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
