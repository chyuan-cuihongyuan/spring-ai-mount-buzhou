package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SimHash 位指纹海明分段索引（spec 10004 / X10009 / impl 2407）——
 * Manku–Das–Motwani 2007 思想（「抽屉原理分块全比对」——Google
 * 网页近似去重同源）：**64 位 SimHash 指纹均分为 blocks 块、每块
 * 一张倒排表；海明距离 ≤ blocks−1 的两指纹必有一块全等（抽屉
 * 原理）——查近邻只读 blocks 张表取并再验距，免全库两两比对**。
 * 与 SimHashFingerprint（core/metrics 已占）同域不同面：指纹计算
 * vs 分块索引近邻查询（本件指纹由外部注入）。查询结果为已验距
 * 的精确集（id 升序确定序）；越界距离/坏块数/负 id/重复 id
 * fail-fast。
 */
public final class SimHashLsh {

    /** 支持分块数（64 位整除且抽屉原理有意义：2/4/8）。 */
    private static final List<Integer> SUPPORTED_BLOCKS = List.of(2, 4, 8);

    /** 指纹总位宽。 */
    private static final int FINGERPRINT_BITS = 64;

    private final int blocks;
    private final int blockBits;
    private final List<Map<Long, List<Integer>>> tables;
    private final Map<Integer, Long> fingerprints;

    /**
     * 构造（blocks 块均分 64 位——查询保证距离 ≤ blocks−1）。
     *
     * @throws IllegalArgumentException 分块数不在 {2,4,8}
     */
    public SimHashLsh(int blocks) {
        if (!SUPPORTED_BLOCKS.contains(blocks)) {
            throw new IllegalArgumentException("分块数域 {2,4,8}（实际 " + blocks + "）");
        }
        this.blocks = blocks;
        this.blockBits = FINGERPRINT_BITS / blocks;
        this.tables = new ArrayList<>(blocks);
        for (int i = 0; i < blocks; i++) {
            tables.add(new HashMap<>());
        }
        this.fingerprints = new HashMap<>();
    }

    /**
     * 登记指纹（集合语义：重复 id fail-fast）。
     *
     * @throws IllegalArgumentException 负 id 或 id 重复
     */
    public void add(long fingerprint, int id) {
        if (id < 0) {
            throw new IllegalArgumentException("id 非负（实际 " + id + "）");
        }
        if (fingerprints.containsKey(id)) {
            throw new IllegalArgumentException("id 无重复（实际 " + id + " 已登记）");
        }
        fingerprints.put(id, fingerprint);
        for (int b = 0; b < blocks; b++) {
            tables.get(b).computeIfAbsent(blockOf(fingerprint, b), k -> new ArrayList<>()).add(id);
        }
    }

    /**
     * 近邻查询（返回已验距的精确 id 集，升序确定序）。
     *
     * @throws IllegalArgumentException 距离超抽屉原理界（≥ blocks）
     */
    public List<Integer> query(long fingerprint, int maxHamming) {
        if (maxHamming < 0 || maxHamming >= blocks) {
            throw new IllegalArgumentException("查询距离域 [0," + (blocks - 1)
                    + "]（抽屉原理界——实际 " + maxHamming + "）");
        }
        Map<Integer, Boolean> candidates = new HashMap<>();
        for (int b = 0; b < blocks; b++) {
            List<Integer> bucket = tables.get(b).get(blockOf(fingerprint, b));
            if (bucket == null) {
                continue;
            }
            for (int id : bucket) {
                candidates.put(id, Boolean.TRUE);
            }
        }
        int[] hits = candidates.keySet().stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(hits);
        List<Integer> result = new ArrayList<>();
        for (int id : hits) {
            if (Long.bitCount(fingerprint ^ fingerprints.get(id)) <= maxHamming) {
                result.add(id);
            }
        }
        return result;
    }

    /** 已登记指纹个数。 */
    public int size() {
        return fingerprints.size();
    }

    /** 两指纹海明距离（静态纯函数面）。 */
    public static int hammingDistance(long first, long second) {
        return Long.bitCount(first ^ second);
    }

    /** 第 blockIndex 块的块值（低 blockBits 位）。 */
    private long blockOf(long fingerprint, int blockIndex) {
        return (fingerprint >>> (blockIndex * blockBits))
                & ((1L << blockBits) - 1);
    }
}
