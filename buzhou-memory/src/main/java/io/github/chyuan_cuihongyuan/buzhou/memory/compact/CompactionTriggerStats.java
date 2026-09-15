package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 压缩触发原因分布（L 会话 1700 系 R29 = effort #1728 / spec 1728 /
 * 票 T2657 + T2658 / impl 1328）——RocksDB / Cassandra compaction stats
 * 思想：压缩（{@link MicroCompactor}）被什么触发（空闲闲时/比例越线/
 * 人工/检查点前）决定触发阈值调参方向——{@link CompactionRatioStats}
 * 管压缩率，本面管触发原因。
 *
 * <p>实例面线程安全：`Trigger` 闭集（IDLE 空闲闲时 / RATIO 比例越线 /
 * MANUAL 人工 / CHECKPOINT 检查点前）+record+census+idleShare+resetForTest。
 * 纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class CompactionTriggerStats {

    /** 触发原因闭集。 */
    public enum Trigger { IDLE, RATIO, MANUAL, CHECKPOINT }

    private final Map<Trigger, AtomicLong> counters = new EnumMap<>(Trigger.class);

    /** 默认构造。 */
    public CompactionTriggerStats() {
        for (Trigger trigger : Trigger.values()) {
            counters.put(trigger, new AtomicLong());
        }
    }

    /** 记一次压缩触发。 */
    public void record(Trigger trigger) {
        counters.get(trigger).incrementAndGet();
    }

    /**
     * @param total      触发总数
     * @param idle       空闲闲时触发数
     * @param ratio      比例越线触发数
     * @param manual     人工触发数
     * @param checkpoint 检查点前触发数
     * @param idleShare  闲时占比 idle/total；无样本哨兵 −1
     */
    public record TriggerCensus(long total, long idle, long ratio,
                                long manual, long checkpoint, double idleShare) {
    }

    /** 快照。 */
    public TriggerCensus census() {
        long idle = counters.get(Trigger.IDLE).get();
        long ratio = counters.get(Trigger.RATIO).get();
        long manual = counters.get(Trigger.MANUAL).get();
        long checkpoint = counters.get(Trigger.CHECKPOINT).get();
        long total = idle + ratio + manual + checkpoint;
        double share = total == 0 ? -1d : (double) idle / total;
        return new TriggerCensus(total, idle, ratio, manual, checkpoint, share);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}
