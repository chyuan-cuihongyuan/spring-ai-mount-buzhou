package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * Stable Bloom Filter 稳定布隆过滤器（spec 6037 / T6275 /
 * impl 2238）——Stable Bloom 思想：**流式衰减位阵**——每次
 * 插入前将 d 个随机位衰减 −1（近期性弱化），再按 k 哈希置
 * 满位；查询判 k 位全满——旧元素自动淡出（标准布隆/计数
 * 布隆一旦插入永久占据）与计数布隆删除需显式指名的病解
 * （流式场景「最近见过」语义）。SplitMix64 双哈希确定性。
 *
 * <p>与 CountingBloomFilter（spec 6036）同族不同面：显式删除
 * vs 隐式时间衰减；与 XorFilter（5042）不同面：动态流式 vs
 * 静态构建。
 */
public final class StableBloomFilter {

    private final byte[] cells;
    private final int decayCount;
    private final int numHashes;
    private final long[] decayCursor = {0};

    /** cells 槽位数、每次插入衰减 d 位、k 哈希（参数越域 fail-fast）。 */
    public StableBloomFilter(int cellsCount, int decayCount, int numHashes) {
        if (cellsCount <= 0) {
            throw new IllegalArgumentException("槽位数必须为正: " + cellsCount);
        }
        if (decayCount <= 0 || decayCount > cellsCount) {
            throw new IllegalArgumentException("衰减数须在 [1,槽位数]: " + decayCount);
        }
        if (numHashes <= 0 || numHashes > 16) {
            throw new IllegalArgumentException("哈希数须在 [1,16]: " + numHashes);
        }
        this.cells = new byte[cellsCount];
        this.decayCount = decayCount;
        this.numHashes = numHashes;
    }

    /** 插入（先随机衰减 d 位再置满 k 位；null fail-fast）。 */
    public void insert(String element) {
        requireNonNull(element);
        for (int i = 0; i < decayCount; i++) {
            int idx = decayIndex();
            if (cells[idx] > 0) {
                cells[idx]--;
            }
        }
        for (long idx : probeIndexes(element)) {
            cells[(int) idx] = 3;
        }
    }

    /** 可能是近期成员（无假阴性——近期插入恒真）。 */
    public boolean mightContain(String element) {
        requireNonNull(element);
        for (long idx : probeIndexes(element)) {
            if (cells[(int) idx] == 0) {
                return false;
            }
        }
        return true;
    }

    /** 槽位数读数。 */
    public int cellCount() {
        return cells.length;
    }

    /** 衰减数读数。 */
    public int decayCount() {
        return decayCount;
    }

    private int decayIndex() {
        long z = decayCursor[0] += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return (int) ((z ^ (z >>> 31)) & 0x7FFFFFFF) % cells.length;
    }

    private long[] probeIndexes(String element) {
        byte[] bytes = element.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long h1 = mix(java.nio.ByteBuffer.wrap(bytes).hashCode() * 31L + bytes.length,
                0x9E3779B97F4A7C15L);
        long h2 = mix(h1, 0xBF58476D1CE4E5B9L);
        long[] indexes = new long[numHashes];
        for (int i = 0; i < numHashes; i++) {
            indexes[i] = Math.floorMod(h1 + i * h2, cells.length);
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
