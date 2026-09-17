package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.DeterministicHash;

/**
 * Maglev 哈希（spec 3032 / T5065 / impl 2033）——Google Maglev 负载
 * 均衡器思想（2016 论文）：每节点生成**步进置换序列**
 * (offset+j·skip) mod M，轮询各节点抢占查找表空位直至填满——
 * 均匀（每节点份额差 ≤1 槽）且**最小扰动**（节点增删只挪 ~1/n
 * 键，朴素取模全量重排病的根治）；查表 O(1) 常数级。查找表
 * 大小须为素数（置换满周期遍历的前提，构造期校验）；节点列表
 * **重复即权重**（重复 k 次占 ~k/n 槽）。
 *
 * <p>键与节点种子复用 DeterministicHash（确定性可复算可审计）。
 */
public final class MaglevHash {

    private final List<String> candidates;
    private final int[] table;

    /** 候选非空 / 表大小为素数且 ≥ 候选数（构造期 O(√M) 校验）。 */
    public MaglevHash(List<String> candidates, int tableSize) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates 非空");
        }
        if (tableSize < candidates.size() || !isPrime(tableSize)) {
            throw new IllegalArgumentException("tableSize 须为 ≥ 候选数的素数：" + tableSize);
        }
        this.candidates = List.copyOf(candidates);
        this.table = buildTable(this.candidates, tableSize);
    }

    /** 键 → 节点名（查表 O(1)：table[hash(key) mod M]）。 */
    public String nodeOf(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        long hash = DeterministicHash.hash64(key);
        int slot = (int) Long.remainderUnsigned(hash, table.length);
        return candidates.get(table[slot]);
    }

    /** 查找表大小。 */
    public int tableSize() {
        return table.length;
    }

    /** 候选登记数（含重复）。 */
    public int candidateCount() {
        return candidates.size();
    }

    /** 节点占槽数（同名单重复合并——分布对账面）。 */
    public long entriesOf(String node) {
        long count = 0;
        for (int slotOwner : table) {
            if (candidates.get(slotOwner).equals(node)) {
                count++;
            }
        }
        return count;
    }

    /** 置换抢占填表（Maglev 核心：轮询候选，各按步进序列抢空位）。 */
    private static int[] buildTable(List<String> candidates, int tableSize) {
        int n = candidates.size();
        long[] offset = new long[n];
        long[] skip = new long[n];
        long[] cursor = new long[n];
        for (int i = 0; i < n; i++) {
            String node = candidates.get(i);
            offset[i] = Long.remainderUnsigned(DeterministicHash.hash64(node + "#offset"), tableSize);
            skip[i] = 1 + Long.remainderUnsigned(DeterministicHash.hash64(node + "#skip"), tableSize - 1);
        }
        int[] table = new int[tableSize];
        java.util.Arrays.fill(table, -1);
        int filled = 0;
        while (filled < tableSize) {
            for (int i = 0; i < n; i++) {
                int slot;
                do {
                    slot = (int) ((offset[i] + cursor[i] * skip[i]) % tableSize);
                    cursor[i]++;
                } while (table[slot] != -1);
                table[slot] = i;
                filled++;
                if (filled == tableSize) {
                    break;
                }
            }
        }
        return table;
    }

    /** 素数判定（试除——构造期一次性，表量级下 √M 可忽略）。 */
    private static boolean isPrime(int value) {
        if (value < 2) {
            return false;
        }
        for (int divisor = 2; (long) divisor * divisor <= value; divisor++) {
            if (value % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}
