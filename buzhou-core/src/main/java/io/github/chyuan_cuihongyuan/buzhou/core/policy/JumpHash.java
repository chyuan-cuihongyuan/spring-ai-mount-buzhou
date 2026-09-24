package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * Jump Consistent Hash 跳跃一致哈希（spec 5030 / T6161 / impl 2181）——
 * Google Lamping-Veach 跳跃一致哈希论文思想：`bucketOf` 以线性同余
 * 发生器逐桶随机游走，桶数 m 增至 m+1 时约 m/(m+1) 的键原桶不动
 * （单调稳定——扩容只搬最少量的键）；无环、无每节点元数据、常数
 * 内存——哈希环 + 虚拟节点（建环/重排/内存开销）的病解；
 * 字符串键走 FNV-1a 64 稳定指纹（跨进程跨 JVM 同键同桶）。
 * 确定性无时间依赖。
 *
 * <p>与 BoundedLoadRing（spec 5003）同族不同面：极简无状态跳跃
 * vs 有界负载环（负载上限约束）。
 */
public final class JumpHash {

    /** 论文线性同余乘子（64 位随机游走发生器）。 */
    private static final long LCG_MULTIPLIER = 2862933555777941757L;

    /** 论文定标：以 2^31 为概率尺度做跨桶跳跃。 */
    private static final long SCALE_BITS = 31L;

    /** 无符号右移位数：取发生器输出的高 31 位作均匀随机数。 */
    private static final long HIGH_BITS_SHIFT = 33L;

    /** FNV-1a 64 偏移基。 */
    private static final long FNV_OFFSET_BASIS = -3750763034362895579L;

    /** FNV-1a 64 素数。 */
    private static final long FNV_PRIME = 1099511628211L;

    private JumpHash() {
    }

    /**
     * 长整键定桶（0 基；同一键同桶数恒同桶；桶数扩容时键
     * 尽量留原桶——单调稳定）。
     */
    public static int bucketOf(long key, int bucketCount) {
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("bucketCount>0：" + bucketCount);
        }
        long chosen = -1;
        long probe = 0;
        long k = key;
        while (probe < bucketCount) {
            chosen = probe;
            k = k * LCG_MULTIPLIER + 1;
            long uniform = (k >>> HIGH_BITS_SHIFT) + 1;
            probe = (long) (((double) (chosen + 1) * (1L << SCALE_BITS)) / (double) uniform);
        }
        return (int) chosen;
    }

    /** 字符串键定桶（FNV-1a 64 稳定指纹 → 长整键定桶）。 */
    public static int bucketOf(String key, int bucketCount) {
        return bucketOf(fingerprint(key), bucketCount);
    }

    /**
     * FNV-1a 64 稳定指纹（{@code String.hashCode} 跨进程不承诺
     * 稳定且仅 32 位——一致性哈希需要跨进程同键同桶）。
     */
    public static long fingerprint(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        long hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < key.length(); i++) {
            hash = (hash ^ key.charAt(i)) * FNV_PRIME;
        }
        return hash;
    }
}
