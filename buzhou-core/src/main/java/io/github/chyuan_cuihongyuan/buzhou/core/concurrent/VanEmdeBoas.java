package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;

/**
 * VanEmdeBoas 有界宇宙树（spec 6046 / T6291 / impl 2246）——
 * van Emde Boas 1975 思想：**宇宙按半位分簇递归 + summary
 * 摘要**——成员/后继/最值 O(log log u)，远低于有序结构
 * O(log n)——有界宇宙（会话 id、槽位号等小整数全域）高频
 * successor 场景的病解。簇与 summary 惰性创建（稀疏集不预支
 * 全域内存）；min 只在本节点镜像、max 每层冗余（CLRS 约定）；
 * 集合语义（重复插入幂等）、缺席删除 fail-fast。
 *
 * <p>与 IndexedHeap（spec 6026）同族不同面：优先级队列
 * decrease-key vs 有界宇宙后继查询；与 InterpolationSearch
 * 不同面：静态有序数组探测 vs 动态集合递归分簇。
 */
public final class VanEmdeBoas {

    /** 递归节点（包私有可测试；快照面只出包装器）。 */
    static final class Node {
        final int bits;
        final int halfBits;
        final int clusterMask;
        Node summary;
        final Map<Integer, Node> clusters = new HashMap<>();
        int min = -1;
        int max = -1;

        Node(int bits) {
            if (bits < 1 || bits > 20) {
                throw new IllegalArgumentException("宇宙位宽须在 [1,20]: " + bits);
            }
            this.bits = bits;
            this.halfBits = bits >> 1;
            this.clusterMask = (1 << halfBits) - 1;
        }

        int high(int x) {
            return x >>> halfBits;
        }

        int low(int x) {
            return x & clusterMask;
        }

        int index(int h, int l) {
            return (h << halfBits) | l;
        }

        Node summary() {
            if (summary == null) {
                summary = new Node(bits - halfBits);
            }
            return summary;
        }

        void insertInternal(int x) {
            if (min == -1) {
                min = max = x;
                return;
            }
            if (x < min) {
                int t = min;
                min = x;
                x = t;
            }
            if (x > max) {
                max = x;
            }
            if (bits == 1) {
                return;
            }
            int h = high(x);
            Node c = clusters.get(h);
            if (c == null || c.min == -1) {
                if (c == null) {
                    c = new Node(halfBits);
                    clusters.put(h, c);
                }
                c.min = c.max = low(x);
                summary().insertInternal(h);
            } else {
                c.insertInternal(low(x));
            }
        }

        boolean containsInternal(int x) {
            if (x == min || x == max) {
                return true;
            }
            if (min == -1 || bits == 1) {
                return false;
            }
            Node c = clusters.get(high(x));
            return c != null && c.containsInternal(low(x));
        }

        int successorInternal(int x) {
            if (min == -1 || x >= max) {
                return -1;
            }
            if (x < min) {
                return min;
            }
            if (bits == 1) {
                return max;
            }
            int h = high(x);
            Node c = clusters.get(h);
            if (c != null && c.min != -1 && low(x) < c.max) {
                return index(h, c.successorInternal(low(x)));
            }
            int s = summary == null ? -1 : summary.successorInternal(h);
            if (s == -1) {
                return max;
            }
            return index(s, clusters.get(s).min);
        }

        void deleteInternal(int x) {
            if (min == max) {
                min = max = -1;
                return;
            }
            if (bits == 1) {
                if (x == min) {
                    min = max;
                } else {
                    max = min;
                }
                return;
            }
            if (x == min) {
                int s = summary.minimumInternal();
                x = index(s, clusters.get(s).min);
                min = x;
            }
            int h = high(x);
            Node c = clusters.get(h);
            c.deleteInternal(low(x));
            if (c.min == -1) {
                clusters.remove(h);
                summary.deleteInternal(h);
            }
            if (x == max) {
                if (summary == null || summary.min == -1) {
                    max = min;
                } else {
                    int sm = summary.maximumInternal();
                    max = index(sm, clusters.get(sm).max);
                }
            }
        }

        int minimumInternal() {
            return min;
        }

        int maximumInternal() {
            return max;
        }
    }

    private final Node root;
    private final int universeSize;
    private int size;

    /** bits∈[1,20]——宇宙 2^bits；越域 fail-fast。 */
    public VanEmdeBoas(int bits) {
        this.root = new Node(bits);
        this.universeSize = 1 << bits;
    }

    /** 插入（集合语义——重复幂等；越域 fail-fast）。 */
    public void insert(int x) {
        checkBounds(x);
        if (contains(x)) {
            return;
        }
        root.insertInternal(x);
        size++;
    }

    /** 删除（缺席 fail-fast）。 */
    public void delete(int x) {
        checkBounds(x);
        if (!contains(x)) {
            throw new IllegalArgumentException("元素不存在: " + x);
        }
        root.deleteInternal(x);
        size--;
    }

    /** 成员判定。 */
    public boolean contains(int x) {
        checkBounds(x);
        return root.containsInternal(x);
    }

    /** 后继（>x 的最小成员；无则 -1）。 */
    public int successor(int x) {
        checkBounds(x);
        return root.successorInternal(x);
    }

    /** 最小成员（空集 -1——诚实）。 */
    public int minimum() {
        return root.min;
    }

    /** 最大成员（空集 -1）。 */
    public int maximum() {
        return root.max;
    }

    /** 元素数读数。 */
    public int size() {
        return size;
    }

    /** 空集判定。 */
    public boolean isEmpty() {
        return size == 0;
    }

    /** 宇宙大小 2^bits。 */
    public int universeSize() {
        return universeSize;
    }

    private void checkBounds(int x) {
        if (x < 0 || x >= universeSize) {
            throw new IllegalArgumentException("越宇宙域 [0," + universeSize + "): " + x);
        }
    }
}
