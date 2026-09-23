package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 有界负载一致哈希（spec 5003 / T6107 / impl 2154）——
 * Consistent Hashing with Bounded Loads 思想（Google vultr）：
 * 稳定哈希定位起点（`hash(key) mod n`），满载则**线性探查**
 * 下一节点（确定性回退序），单节点负载 ≤ 其 capacity——热点
 * 被结构性封顶（哈希扎堆不雪崩）；总容量耗尽 ISE 诚实拒配。
 * 纯一致哈希无上限（扎堆过载）与全量重排（key 洗牌）的病解。
 *
 * <p>线性探查变体的确定性落地口径（诚实入档：非论文的逐 key
 * 随机排列版）；节点集不变时同 key 同落点。与
 * RendezvousHashing（HRW 稳定选择）互补：纯稳定 vs 有界回退。
 */
public final class BoundedLoadRing {

    private final Map<String, Integer> capacities = new LinkedHashMap<>();
    private final Map<String, Integer> loads = new LinkedHashMap<>();

    /** 注册节点（容量 ≤0 / 重复 id fail-fast）。 */
    public void addNode(String id, int capacity) {
        if (id == null || id.isEmpty() || capacity <= 0) {
            throw new IllegalArgumentException("id 非空且容量 >0：" + id + "/" + capacity);
        }
        if (capacities.containsKey(id)) {
            throw new IllegalArgumentException("重复节点：" + id);
        }
        capacities.put(id, capacity);
        loads.put(id, 0);
    }

    /** 摘除节点（未注册 fail-fast）。 */
    public void removeNode(String id) {
        if (capacities.remove(id) == null) {
            throw new IllegalArgumentException("未注册节点：" + id);
        }
        loads.remove(id);
    }

    /**
     * 分配 key（稳定哈希起点 + 满载线性探查；总容量耗尽 ISE）。
     *
     * @param key 任意非 null 对象（hashCode 即稳定哈希）
     * @return 承载节点 id
     */
    public String assign(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非 null");
        }
        List<String> nodes = new ArrayList<>(capacities.keySet());
        if (nodes.isEmpty()) {
            throw new IllegalStateException("空环（无节点）");
        }
        int totalCapacity = capacities.values().stream().mapToInt(Integer::intValue).sum();
        int totalLoad = loads.values().stream().mapToInt(Integer::intValue).sum();
        if (totalLoad >= totalCapacity) {
            throw new IllegalStateException("总容量耗尽（load " + totalLoad + " ≥ capacity "
                    + totalCapacity + "）——诚实拒配");
        }
        int start = Math.floorMod(key.hashCode(), nodes.size());
        for (int probe = 0; probe < nodes.size(); probe++) {
            String candidate = nodes.get((start + probe) % nodes.size());
            if (loads.get(candidate) < capacities.get(candidate)) {
                loads.merge(candidate, 1, Integer::sum);
                return candidate;
            }
        }
        throw new IllegalStateException("不可达：容量守恒被破坏");
    }

    /** 释放一槽（未注册/零负载 fail-fast）。 */
    public void release(String id) {
        Integer load = loads.get(id);
        if (load == null) {
            throw new IllegalArgumentException("未注册节点：" + id);
        }
        if (load == 0) {
            throw new IllegalArgumentException("节点零负载无可释放：" + id);
        }
        loads.put(id, load - 1);
    }

    /** 节点负载读数（未注册 fail-fast）。 */
    public int loadOf(String id) {
        Integer load = loads.get(id);
        if (load == null) {
            throw new IllegalArgumentException("未注册节点：" + id);
        }
        return load;
    }

    /** 节点数读数。 */
    public int size() {
        return capacities.size();
    }
}
