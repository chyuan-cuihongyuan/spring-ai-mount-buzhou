package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * OR-Set 观察删除集（spec 3027 / T5055 / impl 2028）——CRDT
 * Observed-Remove Set 思想（Shapiro et al. 2011）：每次 add 带
 **唯一标签**，remove 只墓碑**当时观察到的标签**——并发场景
 * **add 胜**（remove 看不到并发新标签，元素幸存）；merge 双并集
 * （标签并 + 墓碑并）幂等交换——多副本收敛免协调。会话标签/
 * 协作标注/多端收藏的免协调集语义（与 LWW 寄存器「后写胜」、
 * PN-Counter「增减独立」成 CRDT 三形态）。
 *
 * <p>非线程安全（单副本线程口径，副本间靠 merge 同步）；merge
 * 只读对侧不吞并（对侧状态不动）。
 */
public final class ObservedRemoveSet<T> {

    /** 唯一标签（副本号+本地序——各副本天然不冲突）。 */
    public record Tag(String replica, long seq) {
    }

    private final String replicaId;
    private final Map<T, Set<Tag>> entries = new LinkedHashMap<>();
    private final Set<Tag> tombstones = new HashSet<>();
    private long counter;

    /** 副本号（标签前缀——非空）。 */
    public ObservedRemoveSet(String replicaId) {
        if (replicaId == null || replicaId.isEmpty()) {
            throw new IllegalArgumentException("replicaId 非空");
        }
        this.replicaId = replicaId;
    }

    /** 添加元素（新唯一标签——并发 remove 观察不到即幸存）。 */
    public void add(T element) {
        Tag tag = new Tag(replicaId, ++counter);
        entries.computeIfAbsent(element, e -> new LinkedHashSet<>()).add(tag);
    }

    /** 删除（只墓碑当前可见标签——未观察的并发新标签不受影响）。 */
    public void remove(T element) {
        Set<Tag> tags = entries.get(element);
        if (tags != null) {
            tombstones.addAll(tags);
        }
    }

    /** 可见判定（存在未墓碑标签即含）。 */
    public boolean contains(T element) {
        Set<Tag> tags = entries.get(element);
        return tags != null && tags.stream().anyMatch(t -> !tombstones.contains(t));
    }

    /** 当前可见元素集（快照）。 */
    public Set<T> elements() {
        Set<T> visible = new LinkedHashSet<>();
        for (T element : entries.keySet()) {
            if (contains(element)) {
                visible.add(element);
            }
        }
        return visible;
    }

    /** 可见元素数。 */
    public int size() {
        return elements().size();
    }

    /**
     * 双并集合并（标签并+墓碑并）——幂等、交换、只读对侧。
     */
    public void merge(ObservedRemoveSet<T> other) {
        for (Map.Entry<T, Set<Tag>> entry : other.entries.entrySet()) {
            entries.computeIfAbsent(entry.getKey(), e -> new LinkedHashSet<>()).addAll(entry.getValue());
        }
        tombstones.addAll(other.tombstones);
    }

    /** 副本号读回。 */
    public String replicaId() {
        return replicaId;
    }
}
