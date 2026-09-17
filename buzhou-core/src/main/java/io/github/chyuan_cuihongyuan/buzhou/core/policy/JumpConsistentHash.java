package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * 跳增一致性哈希（spec 3020 / T5041 / impl 2021）——Google jump
 * consistent hash 思想（Lamping & Masayuki 2014）：键 → [0,n) 桶，
 * **O(1) 空间零表零虚节点**；扩容 n→n+1 时仅约 1/(n+1) 的键迁移
 * 且**全部迁往新桶**（最小迁移——朴素取模 n 变则全量重排病的
 * 根治）。与一致性哈希环（Dynamo/Ketama 虚节点）取舍：环支持
 * 任意桶增删与权重、jump 零内存但只支持**尾端扩缩**——键序均衡
 * 大、内存敏感的场景选 jump。
 *
 * <p>纯函数静态件；线性同余推进（论文常数）——确定性可复算。
 */
public final class JumpConsistentHash {

    /** 论文线性同余乘数（jump hash 公认常数）。 */
    private static final long LCG_MULTIPLIER = 2862933555777941757L;

    /** 论文归一基数（2^31——(key>>>33)+1 归一到跳跃概率）。 */
    private static final double NORMALIZER = (double) (1L << 31);

    private JumpConsistentHash() {
    }

    /** 键 → 桶 [0, bucketCount)；确定性（同键同桶跨调用恒等）。 */
    public static int bucketOf(long key, int bucketCount) {
        if (bucketCount < 1) {
            throw new IllegalArgumentException("bucketCount ≥ 1：" + bucketCount);
        }
        long b = -1;
        long j = 0;
        while (j < bucketCount) {
            b = j;
            key = key * LCG_MULTIPLIER + 1;
            j = (long) ((b + 1) * (NORMALIZER / (double) ((key >>> 33) + 1)));
        }
        return (int) b;
    }

    /** 扩缩容迁移判定：bucketOf(key, oldCount) != bucketOf(key, newCount)。 */
    public static boolean movesOnResize(long key, int oldCount, int newCount) {
        return bucketOf(key, oldCount) != bucketOf(key, newCount);
    }
}
