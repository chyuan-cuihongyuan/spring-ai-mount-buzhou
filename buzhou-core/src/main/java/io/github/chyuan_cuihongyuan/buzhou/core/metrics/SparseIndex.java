package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

/**
 * Sparse Index 稀疏索引（spec 5021 / T6143 / impl 2172）——
 * LSM/SSTable sparse index 思想：排序块数据只为**块首键**建
 * 索引（索引量级 O(块数)），`locate(key)` 二分找最后一个
 * firstKey ≤ key 的块——该块**可能**包含 key（稀疏索引只
 * 承诺范围不承诺存在）；key 早于首块 → -1（诚实不在）。
 * 每键全索引（索引与数据同量级）与全块线性扫（读放大）的
 * 病解。
 *
 * <p>与 Bitcask 键目录（R15 内存全索引）同族不同面。
 */
public final class SparseIndex {

    /**
     * 索引块项。
     *
     * @param blockId 块标识
     * @param firstKey 块首键（升序校验键）
     */
    public record Block(long blockId, String firstKey) {

        public Block {
            if (firstKey == null || firstKey.isEmpty()) {
                throw new IllegalArgumentException("firstKey 非空");
            }
        }
    }

    private final List<Block> blocks;

    /** 定构（块集非空且 firstKey 严格升序，否则 fail-fast）。 */
    public SparseIndex(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("块集非空");
        }
        for (int i = 1; i < blocks.size(); i++) {
            if (blocks.get(i).firstKey().compareTo(blocks.get(i - 1).firstKey()) <= 0) {
                throw new IllegalArgumentException("块首键需严格升序："
                        + blocks.get(i - 1) + " → " + blocks.get(i));
            }
        }
        this.blocks = List.copyOf(blocks);
    }

    /**
     * 定位可能包含 key 的块（最后一个 firstKey ≤ key 的块）。
     *
     * @return 块 id；key 早于首块返回 -1（诚实不在）
     */
    public long locate(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非 null");
        }
        int low = 0;
        int high = blocks.size() - 1;
        int answer = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (blocks.get(mid).firstKey().compareTo(key) <= 0) {
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer < 0 ? -1 : blocks.get(answer).blockId();
    }

    /** 块数读数。 */
    public int blockCount() {
        return blocks.size();
    }

    /** 块集读数（确定性）。 */
    public List<Block> blocks() {
        return blocks;
    }
}
