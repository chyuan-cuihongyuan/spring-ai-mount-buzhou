package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 完美哈希（spec 8013 / V8027 / impl 2315）——
 * Czech, Havas & Majewski CHM 两级思想（gperf 同源）：
 * **静态键集两级参数扫描做到零碰撞**——一级散列分桶（桶内
 * 键数有界），二级每桶独立参数扫描桶内单射；lookup 命中
 * [0,n) 唯一槽位、集合外键 −1 诚实缺省（完美哈希不认识
 * 集合外的键）；构建尝试上限后 fail-fast（参数空间耗尽——
 * 诚实拒绝而非吐错表）；种子注入（同种子同表可回放）；
 * null 键/重复键/空集 fail-fast。静态语义（不支持动态
 * 插删——明示）。
 *
 * <p>与 DeterministicHash（spec 2057）同族不同面：通用
 * 确定性散列（碰撞概率可感）vs 静态集无碰撞定位。
 */
public final class PerfectHash {

    /** 参数扫描尝试上限（超限诚实拒绝）。 */
    private static final int DEFAULT_MAX_ATTEMPTS = 4096;

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private final String[] slots;
    private final int[] binOffset;
    private final int[] binModulus;
    private final long[] binSeed;
    private final long baseSeed;
    private final int level1Bins;
    private final int level1Mask;

    private PerfectHash(String[] slots, int[] binOffset, int[] binModulus, long[] binSeed,
                        long baseSeed, int level1Bins, int level1Mask) {
        this.slots = slots;
        this.binOffset = binOffset;
        this.binModulus = binModulus;
        this.binSeed = binSeed;
        this.baseSeed = baseSeed;
        this.level1Bins = level1Bins;
        this.level1Mask = level1Mask;
    }

    /** 构建（null 键/重复键/空集 fail-fast；参数耗尽 IAE）。 */
    public static PerfectHash build(List<String> keys, long seed) {
        return build(keys, seed, DEFAULT_MAX_ATTEMPTS);
    }

    /** 构建（maxAttempts 供测试注入构建失败路径）。 */
    static PerfectHash build(List<String> keys, long seed, int maxAttempts) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("键集非空");
        }
        Set<String> distinct = new HashSet<>();
        for (String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("键非空引用");
            }
            if (!distinct.add(key)) {
                throw new IllegalArgumentException("键重复（" + key + "）");
            }
        }
        int n = keys.size();
        int bins = Integer.highestOneBit(Math.max(1, n - 1)) * 2;
        int level1Mask = bins - 1;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long base = seed + attempt;
            List<List<String>> partition = new ArrayList<>(bins);
            for (int i = 0; i < bins; i++) {
                partition.add(new ArrayList<>());
            }
            for (String key : keys) {
                partition.get(h1(key, base, level1Mask)).add(key);
            }
            int[][] sizes = new int[bins][1];
            int total = 0;
            boolean oversized = false;
            for (int bin = 0; bin < bins; bin++) {
                sizes[bin][0] = partition.get(bin).size();
                total += sizes[bin][0] * sizes[bin][0];
                if (sizes[bin][0] > 32) {
                    oversized = true;
                }
            }
            if (oversized) {
                continue;
            }
            int[] binOffset = new int[bins];
            int[] binModulus = new int[bins];
            long[] binSeed = new long[bins];
            String[] slots = new String[total];
            boolean ok = true;
            for (int bin = 0; bin < bins && ok; bin++) {
                List<String> members = partition.get(bin);
                int s = members.size();
                binOffset[bin] = totalFor(partition, bin);
                binModulus[bin] = s * s;
                if (s == 0) {
                    binSeed[bin] = 0;
                    continue;
                }
                boolean placed = false;
                for (int binAttempt = 0; binAttempt < maxAttempts; binAttempt++) {
                    long binSeedValue = base * 31 + bin * 131 + binAttempt;
                    Set<Integer> used = new HashSet<>();
                    boolean injective = true;
                    for (String member : members) {
                        int pos = h2(member, binSeedValue, binModulus[bin]);
                        if (!used.add(pos)) {
                            injective = false;
                            break;
                        }
                    }
                    if (injective) {
                        for (String member : members) {
                            int pos = h2(member, binSeedValue, binModulus[bin]);
                            slots[binOffset[bin] + pos] = member;
                        }
                        binSeed[bin] = binSeedValue;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    ok = false;
                }
            }
            if (ok) {
                return new PerfectHash(slots, binOffset, binModulus, binSeed, base, bins, level1Mask);
            }
        }
        throw new IllegalArgumentException("参数空间耗尽（尝试 " + maxAttempts + " 轮未收敛）");
    }

    /** 键的唯一槽位 [0,n)（集合外键 −1 诚实缺省）。 */
    public int lookup(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非空引用");
        }
        int bin = h1(key, baseSeed, level1Mask);
        if (binModulus[bin] == 0) {
            return -1;
        }
        int pos = h2(key, binSeed[bin], binModulus[bin]);
        int slot = binOffset[bin] + pos;
        return key.equals(slots[slot]) ? slot : -1;
    }

    /** 键数（= 槽位域大小——双射承诺）。 */
    public int size() {
        int count = 0;
        for (String slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    private static int h1(String key, long seed, int mask) {
        return (int) ((mix(key.hashCode(), seed)) & mask);
    }

    private static int h2(String key, long seed, int modulus) {
        return (int) (Long.remainderUnsigned(mix(key.hashCode(), seed), modulus));
    }

    private static long mix(int value, long seed) {
        long x = (value & 0xFFFFFFFFL) ^ seed;
        x *= GOLDEN;
        x ^= x >>> 29;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 32;
        return x;
    }

    private static int totalFor(List<List<String>> partition, int upTo) {
        int total = 0;
        for (int bin = 0; bin < upTo; bin++) {
            int s = partition.get(bin).size();
            total += s * s;
        }
        return total;
    }
}
