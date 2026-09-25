package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Counting Bloom Filter 计数布隆过滤器（spec 6036 / T6269 续 /
 * impl 2237）——Bloom 1970 计数变体思想：**k 哈希计数数组
 * 替代位阵**——插入计数+1、删除计数−1（饱和于 0 防负），
 * contains 判定全部 k 位计数 >0——标准 Bloom 位阵不可删除
 * （重建才可移除）与误删他人位（共享位翻转）的病解。假阳性
 * 率 ≈ (1−e^{−kn/m})^k 上界可见；无假阴性。SplitMix64 双
 * 哈希模拟 k 次探针（h1+i·h2——确定性）。
 *
 * <p>与 SessionBloomFilter/CuckooFilter 同族不同面：可删除
 * 计数位阵 vs 位阵/指纹槽；与 XorFilter（5042）不同面：
 * 动态可变 vs 静态构建。
 */
public final class CountingBloomFilter {

    private final int[] counters;
    private final int numHashes;
    private long insertedCount;

    /** counters 位宽×8/numHashes 比例可调（参数越域 fail-fast）。 */
    public CountingBloomFilter(int expectedInsertions, double falsePositiveRate, int numHashes) {
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException("预期插入须为正: " + expectedInsertions);
        }
        if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
            throw new IllegalArgumentException("假阳性率须在 (0,1): " + falsePositiveRate);
        }
        if (numHashes <= 0 || numHashes > 16) {
            throw new IllegalArgumentException("哈希数须在 [1,16]: " + numHashes);
        }
        int bits = (int) Math.ceil(-expectedInsertions * Math.log(falsePositiveRate)
                / (Math.log(2) * Math.log(2)));
        this.counters = new int[Math.max(bits, numHashes)];
        this.numHashes = numHashes;
    }

    /** 插入（null fail-fast）。 */
    public void insert(String element) {
        requireNonNull(element);
        long[] probes = probeIndexes(element);
        for (long idx : probes) {
            counters[(int) idx]++;
        }
        insertedCount++;
    }

    /** 可能存在（无假阴性）。 */
    public boolean mightContain(String element) {
        requireNonNull(element);
        for (long idx : probeIndexes(element)) {
            if (counters[(int) idx] == 0) {
                return false;
            }
        }
        return true;
    }

    /** 删除（计数−1，饱和于 0；未插入元素删除会引入假阴性——调用方契约）。 */
    public void remove(String element) {
        requireNonNull(element);
        for (long idx : probeIndexes(element)) {
            if (counters[(int) idx] > 0) {
                counters[(int) idx]--;
            }
        }
        insertedCount = Math.max(0, insertedCount - 1);
    }

    /** 插入计数读数。 */
    public long insertedCount() {
        return insertedCount;
    }

    /** 计数数组长度读数。 */
    public int counterCount() {
        return counters.length;
    }

    /** 哈希数读数。 */
    public int numHashes() {
        return numHashes;
    }

    private long[] probeIndexes(String element) {
        byte[] bytes = element.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long h1 = mix(java.nio.ByteBuffer.wrap(bytes).hashCode() * 31L + bytes.length, 0x9E3779B97F4A7C15L);
        long h2 = mix(h1, 0xBF58476D1CE4E5B9L);
        long[] indexes = new long[numHashes];
        for (int i = 0; i < numHashes; i++) {
            long combined = h1 + i * h2;
            indexes[i] = Math.floorMod(combined, counters.length);
        }
        Arrays.sort(indexes);
        return indexes;
    }

    private static long mix(long value, long seed) {
        long z = value + seed;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static void requireNonNull(String element) {
        if (element == null) {
            throw new IllegalArgumentException("元素非空");
        }
    }
}
