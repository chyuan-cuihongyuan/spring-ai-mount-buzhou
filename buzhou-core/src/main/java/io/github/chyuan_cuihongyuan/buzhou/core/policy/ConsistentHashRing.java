package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 一致性哈希环（spec 2025 / T3151 / impl 1576）——Dynamo/Ketama 思想：
 * 虚节点环 + 顺时针归属。节点增减只影响相邻弧段——键迁移量 ≈ 1/n
 *（朴素取模在节点变化时全量重映射）；虚节点打散物理节点在环上的
 * 分布（防单节点独占大弧段）。
 *
 * <p>确定性散列（FNV-1a 64 + splitmix64 终结，与 HLL/频率素描同款）；
 * synchronized 小临界区。
 */
public final class ConsistentHashRing {

    /** 默认每物理节点虚节点数（Ketama 惯例量级）。 */
    public static final int DEFAULT_VIRTUAL_NODES = 160;

    private final int virtualNodes;
    private final NavigableMap<Long, String> ring = new TreeMap<>();
    private final Map<String, Integer> nodeWeights = new HashMap<>();

    /** 契约：virtualNodes ≥ 1（fail-fast）。 */
    public ConsistentHashRing(int virtualNodes) {
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("virtualNodes 须 ≥ 1：" + virtualNodes);
        }
        this.virtualNodes = virtualNodes;
    }

    public ConsistentHashRing() {
        this(DEFAULT_VIRTUAL_NODES);
    }

    /** 加节点：铺 virtualNodes 个虚节点（node#i 散列分散弧段）。契约：node 非空非重复。 */
    public synchronized void addNode(String node) {
        if (node == null || node.isBlank()) {
            throw new IllegalArgumentException("node 不能为空");
        }
        if (nodeWeights.containsKey(node)) {
            throw new IllegalArgumentException("节点已存在：" + node);
        }
        nodeWeights.put(node, virtualNodes);
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash64(node + "#" + i), node);
        }
    }

    /** 删节点：撤其全部虚节点（只影响其弧段上的键——最小迁移）。 */
    public synchronized void removeNode(String node) {
        if (node == null || !nodeWeights.containsKey(node)) {
            throw new IllegalArgumentException("节点不存在：" + node);
        }
        nodeWeights.remove(node);
        for (int i = 0; i < virtualNodes; i++) {
            ring.remove(hash64(node + "#" + i));
        }
    }

    /** 键归属：环上 ≥ hash(key) 的首个虚节点之物理节点；空环 null。 */
    public synchronized String nodeFor(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        if (ring.isEmpty()) {
            return null;
        }
        long h = hash64(key);
        Map.Entry<Long, String> ceiling = ring.ceilingEntry(h);
        return ceiling != null ? ceiling.getValue() : ring.firstEntry().getValue(); // 回绕
    }

    /** 物理节点数。 */
    public synchronized int nodeCount() {
        return nodeWeights.size();
    }

    /** 虚节点总数（环容量对账面）。 */
    public synchronized int virtualNodeCount() {
        return ring.size();
    }

    /** FNV-1a 64 + splitmix64 终结（与 HLL/频率素描同款确定性散列）。 */
    private static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
