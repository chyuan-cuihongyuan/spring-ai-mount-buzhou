package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extendible Hashing 可扩目录哈希（spec 6032 / T6263 / impl 2233）——
 * Fagin 可扩目录哈希思想：**目录深度按需翻倍**——键按哈希低
 * globalDepth 位寻址目录项；桶溢出且 localDepth==globalDepth
 * 时目录翻倍（其余项平移复制），否则仅该桶分裂（localDepth++）
 * ——静态哈希表满即全量重哈希（扩容停顿放大）的病解。
 * 键哈希 SplitMix64 混淆（确定性分布）。集合语义（重复幂等）。
 *
 * <p>与 RobinHoodHashTable（5045）同族不同面：开放寻址均衡
 * 探测 vs 目录翻倍桶分裂；与 ExtendibleHashing 同族的线性
 * 哈希不在本面。确定性定构（同插入序同目录布局）。
 */
public final class ExtendibleHashing {

    private static final int BUCKET_CAPACITY = 4;

    private static final class Bucket {
        final int localDepth;
        final List<Long> keys = new ArrayList<>();

        Bucket(int localDepth) {
            this.localDepth = localDepth;
        }
    }

    private final Map<Long, Long> mixCache = new HashMap<>();
    private List<Bucket> directory = new ArrayList<>();
    private int globalDepth;
    private int size;

    /** 建表（初始全局深度 1）。 */
    public ExtendibleHashing() {
        directory.add(new Bucket(1));
        directory.add(new Bucket(1));
        this.globalDepth = 1;
    }

    /** 插入（集合语义——重复幂等）。 */
    public void insert(long key) {
        if (contains(key)) {
            return;
        }
        int idx = directoryIndex(key);
        Bucket bucket = directory.get(idx);
        if (bucket.keys.size() < BUCKET_CAPACITY) {
            bucket.keys.add(key);
            size++;
            return;
        }
        if (bucket.localDepth == globalDepth) {
            doubleDirectory();
        }
        split(idx);
        insert(key);
    }

    /** 是否包含。 */
    public boolean contains(long key) {
        return directory.get(directoryIndex(key)).keys.contains(key);
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 目录项数读数（=2^globalDepth）。 */
    public int directorySize() {
        return directory.size();
    }

    /** 全局深度读数。 */
    public int globalDepth() {
        return globalDepth;
    }

    /** 实际桶数读数（≤目录项数）。 */
    public int bucketCount() {
        return (int) directory.stream().distinct().count();
    }

    private int directoryIndex(long key) {
        return (int) (mix(key) & ((1 << globalDepth) - 1));
    }

    private void doubleDirectory() {
        globalDepth++;
        int old = directory.size();
        for (int i = 0; i < old; i++) {
            directory.add(directory.get(i));
        }
    }

    private void split(int idx) {
        Bucket old = directory.get(idx);
        int newDepth = old.localDepth + 1;
        Bucket a = new Bucket(newDepth);
        Bucket b = new Bucket(newDepth);
        for (long key : old.keys) {
            int slot = (int) (mix(key) & ((1 << newDepth) - 1));
            ((slot >>> (newDepth - 1) & 1) == 1 ? b : a).keys.add(key);
        }
        int stride = 1 << newDepth;
        for (int i = idx & (stride - 1); i < directory.size(); i += stride) {
            int high = (i >>> (newDepth - 1)) & 1;
            directory.set(i, high == 1 ? b : a);
        }
    }

    private long mix(long key) {
        return mixCache.computeIfAbsent(key, k -> {
            long z = k + 0x9E3779B97F4A7C15L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        });
    }
}
