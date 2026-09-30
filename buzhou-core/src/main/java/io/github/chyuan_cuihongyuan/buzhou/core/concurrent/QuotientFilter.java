package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 商指纹过滤器（spec 10002 / X10005 / impl 2405）——Bender et al. 2012
 * 思想（「指纹分家+三 meta 位」——Facebook/ScyllaDB/ RocksDB 原型
 * 同源）：**f=h(key) 拆商 q（高位定槽）与余 r（低位定内容），同商
 * 元素成 run、run 按 home 序聚成 cluster；occupied/continuation/shifted
 * 三 meta 位免指针支撑原位删除友好的移位结构**——布隆无删除、
 * 布谷鸟踢箭换巢之间的第三形态（本件为成员面：add/mightContain）。
 *指纹混合常数取黄金比例 64 位混洗（SplitMix64 终结化同款常数家族，
 * 本地私有防跨件耦合）；装载契约（文献已知边界入档）：元素只右移
 * 不环绕——表尾簇无右余量时插入 fail-fast（中段仍有空位也可能触发），
 * 生产建议水位 ≤50% 并预留尾部余量；满容/尾满 fail-fast；
 * 同输入同态确定。
 */
public final class QuotientFilter {

    /** 黄金比例 64 位常数（SplitMix64 终结化同款——指纹混洗用）。 */
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /** 混洗三轮异或移位（SplitMix64 finalizer 同族）。 */
    private static final int MIX_SHIFT_A = 30;
    private static final int MIX_SHIFT_B = 27;
    private static final int MIX_SHIFT_C = 31;

    /** 商位数上界（槽表 2^q 需 int 下标安全）。 */
    private static final int MAX_QUOTIENT_BITS = 16;

    /** 商+余总位宽上界（指纹存 int，≤32 减符号位安全余量）。 */
    private static final int MAX_TOTAL_BITS = 31;

    private final int quotientBits;
    private final int remainderBits;
    private final int capacity;
    private final long remainderMask;

    private final long[] remainders;
    private final boolean[] occupied;
    private final boolean[] continuation;
    private final boolean[] shifted;

    private int size;

    /**
     * 构造（capacity=2^quotientBits 槽，每槽余数 remainderBits 位）。
     *
     * @throws IllegalArgumentException 商/余位宽越界
     */
    public QuotientFilter(int quotientBits, int remainderBits) {
        if (quotientBits < 1 || quotientBits > MAX_QUOTIENT_BITS) {
            throw new IllegalArgumentException("商位数域 [1," + MAX_QUOTIENT_BITS
                    + "]（实际 " + quotientBits + "）");
        }
        if (remainderBits < 1 || quotientBits + remainderBits > MAX_TOTAL_BITS) {
            throw new IllegalArgumentException("余数位宽非负且商+余≤" + MAX_TOTAL_BITS
                    + "（实际 " + remainderBits + "）");
        }
        this.quotientBits = quotientBits;
        this.remainderBits = remainderBits;
        this.capacity = 1 << quotientBits;
        this.remainderMask = (1L << remainderBits) - 1;
        this.remainders = new long[capacity];
        this.occupied = new boolean[capacity];
        this.continuation = new boolean[capacity];
        this.shifted = new boolean[capacity];
    }

    /**
     * 加入键（集合语义——重复返回 false 不入）。
     *
     * @throws IllegalStateException 槽表满（无空闲槽承接移位）
     */
    public boolean add(long key) {
        long fingerprint = mix(key);
        int quotient = (int) (fingerprint >>> remainderBits) & (capacity - 1);
        long remainder = fingerprint & remainderMask;
        if (occupied[quotient]) {
            int runStart = findRunStart(quotient);
            int runEnd = runEnd(runStart);
            for (int i = runStart; i <= runEnd; i++) {
                if (remainders[i] == remainder) {
                    return false;
                }
            }
            int insert = runEnd + 1;
            int free = firstFreeSlot(insert);
            shiftRight(insert, free);
            place(insert, remainder, true, insert != quotient);
        } else {
            // 先判槽占用态再置 occupied——occupied[q] 此处必假，cont/shifted 假即
            // 自由立位（cluster 起点判定的自指陷阱：先置位再查 isTaken 必自咬）
            boolean freeStanding = !shifted[quotient] && !continuation[quotient];
            occupied[quotient] = true;
            if (freeStanding) {
                place(quotient, remainder, false, false);
            } else {
                int clusterStart = quotient;
                while (shifted[clusterStart]) {
                    clusterStart--;
                }
                int preceding = 0;
                for (int h = clusterStart + 1; h < quotient; h++) {
                    if (occupied[h]) {
                        preceding++;
                    }
                }
                int j = clusterStart;
                while (preceding > 0) {
                    j++;
                    while (continuation[j]) {
                        j++;
                    }
                    preceding--;
                }
                // 游走停在第 preceding 个 run 的起点——须推到其末槽再 +1，
                // 否则 insert 落进 run 中段会把长 run 劈开孤儿化尾部
                while (j + 1 < capacity && continuation[j + 1]) {
                    j++;
                }
                int insert = j + 1;
                int free = firstFreeSlot(insert);
                shiftRight(insert, free);
                place(insert, remainder, false, insert != quotient);
            }
        }
        size++;
        return true;
    }

    /** 近似成员判定（无误报否定；有基率相关的小概率误报）。 */
    public boolean mightContain(long key) {
        long fingerprint = mix(key);
        int quotient = (int) (fingerprint >>> remainderBits) & (capacity - 1);
        long remainder = fingerprint & remainderMask;
        if (!occupied[quotient]) {
            return false;
        }
        int runStart = findRunStart(quotient);
        int runEnd = runEnd(runStart);
        for (int i = runStart; i <= runEnd; i++) {
            if (remainders[i] == remainder) {
                return true;
            }
        }
        return false;
    }

    /** 已接收元素个数。 */
    public int size() {
        return size;
    }

    /** 槽表容量（2^quotientBits）。 */
    public int capacity() {
        return capacity;
    }

    /** 指纹混洗（SplitMix64 finalizer 同族三轮，确定无状态）。 */
    private static long mix(long key) {
        long z = key + GOLDEN_GAMMA;
        z = (z ^ (z >>> MIX_SHIFT_A)) * GOLDEN_GAMMA;
        z = (z ^ (z >>> MIX_SHIFT_B)) * GOLDEN_GAMMA;
        return z ^ (z >>> MIX_SHIFT_C);
    }

    /** run(q) 首槽（occupied[q] 必真；沿 cluster 起点按 home 序游走）。 */
    private int findRunStart(int quotient) {
        int s = quotient;
        while (shifted[s]) {
            s--;
        }
        if (s == quotient) {
            return s;
        }
        int rank = 0;
        for (int h = s + 1; h <= quotient; h++) {
            if (occupied[h]) {
                rank++;
            }
        }
        int j = s;
        while (rank > 0) {
            j++;
            while (continuation[j]) {
                j++;
            }
            rank--;
        }
        return j;
    }

    /** run 末槽（向后吃 continuation）。 */
    private int runEnd(int runStart) {
        int end = runStart;
        while (end + 1 < capacity && continuation[end + 1]) {
            end++;
        }
        return end;
    }

    /** 第一个空闲槽（三 meta 位全假；越容即 fail-fast）。 */
    private int firstFreeSlot(int from) {
        int t = from;
        while (t < capacity && isTaken(t)) {
            t++;
        }
        if (t >= capacity) {
            throw new IllegalStateException("商过滤器已满（capacity=" + capacity
                    + "，size=" + size + "）");
        }
        return t;
    }

    /** [from..free-1] 右移一格（moved 元素标 shifted；cont 原样随迁）。 */
    private void shiftRight(int from, int free) {
        for (int i = free; i > from; i--) {
            remainders[i] = remainders[i - 1];
            continuation[i] = continuation[i - 1];
            shifted[i] = true;
        }
    }

    private void place(int slot, long remainder, boolean cont, boolean shiftedFlag) {
        remainders[slot] = remainder;
        continuation[slot] = cont;
        shifted[slot] = shiftedFlag;
    }

    private boolean isTaken(int slot) {
        return occupied[slot] || continuation[slot] || shifted[slot];
    }
}
