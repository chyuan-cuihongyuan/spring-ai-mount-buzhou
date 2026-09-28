package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * WaitForGraph 等待图死锁检测（spec 6045 / T6289 / impl 2245）——
 * DB2/SQL Server 锁管理器思想：**边加入时检测环**——节点为
 * 事务（或持锁者），有向边 waiter→holder 表示「等待」；环即
 * 死锁。增量加边即时回报死锁环与受难者（victim=环内最大 id，
 * 「最年轻者回滚代价最小」约定），打断靠 removeNode 摘除——
 * 事后全图扫描（死锁已扩大化才被发现）与全局超时轮询（无差别
 * 惩罚无辜等待者）的病解。邻接按 id 升序遍历——同图同环，
 * 检测完全确定。
 *
 * <p>与 TarjanSccFinder（同包）同族不同面：离线全图强连通
 * 分量 vs 增量加边即时环检测+受难者裁决。
 */
public final class WaitForGraph {

    /** waiter → 持有者集合（有序——环检测确定性）。 */
    private final Map<Long, TreeSet<Long>> waitsFor = new HashMap<>();
    private long edgeCount;

    /** 注册节点（孤立可入边；重复 fail-fast）。 */
    public void addNode(long id) {
        if (waitsFor.putIfAbsent(id, new TreeSet<>()) != null) {
            throw new IllegalArgumentException("节点已注册: " + id);
        }
    }

    /**
     * 加等待边 waiter→holder（两侧须已注册；自环 fail-fast）。
     * 返回加边后经过该边的死锁环（闭 walk，首尾同 id，如
     * [1,2,3,1]）；无环返回空表——边照常记录，打断由调用方
     * removeNode 完成。
     */
    public List<Long> addEdge(long waiter, long holder) {
        requireNode(waiter);
        requireNode(holder);
        if (waiter == holder) {
            throw new IllegalArgumentException("自环无意义: " + waiter);
        }
        if (waitsFor.get(waiter).add(holder)) {
            edgeCount++;
        }
        return findCycleThrough(waiter);
    }

    /** 摘除节点及其全部出入边（缺席 fail-fast）。 */
    public void removeNode(long id) {
        TreeSet<Long> outgoing = waitsFor.remove(id);
        if (outgoing == null) {
            throw new IllegalArgumentException("节点未注册: " + id);
        }
        edgeCount -= outgoing.size();
        for (TreeSet<Long> holders : waitsFor.values()) {
            if (holders.remove(id)) {
                edgeCount--;
            }
        }
    }

    /** 当前图是否存在死锁环。 */
    public boolean hasDeadlock() {
        return !findCycle().isEmpty();
    }

    /**
     * 规范环检测：起点与邻接均按 id 升序的 DFS，首个反边即
     * 规范环（同图同环）。无环返回空表。
     */
    public List<Long> findCycle() {
        for (long start : sortedNodeIds()) {
            List<Long> cycle = findCycleThrough(start);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.of();
    }

    /** 当前规范死锁环的受难者（环内最大 id——最年轻者先回滚；无环 -1）。 */
    public long deadlockVictim() {
        List<Long> cycle = findCycle();
        if (cycle.isEmpty()) {
            return -1L;
        }
        long victim = Long.MIN_VALUE;
        for (long id : cycle) {
            victim = Math.max(victim, id);
        }
        return victim;
    }

    /** 节点数读数。 */
    public int nodeCount() {
        return waitsFor.size();
    }

    /** 等待边数读数。 */
    public long edgeCount() {
        return edgeCount;
    }

    private List<Long> findCycleThrough(long start) {
        Map<Long, Integer> state = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        List<Long> path = new ArrayList<>();
        if (dfs(start, state, parent, path)) {
            long repeat = path.get(path.size() - 1);
            int from = path.indexOf(repeat);
            return canonicalRotation(path.subList(from, path.size()));
        }
        return List.of();
    }

    /** 规范化：环内最小 id 旋转到首位（同环同表述，与发现起点无关）。 */
    private List<Long> canonicalRotation(List<Long> cycle) {
        int minAt = 0;
        for (int i = 1; i < cycle.size() - 1; i++) {
            if (cycle.get(i) < cycle.get(minAt)) {
                minAt = i;
            }
        }
        List<Long> rotated = new ArrayList<>();
        for (int i = minAt; i < cycle.size() - 1; i++) {
            rotated.add(cycle.get(i));
        }
        for (int i = 0; i < minAt; i++) {
            rotated.add(cycle.get(i));
        }
        rotated.add(rotated.get(0));
        return List.copyOf(rotated);
    }

    /** 返回 path 尾部附加「重复出现节点」表示发现环。 */
    private boolean dfs(long node, Map<Long, Integer> state,
            Map<Long, Long> parent, List<Long> path) {
        state.put(node, 1);
        path.add(node);
        for (long next : waitsFor.get(node)) {
            Integer color = state.get(next);
            if (color == null) {
                parent.put(next, node);
                if (dfs(next, state, parent, path)) {
                    return true;
                }
            } else if (color == 1) {
                path.add(next);
                return true;
            }
        }
        state.put(node, 2);
        path.remove(path.size() - 1);
        return false;
    }

    private List<Long> sortedNodeIds() {
        List<Long> ids = new ArrayList<>(waitsFor.keySet());
        ids.sort(Long::compare);
        return ids;
    }

    private void requireNode(long id) {
        if (!waitsFor.containsKey(id)) {
            throw new IllegalArgumentException("节点未注册: " + id);
        }
    }
}
