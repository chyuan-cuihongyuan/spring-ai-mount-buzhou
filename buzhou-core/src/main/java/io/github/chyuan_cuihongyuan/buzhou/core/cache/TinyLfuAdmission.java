package io.github.chyuan_cuihongyuan.buzhou.core.cache;

/**
 * TinyLFU Admission 准入策略（spec 5046 / T6193 / impl 2197）——
 * Caffeine W-TinyLFU 准入面思想：4 位饱和计数 sketch
 * （两行 count-min——估计取两行最小抑制哈希碰撞高估），
 * `record` 记键访问频次（饱和不溢出），`admit` 裁决——
 * 候选频次 ≥ 受害者频次才准入（新键不再无条件挤掉热键，
 * 一次性扫描被拒之门外）；计数达阈值全体**减半老化**
 （旧热点平稳淡出， sketch 不被历史占死）。确定性无时间
 依赖。
 *
 * <p>与 SlruCache（spec 5039）同族不同面：准入裁决 vs
 * 双段驱逐；与 FrequencySketch（metrics，若在）互补。
 */
public final class TinyLfuAdmission<K> {

    /** 4 位饱和上限。 */
    private static final int SATURATION = 15;

    /** count-min 行数。 */
    private static final int ROWS = 2;

    private final int[] table;
    private final int columns;
    private final int mask;
    private final int resetEvery;
    private int sinceReset;
    private long resetCount;

    /** 定构（expectedKeys≥1 → 列数=其后首个 2 的幂；老化阈值=列数半）。 */
    public TinyLfuAdmission(int expectedKeys) {
        if (expectedKeys < 1) {
            throw new IllegalArgumentException("expectedKeys≥1：" + expectedKeys);
        }
        int power = 1;
        while (power < expectedKeys) {
            power <<= 1;
        }
        this.columns = power;
        this.mask = power - 1;
        this.table = new int[ROWS * power];
        this.resetEvery = Math.max(1, power / 2);
    }

    /** 记键一次访问（饱和加；达老化阈值全体减半）。 */
    public void record(K key) {
        int[] slots = slotsOf(key);
        if (table[slots[0]] < SATURATION) {
            table[slots[0]]++;
        }
        if (table[columns + slots[1]] < SATURATION) {
            table[columns + slots[1]]++;
        }
        sinceReset++;
        if (sinceReset >= resetEvery) {
            age();
        }
    }

    /** 频次估计（count-min：两行取最小）。 */
    public int estimate(K key) {
        int[] slots = slotsOf(key);
        return Math.min(table[slots[0]], table[columns + slots[1]]);
    }

    /**
     * 准入裁决（候选估计 ≥ 受害者估计才准入——
     * 热者不让位、冷者不挤占）。
     */
    public boolean admit(K candidate, K victim) {
        return estimate(candidate) >= estimate(victim);
    }

    /** 老化次数读数（历史淡出的诚实可见）。 */
    public long resetCount() {
        return resetCount;
    }

    /** 老化阈值读数（记录数达此值全体减半）。 */
    public int resetEvery() {
        return resetEvery;
    }

    /** 自上次老化以来的记录数读数。 */
    public int sinceReset() {
        return sinceReset;
    }

    private void age() {
        for (int i = 0; i < table.length; i++) {
            table[i] >>= 1;
        }
        sinceReset = 0;
        resetCount++;
    }

    private int[] slotsOf(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        int hash = spread(key.hashCode());
        return new int[]{hash & mask, (hash >>> 16) & mask};
    }

    private static int spread(int value) {
        int z = value * 0x9e3779b9;
        z ^= z >>> 16;
        z *= 0x85ebca6b;
        z ^= z >>> 13;
        return z;
    }
}
