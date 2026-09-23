package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Space-Saving 频繁项 top-k（spec 4002 / T6005 / impl 2103）——固定
 * k 槽精确计数频繁项思想（Metwally 2005；ClickHouse sumKMorQ 系）：
 * 只保 k 个键的**精确计数器**；新键到达且槽满时淘汰最小计数键、
 * 新键继承其计数 +1（最小计数即误差界）——保留者计数恒 ≥ 真值
 * （单侧高估），真 top-k 以计数超 n/k 为保底全部召回。
 *
 * <p>MisraGries 的对偶升级：MG 用减法找候选（计数 ≤ 真值，最后要
 * 二次精扫），SS 用继承保留精确计数（读数即答案）——「要名单还要
 * 名次」的场景（热门工具榜/高频错误榜）用本件；CountMinSketch 则
 * 换任意键点查。三件素描族按问句选型。
 */
public final class SpaceSavingTopK {

    /** 榜单条目（键 + 保留计数——保留者恒 ≥ 真值）。 */
    public record Entry(String key, long count) {
    }

    private final int capacity;
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private long totalObservations;

    /** 定构（k 槽 ≥1 否则 fail-fast）。 */
    public SpaceSavingTopK(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity 必须 ≥1（实际 " + capacity + "）");
        }
        this.capacity = capacity;
    }

    /** 观测一次：在榜精确 +1；满榜新键淘汰最小者并继承其计数 +1。 */
    public void observe(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        totalObservations++;
        Long current = counters.get(key);
        if (current != null) {
            counters.put(key, current + 1);
            return;
        }
        if (counters.size() < capacity) {
            counters.put(key, 1L);
            return;
        }
        Map.Entry<String, Long> min = minEntry();
        counters.remove(min.getKey());
        counters.put(key, min.getValue() + 1);   // 继承最小计数 + 本次观测
    }

    /** 键计数估计：在榜读保留计数（恒 ≥ 真值）；未入榜为 0。 */
    public long estimate(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        return counters.getOrDefault(key, 0L);
    }

    /** 榜单：计数降序、并列先入榜者先（确定性可回放）。 */
    public List<Entry> top() {
        List<Entry> list = new ArrayList<>();
        counters.forEach((k, v) -> list.add(new Entry(k, v)));
        list.sort((a, b) -> {
            int byCount = Long.compare(b.count(), a.count());
            return byCount != 0 ? byCount : Integer.compare(
                    indexOfKey(a.key()), indexOfKey(b.key()));
        });
        return list;
    }

    /** 当前最小计数（在榜误差界——继承链的累计代价）。 */
    public long minCount() {
        return minEntry().getValue();
    }

    /** 总观测数守恒账。 */
    public long totalObservations() {
        return totalObservations;
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    private Map.Entry<String, Long> minEntry() {
        Map.Entry<String, Long> min = null;
        for (Map.Entry<String, Long> e : counters.entrySet()) {
            if (min == null || e.getValue() < min.getValue()) {
                min = e;
            }
        }
        return min;
    }

    private int indexOfKey(String key) {
        int i = 0;
        for (String k : counters.keySet()) {
            if (k.equals(key)) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
