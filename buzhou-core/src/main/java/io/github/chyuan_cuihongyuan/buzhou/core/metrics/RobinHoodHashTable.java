package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Optional;

/**
 * Robin Hood Hash Table 劫富济贫哈希表（spec 5045 / T6191 /
 * impl 2196）——Robin Hood 开放寻址思想：线性探测插入时
 * 若来键的探测距离超过在位键的探测距离即**换位**（富者
 * 让位贫者）——探测距离方差收窄、长簇截断（普通线性
 * 探测聚类越长越长、命中距离无上界的病解）；删除用
 * **后向搬移**（回填空洞维持不变量，无墓碑）；超 0.75
 * 负载倍容重排。确定性无时间依赖。
 *
 * <p>与 MemTable（spec 5024，可变有序表）同族不同面：
 * 哈希无序 O(1) 面 vs 有序 O(log n) 面。
 */
public final class RobinHoodHashTable<K, V> {

    /** 负载上限（超过即倍容）。 */
    private static final double MAX_LOAD_FACTOR = 0.75;

    /** 初始容量下限。 */
    private static final int MIN_CAPACITY = 8;

    private Object[] keys;
    private Object[] values;
    private int capacity;
    private int size;
    private long resizeCount;

    /** 定构（容量按下限保护；capacity<1 fail-fast）。 */
    public RobinHoodHashTable(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity≥1：" + initialCapacity);
        }
        this.capacity = Math.max(initialCapacity, MIN_CAPACITY);
        this.keys = new Object[this.capacity];
        this.values = new Object[this.capacity];
    }

    /** 放入/覆盖（返回旧值；null 键值 fail-fast）。 */
    @SuppressWarnings("unchecked")
    public Optional<V> put(K key, V value) {
        requireKey(key);
        if (value == null) {
            throw new IllegalArgumentException("value 非空");
        }
        if ((size + 1) > capacity * MAX_LOAD_FACTOR) {
            resize();
        }
        int slot = homeOf(key);
        int distance = 0;
        K incomingKey = key;
        V incomingValue = value;
        while (true) {
            if (keys[slot] == null) {
                keys[slot] = incomingKey;
                values[slot] = incomingValue;
                size++;
                return Optional.empty();
            }
            if (keys[slot].equals(incomingKey)) {
                V previous = (V) values[slot];
                values[slot] = incomingValue;
                return Optional.of(previous);
            }
            int occupiedDistance = distance(homeOf((K) keys[slot]), slot);
            if (occupiedDistance < distance) {
                K displacedKey = (K) keys[slot];
                V displacedValue = (V) values[slot];
                keys[slot] = incomingKey;
                values[slot] = incomingValue;
                incomingKey = displacedKey;
                incomingValue = displacedValue;
                distance = occupiedDistance;
            }
            distance++;
            slot = (slot + 1) % capacity;
        }
    }

    /** 取值（探测至空槽即缺席）。 */
    @SuppressWarnings("unchecked")
    public Optional<V> get(K key) {
        requireKey(key);
        int slot = homeOf(key);
        int distance = 0;
        while (keys[slot] != null) {
            if (distance > capacity) {
                break;
            }
            if (keys[slot].equals(key)) {
                return Optional.of((V) values[slot]);
            }
            distance++;
            slot = (slot + 1) % capacity;
        }
        return Optional.empty();
    }

    /** 删除（后向搬移回填空洞；返回是否存在）。 */
    public boolean remove(K key) {
        requireKey(key);
        int hole = findSlot(key);
        if (hole < 0) {
            return false;
        }
        keys[hole] = null;
        values[hole] = null;
        size--;
        shiftBack(hole);
        return true;
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    /** 倍容次数读数（确定性增长史）。 */
    public long resizeCount() {
        return resizeCount;
    }

    /** 全表最大探测距离读数（劫富济贫的成效可见）。 */
    public int maxProbeDistance() {
        int max = 0;
        for (int slot = 0; slot < capacity; slot++) {
            if (keys[slot] != null) {
                max = Math.max(max, distance(homeOf((K) keys[slot]), slot));
            }
        }
        return max;
    }

    private int findSlot(K key) {
        int slot = homeOf(key);
        int distance = 0;
        while (keys[slot] != null && distance <= capacity) {
            if (keys[slot].equals(key)) {
                return slot;
            }
            distance++;
            slot = (slot + 1) % capacity;
        }
        return -1;
    }

    private void shiftBack(int hole) {
        int probe = hole;
        int steps = 0;
        while (steps < capacity) {
            probe = (probe + 1) % capacity;
            if (keys[probe] == null) {
                return;
            }
            int home = homeOf((K) keys[probe]);
            if (distance(home, hole) < distance(home, probe)) {
                keys[hole] = keys[probe];
                values[hole] = values[probe];
                keys[probe] = null;
                values[probe] = null;
                hole = probe;
            }
            steps++;
        }
    }

    private void resize() {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        capacity *= 2;
        keys = new Object[capacity];
        values = new Object[capacity];
        size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                put((K) oldKeys[i], (V) oldValues[i]);
            }
        }
        resizeCount++;
    }

    private int homeOf(K key) {
        return (key.hashCode() & 0x7fffffff) % capacity;
    }

    private int distance(int home, int slot) {
        return (slot - home + capacity) % capacity;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
    }
}
