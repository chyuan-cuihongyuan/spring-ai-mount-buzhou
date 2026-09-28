package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Floyd 判圈（spec 7044 / U7289 / impl 2296）——Floyd 1967
 * 龟兔赛跑思想：**快慢双指针在函数图上迭代，相遇即有环，
 * 头部二次迭代定环入口**——HashSet 记访存 O(n) 空间（大
 * 状态域不可承受）的病解（O(1) 空间）。函数图语义（每点
 * 恰一后继）；无环返回 −1（诚实缺省）；入口/环长双读数；
 * 完全确定。
 *
 * <p>与 WaitForGraph（同包）同族不同面：一般有向图增量环
 * 检测 vs 函数图 O(1) 空间判圈。
 */
public final class FloydCycleDetector {

    private final int[] next;
    private final Map<Integer, Integer> indexById = new HashMap<>();

    /** 函数图（next[i] = i 的后继；越域 fail-fast）。 */
    public FloydCycleDetector(int[] next) {
        if (next == null) {
            throw new IllegalArgumentException("后继表非空");
        }
        for (int successor : next) {
            if (successor < 0 || successor >= next.length) {
                throw new IllegalArgumentException("后继越域 [0," + next.length + "): " + successor);
            }
        }
        this.next = next.clone();
        for (int i = 0; i < next.length; i++) {
            indexById.putIfAbsent(i, i);
        }
    }

    /** 从 start 出发的环长（无环 -1）。 */
    public int cycleLength(int start) {
        checkNode(start);
        int tortoise = next(start);
        int hare = next(next(start));
        while (tortoise != hare) {
            tortoise = next(tortoise);
            hare = next(next(hare));
            if (hare == -1 || tortoise == -1) {
                return -1;
            }
        }
        int length = 1;
        int cursor = tortoise;
        while (next(cursor) != tortoise) {
            cursor = next(cursor);
            length++;
        }
        return length;
    }

    /** 环入口（无环 -1）。 */
    public int cycleEntry(int start) {
        int length = cycleLength(start);
        if (length == -1) {
            return -1;
        }
        int left = start;
        int right = start;
        for (int i = 0; i < length; i++) {
            right = next(right);
        }
        while (left != right) {
            left = next(left);
            right = next(right);
        }
        return left;
    }

    /** 是否有环。 */
    public boolean hasCycle(int start) {
        return cycleLength(start) != -1;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return next.length;
    }

    private int next(int node) {
        if (node < 0 || node >= next.length) {
            return -1;
        }
        return next[node];
    }

    private void checkNode(int start) {
        if (start < 0 || start >= next.length) {
            throw new IllegalArgumentException("起点越域 [0," + next.length + "): " + start);
        }
    }
}
