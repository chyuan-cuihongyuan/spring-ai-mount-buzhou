package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 频率素描（spec 2007 / T3115 / impl 1558）——Caffeine W-TinyLFU 思想：
 * 4bit Count-Min 计数板做缓存准入门控——key 散列到相邻两槽，取计数
 * 较小槽 increment（防单 key 双槽独占倾斜），读数 = min(两槽)；
 * 4bit 饱和 15 不回绕（热点上限即 15——门控只需序不需精确值）。
 *
 * <p>确定性散列（FNV-1a 64 + splitmix64 终结——与 HllCardinalitySketch
 * 同款）；表为 2 的幂长 long[]，每 long 16 槽；synchronized 小临界区。
 */
public final class FrequencySketch {

    /** 4bit 槽饱和上限（门控只需序——15 封顶防回绕）。 */
    public static final int SATURATION = 15;

    /** 最小表长（long 数，2 的幂下限）。 */
    public static final int MIN_TABLE_LENGTH = 16;

    private static final long SLOT_MASK = 0xFL;
    private static final int SLOTS_PER_LONG = 16;

    private final long[] table;
    private final int slotMask; // 槽总数 − 1（2 的幂）

    /** 契约：expectedEntries ≥ 1（表长 = 2^ceil(log2(entries×2/16))，至少 16 long）。 */
    public FrequencySketch(int expectedEntries) {
        if (expectedEntries < 1) {
            throw new IllegalArgumentException("expectedEntries 须 ≥ 1：" + expectedEntries);
        }
        int slotCount = Integer.highestOneBit(Math.max(expectedEntries * 2, MIN_TABLE_LENGTH * SLOTS_PER_LONG) - 1) << 1;
        this.slotMask = slotCount - 1;
        this.table = new long[Math.max(MIN_TABLE_LENGTH, slotCount / SLOTS_PER_LONG)];
    }

    /** 记一次访问：相邻两槽取较小者 +1（碰撞双方共享增长——门控公平）。 */
    public synchronized void increment(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        long hash = hash64(key);
        int i = (int) (hash & slotMask);
        int j = (i + 1) & slotMask;
        int vi = valueAt(i);
        int vj = valueAt(j);
        if (vi <= vj) {
            writeSlot(i, vi + 1);
        } else {
            writeSlot(j, vj + 1);
        }
    }

    /** 访问频率读数 = min(相邻两槽)（Count-Min 下界语义——碰撞只低估不高估）。 */
    public synchronized int frequency(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        long hash = hash64(key);
        int i = (int) (hash & slotMask);
        int j = (i + 1) & slotMask;
        return Math.min(valueAt(i), valueAt(j));
    }

    private int valueAt(int slot) {
        int word = slot / SLOTS_PER_LONG;
        int offset = (slot % SLOTS_PER_LONG) * 4;
        return (int) ((table[word] >>> offset) & SLOT_MASK);
    }

    private void writeSlot(int slot, int value) {
        int word = slot / SLOTS_PER_LONG;
        int offset = (slot % SLOTS_PER_LONG) * 4;
        int v = Math.min(value, SATURATION); // 饱和封顶
        table[word] = (table[word] & ~(SLOT_MASK << offset)) | ((long) v << offset);
    }

    /** FNV-1a 64 + splitmix64 终结（与 HllCardinalitySketch 同款确定性散列）。 */
    private static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
