package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.DeterministicHash;

/**
 * 布谷鸟过滤器（spec 2016 / T3133 / impl 1567）——Cuckoo filter 思想：
 * 指纹+双桶+踢出重排的近似成员过滤器——**可删除**（布伦的硬缺口：
 * 检疫集释放成员后布伦无法撤销，误留永久假阳性）；假阳性率随指纹
 * 位宽与负载因子下降。
 *
 * <p>确定性踢出（轮流起点——无随机数，同序列同答案可回放）；
 * synchronized 小临界区；溢出（踢尽 MAX_KICKS 仍满）拒插并计数。
 */
public final class CuckooFilter {

    /** 每桶槽数（标准 b=4——空间/踢出成功率平衡点）。 */
    public static final int BUCKET_SLOTS = 4;

    /** 单次插入最大踢出次数（标准 500——超过即判满）。 */
    public static final int MAX_KICKS = 500;

    /** 默认桶数（2 的幂——万级成员 × 负载 <85% 量级）。 */
    public static final int DEFAULT_BUCKETS = 8192;

    /** 空槽哨兵（0 为空——指纹散列避开 0）。 */
    private static final short EMPTY = 0;

    private final short[] buckets; // buckets × BUCKET_SLOTS
    private final int bucketCount;
    private int kickCursor; // 轮流踢出起点（确定性）
    private long size;
    private long overflowed;

    /** 契约：bucketCount 为 2 的幂且 ≥ 16（fail-fast）。 */
    public CuckooFilter(int bucketCount) {
        if (bucketCount < 16 || Integer.bitCount(bucketCount) != 1) {
            throw new IllegalArgumentException("bucketCount 须为 ≥16 的 2 的幂：" + bucketCount);
        }
        this.bucketCount = bucketCount;
        this.buckets = new short[bucketCount * BUCKET_SLOTS];
    }

    public CuckooFilter() {
        this(DEFAULT_BUCKETS);
    }

    /**
     * 插入（近似成员——同元素重复插占多槽，先 delete 再 insert 可免）。
     * 桶满踢出重排（轮流起点确定性）；踢尽仍满拒插 false 并计
     * overflowed。契约：item 非空。
     */
    public synchronized boolean insert(String item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        short fp = fingerprint(item);
        int i1 = bucketIndex(DeterministicHash.hash64(item));
        int i2 = i1 ^ bucketIndex(DeterministicHash.hash64(Short.toString(fp)));
        if (tryInsertTo(i1, fp) || tryInsertTo(i2, fp)) {
            size++;
            return true;
        }
        // 双桶满：踢出重排
        int bucket = kickCursor % 2 == 0 ? i1 : i2;
        for (int kick = 0; kick < MAX_KICKS; kick++) {
            int slot = kickCursor % BUCKET_SLOTS;
            kickCursor++;
            short victim = buckets[bucket * BUCKET_SLOTS + slot];
            buckets[bucket * BUCKET_SLOTS + slot] = fp;
            fp = victim;
            bucket = bucket ^ bucketIndex(DeterministicHash.hash64(Short.toString(fp)));
            if (tryInsertTo(bucket, fp)) {
                size++;
                return true;
            }
        }
        overflowed++;
        return false; // 踢尽仍满——拒插（负载因子超限信号）
    }

    /** 近似查询：任一候选桶含指纹即 true（可能假阳性，不假阴性）。 */
    public synchronized boolean mightContain(String item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        short fp = fingerprint(item);
        int i1 = bucketIndex(DeterministicHash.hash64(item));
        int i2 = i1 ^ bucketIndex(DeterministicHash.hash64(Short.toString(fp)));
        return bucketHas(i1, fp) || bucketHas(i2, fp);
    }

    /**
     * 删除一个（近似——删除未插入元素可能误删同指纹者，调用方自律：
     * 只删确曾插入的）；成功 true，未见指纹 false。
     */
    public synchronized boolean delete(String item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        short fp = fingerprint(item);
        int i1 = bucketIndex(DeterministicHash.hash64(item));
        int i2 = i1 ^ bucketIndex(DeterministicHash.hash64(Short.toString(fp)));
        int bucketWithFp = bucketHasSlot(i1, fp) >= 0 ? i1
                : bucketHasSlot(i2, fp) >= 0 ? i2 : -1;
        if (bucketWithFp < 0) {
            return false;
        }
        for (int s = 0; s < BUCKET_SLOTS; s++) {
            int idx = bucketWithFp * BUCKET_SLOTS + s;
            if (buckets[idx] == fp) {
                buckets[idx] = EMPTY;
                size--;
                return true;
            }
        }
        return false;
    }

    /** 当前占用槽数（负载因子对账面）。 */
    public synchronized long size() {
        return size;
    }

    /** 溢出（拒插）计数——扩容信号。 */
    public synchronized long overflowCount() {
        return overflowed;
    }

    private boolean tryInsertTo(int bucket, short fp) {
        for (int s = 0; s < BUCKET_SLOTS; s++) {
            int idx = bucket * BUCKET_SLOTS + s;
            if (buckets[idx] == EMPTY) {
                buckets[idx] = fp;
                return true;
            }
        }
        return false;
    }

    private boolean bucketHas(int bucket, short fp) {
        return bucketHasSlot(bucket, fp) >= 0;
    }

    private int bucketHasSlot(int bucket, short fp) {
        for (int s = 0; s < BUCKET_SLOTS; s++) {
            if (buckets[bucket * BUCKET_SLOTS + s] == fp) {
                return bucket;
            }
        }
        return -1;
    }

    private int bucketIndex(long hash) {
        return (int) (hash & (bucketCount - 1));
    }

    /** 16bit 指纹（0 哨兵避开——+1 偏移）。 */
    private static short fingerprint(String item) {
        short fp = (short) (DeterministicHash.hash64(item) & 0xFFFF);
        return fp == EMPTY ? 1 : fp;
    }

}
