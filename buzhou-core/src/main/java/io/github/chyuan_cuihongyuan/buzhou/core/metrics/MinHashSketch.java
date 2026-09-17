package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * MinHash Jaccard 素描（spec 3010 / T5021 / impl 2011）——MinHash
 * 思想（Broder 1997，AltaVista 近重复检测）：集合压成 k 路最小哈希
 * **签名**——两签名相等位占比是 Jaccard 相似度的无偏估计：
 * P[min_ h(A)=min_ h(B)] = |A∩B|/|A∪B|。近重复**粗筛**换复杂度：
 * 全量两两精确 Jaccard 是 O(n²·|元素|)，签名比较 O(k)——大规模
 * 候选对粗筛后再精确复核（粗筛+精复两层口径）。
 *
 * <p>基散列复用 {@link DeterministicHash#hash64}，第 i 路派生 =
 * splitmix64 终结器（确定性——同集合同签名，无随机源）；空集合
 * 语义诚实：双空 NaN / 单空 0。
 */
public final class MinHashSketch {

    /** splitmix64 Weyl 增量（黄金比例 64 位——见终结器常量族）。 */
    private static final long WEYL_GOLDEN = 0x9E3779B97F4A7C15L;

    /** splitmix64 终结器第一乘（StreamLib/splitmix64 公认常量）。 */
    private static final long MIX_MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;

    /** splitmix64 终结器第二乘。 */
    private static final long MIX_MULTIPLIER_2 = 0x94D049BB133111EBL;

    private final int hashCount;
    private final long[] signature;
    private long offerCount;

    /** k 路签名（hashCount ≥ 1；越大估计方差越小——σ≈√(J(1−J)/k)）。 */
    public MinHashSketch(int hashCount) {
        if (hashCount < 1) {
            throw new IllegalArgumentException("hashCount ≥ 1：" + hashCount);
        }
        this.hashCount = hashCount;
        this.signature = new long[hashCount];
        java.util.Arrays.fill(signature, Long.MAX_VALUE);
    }

    /** 吸收集合元素（重复 offer 幂等——最小值语义天然去重）。 */
    public void offer(String element) {
        long base = DeterministicHash.hash64(element);
        for (int i = 0; i < hashCount; i++) {
            long candidate = mix(base, i);
            if (candidate < signature[i]) {
                signature[i] = candidate;
            }
        }
        offerCount++;
    }

    /** 签名读数（防御性拷贝——外部改不动账面）。 */
    public long[] signature() {
        return signature.clone();
    }

    /** 签名路数。 */
    public int hashCount() {
        return hashCount;
    }

    /** offer 次数（含重复——原始流量对账面）。 */
    public long offerCount() {
        return offerCount;
    }

    /**
     * Jaccard 相似度估计 = 相等位占比（无偏）。双空 NaN（J(∅,∅)
     * 无定义——诚实不臆答）；单空 0（无交集）。
     */
    public double similarityTo(MinHashSketch other) {
        boolean selfEmpty = offerCount == 0;
        boolean otherEmpty = other.offerCount == 0;
        if (selfEmpty && otherEmpty) {
            return Double.NaN;
        }
        if (selfEmpty || otherEmpty) {
            return 0.0;
        }
        if (other.hashCount != hashCount) {
            throw new IllegalArgumentException("签名路数不一致：" + hashCount + " vs " + other.hashCount);
        }
        int matches = 0;
        for (int i = 0; i < hashCount; i++) {
            if (signature[i] == other.signature[i]) {
                matches++;
            }
        }
        return (double) matches / hashCount;
    }

    /** 第 i 路哈希：Weyl 序掺 index + splitmix64 终结器（确定性）。 */
    private static long mix(long base, int index) {
        long z = base + index * WEYL_GOLDEN;
        z = (z ^ (z >>> 30)) * MIX_MULTIPLIER_1;
        z = (z ^ (z >>> 27)) * MIX_MULTIPLIER_2;
        return z ^ (z >>> 31);
    }
}
