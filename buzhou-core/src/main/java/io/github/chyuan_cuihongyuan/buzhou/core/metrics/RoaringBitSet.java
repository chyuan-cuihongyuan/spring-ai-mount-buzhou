package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Roaring 压缩位图（spec 5001 / T6103 / impl 2152）——容器化
 * 自适应压缩思想（RoaringBitmap/Lucene/Spark）：高 16 位分桶
 * （container key），桶内按元素数自适应——稠密桶（≥ {@value
 * #DENSE_THRESHOLD}）转 {@code long[]} 位图容器、稀疏桶走
 * 短整型有序数组容器——稠密段省内存、稀疏段不浪费。裸
 * HashSet 全对象存储与固定粒度位图（稀疏 id 同样爆炸）的病解。
 *
 * <p>与 HllCardinalitySketch 互补：精确压缩集合 vs 概率基数。
 * 迭代序确定性：桶 key 升序 + 桶内升序。
 */
public final class RoaringBitSet {

    /** 桶内转位图容器的元素数阈值（Roaring 同量级）。 */
    private static final int DENSE_THRESHOLD = 4096;

    /** 桶内位图容器字数（桶内 16 位全域 65536 bit）。 */
    private static final int DENSE_WORDS = 1024;

    private final TreeMap<Integer, Container> containers = new TreeMap<>();
    private long cardinality;

    /** 加入元素（负值 fail-fast；已存在幂等）。 */
    public void add(int value) {
        checkRange(value);
        int key = high(value);
        Container container = containers.get(key);
        if (container == null) {
            container = new Container();
            containers.put(key, container);
        }
        if (container.add(low(value))) {
            cardinality++;
        }
    }

    /** 移除元素（不存在幂等）。 */
    public void remove(int value) {
        checkRange(value);
        Container container = containers.get(high(value));
        if (container != null && container.remove(low(value))) {
            cardinality--;
        }
    }

    /** 是否包含。 */
    public boolean contains(int value) {
        checkRange(value);
        Container container = containers.get(high(value));
        return container != null && container.contains(low(value));
    }

    /** 精确基数读数（增量维护）。 */
    public long cardinality() {
        return cardinality;
    }

    /** 桶（容器）数读数。 */
    public int containerCount() {
        return containers.size();
    }

    /** 交集（返回新位图；两操作数不变）。 */
    public static RoaringBitSet and(RoaringBitSet left, RoaringBitSet right) {
        RoaringBitSet result = new RoaringBitSet();
        for (Map.Entry<Integer, Container> entry : left.containers.entrySet()) {
            Container other = right.containers.get(entry.getKey());
            if (other != null) {
                result.containers.put(entry.getKey(), entry.getValue().and(other));
            }
        }
        result.cardinality = result.countAll();
        return result;
    }

    /** 并集（返回新位图；两操作数不变）。 */
    public static RoaringBitSet or(RoaringBitSet left, RoaringBitSet right) {
        RoaringBitSet result = new RoaringBitSet();
        result.mergeIn(left);
        result.mergeIn(right);
        result.cardinality = result.countAll();
        return result;
    }

    private void mergeIn(RoaringBitSet other) {
        for (Map.Entry<Integer, Container> entry : other.containers.entrySet()) {
            Container mine = containers.get(entry.getKey());
            containers.put(entry.getKey(), mine == null ? entry.getValue().copy() : mine.or(entry.getValue()));
        }
    }

    private long countAll() {
        long total = 0;
        for (Container container : containers.values()) {
            total += container.cardinality;
        }
        return total;
    }

    private static int high(int value) {
        return value >>> 16;
    }

    private static int low(int value) {
        return value & 0xFFFF;
    }

    private static void checkRange(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("元素需非负：" + value);
        }
    }

    /** 桶内容器：稀疏有序 short 数组 ↔ 稠密 long[] 位图自适应。 */
    private static final class Container {

        private short[] sparse = new short[0];
        private long[] dense;
        private int cardinality;

        private boolean add(int low) {
            if (dense != null) {
                boolean absent = (dense[low >>> 6] & (1L << (low & 63))) == 0;
                if (absent) {
                    dense[low >>> 6] |= 1L << (low & 63);
                    cardinality++;
                }
                return absent;
            }
            int position = binarySearch(low);
            if (position >= 0) {
                return false;
            }
            int insert = -(position + 1);
            sparse = Arrays.copyOf(sparse, sparse.length + 1);
            System.arraycopy(sparse, insert, sparse, insert + 1, sparse.length - insert - 1);
            sparse[insert] = (short) low;
            cardinality++;
            if (cardinality >= DENSE_THRESHOLD) {
                toDense();
            }
            return true;
        }

        private boolean remove(int low) {
            if (dense != null) {
                boolean present = (dense[low >>> 6] & (1L << (low & 63))) != 0;
                if (present) {
                    dense[low >>> 6] &= ~(1L << (low & 63));
                    cardinality--;
                }
                return present;
            }
            int position = binarySearch(low);
            if (position < 0) {
                return false;
            }
            System.arraycopy(sparse, position + 1, sparse, position, sparse.length - position - 1);
            sparse = Arrays.copyOf(sparse, sparse.length - 1);
            cardinality--;
            return true;
        }

        private boolean contains(int low) {
            if (dense != null) {
                return (dense[low >>> 6] & (1L << (low & 63))) != 0;
            }
            return binarySearch(low) >= 0;
        }

        private int binarySearch(int low) {
            return Arrays.binarySearch(sparse, (short) low);
        }

        private void toDense() {
            dense = new long[DENSE_WORDS];
            for (short item : sparse) {
                int low = item & 0xFFFF;
                dense[low >>> 6] |= 1L << (low & 63);
            }
            sparse = null;
        }

        private Container and(Container other) {
            Container result = new Container();
            if (dense != null && other.dense != null) {
                result.dense = new long[DENSE_WORDS];
                for (int i = 0; i < DENSE_WORDS; i++) {
                    result.dense[i] = dense[i] & other.dense[i];
                    result.cardinality += Long.bitCount(result.dense[i]);
                }
                return result;
            }
            for (int i = 0; i < cardinality; i++) {
                int item = itemAt(i);
                if (other.contains(item)) {
                    result.add(item);
                }
            }
            return result;
        }

        private Container or(Container other) {
            Container result = copy();
            for (int i = 0; i < other.cardinality; i++) {
                result.add(other.itemAt(i));
            }
            return result;
        }

        private Container copy() {
            Container result = new Container();
            if (dense != null) {
                result.dense = dense.clone();
                result.cardinality = cardinality;
            } else {
                result.sparse = sparse.clone();
                result.cardinality = cardinality;
            }
            return result;
        }

        private int itemAt(int position) {
            if (dense != null) {
                int remaining = position;
                for (int word = 0; word < DENSE_WORDS; word++) {
                    int bits = Long.bitCount(dense[word]);
                    if (remaining < bits) {
                        long wordValue = dense[word];
                        for (int bit = 0; bit < Long.SIZE; bit++) {
                            remaining -= (int) ((wordValue >>> bit) & 1L);
                            if (remaining < 0) {
                                return (word << 6) | bit;
                            }
                        }
                    }
                    remaining -= bits;
                }
                return -1;
            }
            return sparse[position] & 0xFFFF;
        }
    }
}
