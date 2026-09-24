package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Xor Filter 异或过滤器（spec 5042 / T6185 / impl 2193）——
 * Graf-Lemire xor filter 思想（ClickHouse/DuckDB 社区，
 * Bloom 后继）：静态键集构建 8 位指纹数组，每键经三个
 * 独立散列定位三槽——查询=三槽指纹异或等于键指纹；构建
 * 用 **剥洋葱**（peeling：反复摘除度为一的槽-键边，逆序
 * 回填指纹）——比 Bloom 更省内存（~9.84 bits/键 vs
 * Bloom ~10+）且查询无探针级联；剥皮失败换种子重试
 * （确定性盐序列——同键集同结果）。确定性无时间依赖，
 * 无假阴性（成员必过）、假阳性率 ≤ 2^-8 量级。
 *
 * <p>与 SessionBloomFilter/CuckooFilter（session）同族不同面：
 * 异或三槽静态集 vs 布尔位阵/布谷鸟指纹动态集。
 */
public final class XorFilter {

    private static final int BITS_PER_KEY_NUMERATOR = 123;

    private static final int BITS_PER_KEY_DENOMINATOR = 100;

    private static final int BASE_SLOTS = 32;

    private static final int SLOTS_PER_KEY_SET = 3;

    private static final int FINGERPRINT_MODULUS = 255;

    private static final int MAX_BUILD_ATTEMPTS = 64;

    private static final long SPLITMIX_GAMMA = 0x9e3779b97f4a7c15L;

    private static final long SPLITMIX_MULT_A = 0xbf58476d1ce4e5b9L;

    private static final long SPLITMIX_MULT_B = 0x94d049bb133111ebL;

    private final byte[] fingerprints;
    private final long elementCount;
    private final long saltBase;

    /** 静态键集定构（null/重复键、空集 fail-fast）。 */
    public XorFilter(Iterable<Long> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("keys 非空");
        }
        List<Long> distinct = new ArrayList<>();
        for (Long key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("key 非空");
            }
            if (distinct.contains(key)) {
                throw new IllegalArgumentException("键重复：" + key);
            }
            distinct.add(key);
        }
        if (distinct.isEmpty()) {
            throw new IllegalArgumentException("键集非空");
        }
        this.elementCount = distinct.size();
        long needed = BASE_SLOTS
                + (distinct.size() * BITS_PER_KEY_NUMERATOR + BITS_PER_KEY_DENOMINATOR - 1)
                        / BITS_PER_KEY_DENOMINATOR;
        long slots = ((needed + SLOTS_PER_KEY_SET - 1) / SLOTS_PER_KEY_SET) * SLOTS_PER_KEY_SET;
        byte[] built = null;
        long usedSaltBase = 0;
        for (int attempt = 0; attempt < MAX_BUILD_ATTEMPTS && built == null; attempt++) {
            built = tryBuild(distinct, (int) slots, attempt);
            if (built != null) {
                usedSaltBase = attempt;
            }
        }
        if (built == null) {
            throw new IllegalStateException("剥皮连续失败（键集病态）：" + distinct.size());
        }
        this.fingerprints = built;
        this.saltBase = usedSaltBase;
    }

    /** 成员判定（成员恒真——无假阴性；非成员小概率误真）。 */
    public boolean contains(long key) {
        int slot1 = mod(mix(key, saltBase), fingerprints.length);
        int slot2 = mod(mix(key, saltBase + 1), fingerprints.length);
        int slot3 = mod(mix(key, saltBase + 2), fingerprints.length);
        byte expected = fingerprintOf(key);
        byte actual = (byte) (fingerprints[slot1] ^ fingerprints[slot2] ^ fingerprints[slot3]);
        return expected == actual;
    }

    /** 指纹数组校验和读数（同键集同校验和——构建确定性可审计）。 */
    public long checksum() {
        long sum = 0;
        for (byte fingerprint : fingerprints) {
            sum = sum * 31 + fingerprint;
        }
        return sum;
    }

    /** 槽位数组长度读数（3 的倍数）。 */
    public int arrayLength() {
        return fingerprints.length;
    }

    /** 键数读数。 */
    public long elementCount() {
        return elementCount;
    }

    private byte[] tryBuild(List<Long> keys, int slots, int attempt) {
        int[][] keySlots = new int[keys.size()][SLOTS_PER_KEY_SET];
        byte[] keyPrints = new byte[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            long key = keys.get(i);
            keySlots[i][0] = mod(mix(key, attempt), slots);
            keySlots[i][1] = mod(mix(key, attempt + 1), slots);
            keySlots[i][2] = mod(mix(key, attempt + 2), slots);
            keyPrints[i] = fingerprintOf(key);
        }
        int[] degree = new int[slots];
        for (int[] triple : keySlots) {
            for (int slot : triple) {
                degree[slot]++;
            }
        }
        boolean[] done = new boolean[keys.size()];
        Deque<int[]> peelOrder = new ArrayDeque<>();
        int remaining = keys.size();
        while (remaining > 0) {
            boolean progressed = false;
            for (int i = 0; i < keys.size(); i++) {
                if (done[i]) {
                    continue;
                }
                int peelSlot = -1;
                for (int slot : keySlots[i]) {
                    if (degree[slot] == 1) {
                        peelSlot = slot;
                        break;
                    }
                }
                if (peelSlot >= 0) {
                    done[i] = true;
                    peelOrder.addFirst(new int[]{peelSlot, i});
                    for (int slot : keySlots[i]) {
                        degree[slot]--;
                    }
                    remaining--;
                    progressed = true;
                }
            }
            if (!progressed) {
                return null;
            }
        }
        byte[] array = new byte[slots];
        for (int[] entry : peelOrder) {
            int slot = entry[0];
            int keyIndex = entry[1];
            byte value = keyPrints[keyIndex];
            for (int other : keySlots[keyIndex]) {
                if (other != slot) {
                    value ^= array[other];
                }
            }
            array[slot] = value;
        }
        return array;
    }

    private static byte fingerprintOf(long key) {
        long hash = mix(key, 3);
        byte print = (byte) (hash & FINGERPRINT_MODULUS);
        return print == 0 ? (byte) ((hash >>> 8) & FINGERPRINT_MODULUS) : print;
    }

    private static int mod(long value, int modulus) {
        long reduced = value % modulus;
        return (int) (reduced < 0 ? reduced + modulus : reduced);
    }

    private static long mix(long key, long salt) {
        long z = key + salt * SPLITMIX_GAMMA;
        z = (z ^ (z >>> 30)) * SPLITMIX_MULT_A;
        z = (z ^ (z >>> 27)) * SPLITMIX_MULT_B;
        return z ^ (z >>> 31);
    }
}
