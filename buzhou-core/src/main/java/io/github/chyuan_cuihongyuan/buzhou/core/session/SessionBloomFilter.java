package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话布隆粗筛（spec 1826 / T2853 / impl 1427）——Bloom filter 思想：
 * 「**从未见过**」的确定性快速判定——零假阴性（见过必报见过），小概率
 * 假阳性（没见过可能误报见过）。用途：新会话路由/归属快速排除（粗筛说
 * 没见过就一定没见过，直接走新建路径不用查全量索引）；内存代价是位图
 * 而非会话集。饱和度读数（setBits 占比）提示该重建——布隆只能增不能清。
 *
 * <p>确定性哈希（seed 混合 hashCode，无随机数——同输入同答案可回放）；
 * synchronized 小临界区（位图翻转与读数原子）。
 */
public final class SessionBloomFilter {

    /** 默认位图大小（位）——万级会话下假阳性率约 0.1% 量级。 */
    public static final int DEFAULT_BITS = 4096;

    /** 默认哈希函数数——3 次翻转在位图利用率与误报率间取衡。 */
    public static final int DEFAULT_HASHES = 3;

    /** 重建建议阈值：置位占比超过 50% 假阳性率开始爬升。 */
    public static final double SATURATION_THRESHOLD = 0.5d;

    private final int bits;
    private final int hashes;
    private final long[] words;

    /** 契约：bits ≥ 64、hashes ∈ [1,8]（fail-fast）。 */
    public SessionBloomFilter(int bits, int hashes) {
        if (bits < 64) {
            throw new IllegalArgumentException("bits 不能小于 64：" + bits);
        }
        if (hashes < 1 || hashes > 8) {
            throw new IllegalArgumentException("hashes 须在 [1,8]：" + hashes);
        }
        this.bits = bits;
        this.hashes = hashes;
        this.words = new long[(bits + 63) / 64];
    }

    public SessionBloomFilter() {
        this(DEFAULT_BITS, DEFAULT_HASHES);
    }

    /** 加入会话（见过）——幂等（重复加入同 id 位图不变）。 */
    public synchronized void add(String sessionId) {
        requireId(sessionId);
        for (int i = 0; i < hashes; i++) {
            long bit = bitFor(sessionId, i);
            words[(int) (bit >>> 6)] |= 1L << (bit & 63);
        }
    }

    /** 粗筛：false = **一定没见过**（零假阴性）；true = 可能见过。 */
    public synchronized boolean mightContain(String sessionId) {
        requireId(sessionId);
        for (int i = 0; i < hashes; i++) {
            long bit = bitFor(sessionId, i);
            if ((words[(int) (bit >>> 6)] & (1L << (bit & 63))) == 0) {
                return false;
            }
        }
        return true;
    }

    /** 置位占比（饱和度——超 {@link #SATURATION_THRESHOLD} 建议重建）。 */
    public synchronized double fillRatio() {
        long set = 0;
        for (long w : words) {
            set += Long.bitCount(w);
        }
        return (double) set / bits;
    }

    /** 确定性位选择：seed 混合 hashCode（FNV-1a 式扩散 + 黄金比散布）。 */
    private long bitFor(String id, int seed) {
        long h = id.hashCode() * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return Math.floorMod(h, bits);
    }

    private static void requireId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
    }
}
