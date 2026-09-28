package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Indexed Heap 索引堆（spec 6026 / T6253 / impl 2227）——
 * Dijkstra/Prim 皆依赖的**位置映射 + decrease-key** 二叉
 * 最小堆思想：id→堆位哈希映射 O(1) 定位，优先级更新后
 * 上浮/下沉各走对数——PairingHeap 无 decrease-key（其
 * spec 明示裁剪面）、朴素堆更新需全扫 O(n) 的病解。同一
 * id 唯一在堆（重复 push fail-fast）；堆序确定性（优先级
 * 同按 id 字典序——同操作序列同出序）。
 *
 * <p>与 PairingHeap（spec 5048）同族不同面：可合并堆（无
 * decrease-key） vs 索引堆（decrease-key 一等公民）；与
 * AgingPriorityQueue（exec）不同面：老化防饿 vs 图算法
 * 原语。
 */
public final class IndexedHeap {

    private long[] ids = new long[16];
    private long[] priorities = new long[16];
    private int size;
    private final Map<Long, Integer> positionOf = new HashMap<>();

    /** 压入（重复 id fail-fast）。 */
    public void push(long id, long priority) {
        if (contains(id)) {
            throw new IllegalArgumentException("id 已在堆: " + id);
        }
        ensureCapacity(size + 1);
        int pos = size++;
        ids[pos] = id;
        priorities[pos] = priority;
        positionOf.put(id, pos);
        siftUp(pos);
    }

    /** 更新优先级（缺席 fail-fast；双向——升/降皆可）。 */
    public void updatePriority(long id, long newPriority) {
        Integer pos = positionOf.get(id);
        if (pos == null) {
            throw new IllegalArgumentException("id 不在堆: " + id);
        }
        long old = priorities[pos];
        priorities[pos] = newPriority;
        if (newPriority < old) {
            siftUp(pos);
        } else if (newPriority > old) {
            siftDown(pos);
        }
    }

    /** 弹出最小优先级的 id（空堆 fail-fast）。 */
    public long popMin() {
        requireNonEmpty();
        long minId = ids[0];
        removeAt(0);
        return minId;
    }

    /** 窥视最小优先级 id（空堆 fail-fast）。 */
    public long peekMin() {
        requireNonEmpty();
        return ids[0];
    }

    /** 窥视最小优先级（空堆 fail-fast）。 */
    public long peekMinPriority() {
        requireNonEmpty();
        return priorities[0];
    }

    /** 是否包含 id。 */
    public boolean contains(long id) {
        return positionOf.containsKey(id);
    }

    /** 某键当前优先级（缺席 fail-fast）。 */
    public long priorityOf(long id) {
        Integer pos = positionOf.get(id);
        if (pos == null) {
            throw new IllegalArgumentException("id 不在堆: " + id);
        }
        return priorities[pos];
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 是否空。 */
    public boolean isEmpty() {
        return size == 0;
    }

    private void removeAt(int pos) {
        positionOf.remove(ids[pos]);
        int last = --size;
        if (pos != last) {
            ids[pos] = ids[last];
            priorities[pos] = priorities[last];
            positionOf.put(ids[pos], pos);
            siftDown(pos);
            siftUp(pos);
        }
    }

    private void siftUp(int start) {
        int pos = start;
        while (pos > 0) {
            int parent = (pos - 1) >>> 1;
            if (less(pos, parent)) {
                swap(pos, parent);
                pos = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int start) {
        int pos = start;
        while (true) {
            int left = pos * 2 + 1;
            if (left >= size) {
                break;
            }
            int child = left;
            int right = left + 1;
            if (right < size && less(right, left)) {
                child = right;
            }
            if (less(child, pos)) {
                swap(child, pos);
                pos = child;
            } else {
                break;
            }
        }
    }

    /** 堆序：优先级小者先；同优先级按 id 小者先（确定性）。 */
    private boolean less(int a, int b) {
        if (priorities[a] != priorities[b]) {
            return priorities[a] < priorities[b];
        }
        return ids[a] < ids[b];
    }

    private void swap(int a, int b) {
        long tmpId = ids[a];
        ids[a] = ids[b];
        ids[b] = tmpId;
        long tmpP = priorities[a];
        priorities[a] = priorities[b];
        priorities[b] = tmpP;
        positionOf.put(ids[a], a);
        positionOf.put(ids[b], b);
    }

    private void ensureCapacity(int required) {
        if (required <= ids.length) {
            return;
        }
        ids = Arrays.copyOf(ids, ids.length * 2);
        priorities = Arrays.copyOf(priorities, priorities.length * 2);
    }

    private void requireNonEmpty() {
        if (size == 0) {
            throw new IllegalArgumentException("空堆");
        }
    }
}
