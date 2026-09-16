package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 键压缩日志语义（spec 2014 / T3129 / impl 1565）——Kafka log
 * compaction 思想：按 key 压缩保留**最新**记录（旧值让位），tombstone
 * （null payload）即删除 key 的墓碑记录；压缩幂等（乱序输入同结果——
 * 保留者只由 (key, seq) 最大决定，与输入顺序无关）。状态类日志读取
 * （工具调用台账 / 会话特征）不用全量扫：一遍压缩即终态。
 *
 * <p>纯函数零状态、确定性。
 */
public final class KeyCompaction {

    /** 压缩条目：key + 序号 + 载荷（payload 为 null 即 tombstone 墓碑）。 */
    public record Entry(String key, long seq, String payload) {
    }

    /** 压缩结果：终态键值（墓碑已删）+ 对账计数。 */
    public record CompactionResult(Map<String, String> latest, int tombstones, int superseded) {

        /** 压缩率 = 被压缩掉（旧值+墓碑）÷ 总输入。 */
        public double compactionRatio(int totalInput) {
            return totalInput == 0 ? 0.0d : (double) superseded / totalInput;
        }
    }

    private KeyCompaction() {
    }

    /**
     * 压缩：同 key 取 seq 最大者；最大者为 tombstone → key 删除；
     * seq 相同取后见者（输入序）——严格递增序号下的退化口径。
     * 契约：key 非空、seq ≥ 0（fail-fast）。
     */
    public static CompactionResult compact(Collection<Entry> entries) {
        Map<String, Entry> winners = new HashMap<>();
        int tombstones = 0;
        int superseded = 0;
        for (Entry e : entries == null ? java.util.List.<Entry>of() : entries) {
            if (e.key() == null) {
                throw new IllegalArgumentException("key 不能为 null");
            }
            if (e.seq() < 0) {
                throw new IllegalArgumentException("seq 须 ≥ 0：" + e.seq());
            }
            Entry incumbent = winners.get(e.key());
            if (incumbent == null || e.seq() >= incumbent.seq()) {
                winners.put(e.key(), e);
                if (incumbent != null) {
                    superseded++; // 卫冕者让位
                }
            } else {
                superseded++; // 迟到旧值
            }
        }
        Map<String, String> latest = new HashMap<>();
        for (Map.Entry<String, Entry> e : winners.entrySet()) {
            if (e.getValue().payload() == null) {
                tombstones++;
            } else {
                latest.put(e.getKey(), e.getValue().payload());
            }
        }
        return new CompactionResult(Map.copyOf(latest), tombstones, superseded);
    }
}
