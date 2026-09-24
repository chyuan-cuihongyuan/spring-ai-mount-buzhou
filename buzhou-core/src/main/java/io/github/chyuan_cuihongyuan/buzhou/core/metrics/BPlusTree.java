package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * B+ Tree 有序索引（spec 5032 / T6165 / impl 2183）——
 * MySQL InnoDB/数据库存储引擎 B+ 树思想：全部键值落叶层、
 * 内节点只放分隔键（每节点扇出=m——树高即磁盘/比较次数
 * 上界），叶层 next 链顺序扫描；满节点确定性对半分裂
 * （叶分裂分隔键=左半末键**复制**上提，内节点分裂中位键
 * **移动**上提），put upsert（同键覆盖不增位）。确定性
 * 无时间依赖。
 *
 * <p>与 SkipList（spec 5025）同族不同面：概率多层链 vs
 * 确定性多路平衡树（树高紧凑、扇出可控）；不做删除
 * （借用/合并面不在本件——见 Out of Scope）。
 */
public final class BPlusTree {

    /** 节点最小分隔键容量（m≥2 才有分裂意义）。 */
    private static final int MIN_MAX_KEYS = 2;

    /** 一对键值（叶层有序全集的不可变视图）。 */
    public record Entry(int key, int value) {
    }

    /** 分裂上提：分隔键 + 新右节点。 */
    private record Split(int separatorKey, Node right) {
    }

    private abstract static class Node {
        final List<Integer> keys = new ArrayList<>();
    }

    private static final class Leaf extends Node {
        final List<Integer> values = new ArrayList<>();
        Leaf next;
    }

    private static final class Internal extends Node {
        final List<Node> children = new ArrayList<>();
    }

    private Node root = new Leaf();
    private int height = 1;
    private int size;
    private final int maxKeys;

    /** 定构（每节点分隔键上限 m≥2 fail-fast）。 */
    public BPlusTree(int maxKeys) {
        if (maxKeys < MIN_MAX_KEYS) {
            throw new IllegalArgumentException("maxKeys≥2：" + maxKeys);
        }
        this.maxKeys = maxKeys;
    }

    /** 插入/覆盖（upsert：同键覆盖不增位；满节点确定性分裂）。 */
    public void put(int key, int value) {
        Split split = putRecursive(root, key, value);
        if (split != null) {
            Internal newRoot = new Internal();
            newRoot.keys.add(split.separatorKey());
            newRoot.children.add(root);
            newRoot.children.add(split.right());
            root = newRoot;
            height++;
        }
    }

    /** 键存在性。 */
    public boolean containsKey(int key) {
        return findLeaf(key).keys.indexOf(key) >= 0;
    }

    /** 键取值（缺席回退指定值——无 null 面）。 */
    public int getOrDefault(int key, int fallback) {
        Leaf leaf = findLeaf(key);
        int pos = leaf.keys.indexOf(key);
        return pos >= 0 ? leaf.values.get(pos) : fallback;
    }

    /** 叶层全序键值（next 链顺序扫描——范围扫底座）。 */
    public List<Entry> entriesInOrder() {
        List<Entry> entries = new ArrayList<>();
        Leaf leaf = leftmostLeaf();
        while (leaf != null) {
            for (int i = 0; i < leaf.keys.size(); i++) {
                entries.add(new Entry(leaf.keys.get(i), leaf.values.get(i)));
            }
            leaf = leaf.next;
        }
        return List.copyOf(entries);
    }

    /** 键数读数。 */
    public int size() {
        return size;
    }

    /** 树高读数（根单叶=1——比较/寻址次数上界）。 */
    public int height() {
        return height;
    }

    private Split putRecursive(Node node, int key, int value) {
        if (node instanceof Leaf leaf) {
            int pos = leaf.keys.indexOf(key);
            if (pos >= 0) {
                leaf.values.set(pos, value);
                return null;
            }
            int insertAt = childIndexOf(leaf.keys, key);
            leaf.keys.add(insertAt, key);
            leaf.values.add(insertAt, value);
            size++;
            return leaf.keys.size() > maxKeys ? splitLeaf(leaf) : null;
        }
        Internal internal = (Internal) node;
        Split childSplit = putRecursive(internal.children.get(childIndexOf(internal.keys, key)),
                key, value);
        if (childSplit == null) {
            return null;
        }
        int insertAt = childIndexOf(internal.keys, childSplit.separatorKey());
        internal.keys.add(insertAt, childSplit.separatorKey());
        internal.children.add(insertAt + 1, childSplit.right());
        return internal.keys.size() > maxKeys ? splitInternal(internal) : null;
    }

    /** 叶分裂：对半（左取上取整），分隔键=左半末键复制上提。 */
    private Split splitLeaf(Leaf leaf) {
        int leftCount = (leaf.keys.size() + 1) / 2;
        Leaf right = new Leaf();
        right.keys.addAll(leaf.keys.subList(leftCount, leaf.keys.size()));
        right.values.addAll(leaf.values.subList(leftCount, leaf.values.size()));
        right.next = leaf.next;
        leaf.next = right;
        int separatorKey = leaf.keys.get(leftCount - 1);
        leaf.keys.subList(leftCount, leaf.keys.size()).clear();
        leaf.values.subList(leftCount, leaf.values.size()).clear();
        return new Split(separatorKey, right);
    }

    /** 内节点分裂：中位分隔键移动上提（不保留在任一半）。 */
    private Split splitInternal(Internal internal) {
        int midIndex = internal.keys.size() / 2;
        int separatorKey = internal.keys.get(midIndex);
        Internal right = new Internal();
        right.keys.addAll(internal.keys.subList(midIndex + 1, internal.keys.size()));
        right.children.addAll(internal.children.subList(midIndex + 1, internal.children.size()));
        internal.keys.subList(midIndex, internal.keys.size()).clear();
        internal.children.subList(midIndex + 1, internal.children.size()).clear();
        return new Split(separatorKey, right);
    }

    private Leaf findLeaf(int key) {
        Node node = root;
        while (node instanceof Internal internal) {
            node = internal.children.get(childIndexOf(internal.keys, key));
        }
        return (Leaf) node;
    }

    private Leaf leftmostLeaf() {
        Node node = root;
        while (node instanceof Internal internal) {
            node = internal.children.getFirst();
        }
        return (Leaf) node;
    }

    /**
     * 首个 keys[i] ≥ key 的下标（严格小于计数）——分隔键=左半
     * 末键复制，等键走左子树（左子树持有 ≤ 分隔键的全部键）。
     */
    private static int childIndexOf(List<Integer> keys, int key) {
        int low = 0;
        int high = keys.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (keys.get(mid) < key) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
