package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量攒批器（spec 3016 / T5033 / impl 2017）——Kafka producer
 * 攒批思想（batch.size + linger.ms 双阈值）：条目攒批，**条数满
 * 或批龄到**任一达标即冲——吞吐（攒满大批摊薄单条开销）与延迟
 * （龄到即冲不等满）的显式权衡旋钮。时间由调用方传入（确定性
 * 免注入）；「事件外发 webhook / 指标上报 / 批量落盘」的低摩擦
 * 前置件。
 *
 * <p>守恒对账：totalOffered == totalFlushed + 当前批 size（溢出
 * 零丢失口径）；非线程安全（单攒批线程，冲批归调用方调度）。
 */
public final class BatchAccumulator<T> {

    private final int maxBatchSize;
    private final long lingerMillis;
    private final List<T> buffer = new ArrayList<>();
    private long oldestAtMillis = -1;
    private long totalOffered;
    private long totalFlushed;
    private long flushCount;

    /** 双阈值（均 ≥1：maxBatchSize 条 / lingerMillis 毫秒龄）。 */
    public BatchAccumulator(int maxBatchSize, long lingerMillis) {
        if (maxBatchSize < 1 || lingerMillis < 1) {
            throw new IllegalArgumentException("maxBatchSize/lingerMillis ≥ 1：" + maxBatchSize + "/" + lingerMillis);
        }
        this.maxBatchSize = maxBatchSize;
        this.lingerMillis = lingerMillis;
    }

    /** 入批（记录首入时间锚）；返回 true=条数已满应立即冲。 */
    public boolean offer(T item, long nowMillis) {
        if (oldestAtMillis < 0) {
            oldestAtMillis = nowMillis;
        }
        buffer.add(item);
        totalOffered++;
        return buffer.size() >= maxBatchSize;
    }

    /** 冲批判定：条数满 **或**（非空且批龄 ≥ linger）任一成立。 */
    public boolean flushReady(long nowMillis) {
        if (buffer.size() >= maxBatchSize) {
            return true;
        }
        return !buffer.isEmpty() && nowMillis - oldestAtMillis >= lingerMillis;
    }

    /** 取走整批（空批返空表不动账）；账面：flushCount/totalFlushed。 */
    public List<T> drain() {
        if (buffer.isEmpty()) {
            return List.of();
        }
        List<T> batch = List.copyOf(buffer);
        totalFlushed += batch.size();
        flushCount++;
        buffer.clear();
        oldestAtMillis = -1;
        return batch;
    }

    /** 当前批条数。 */
    public int size() {
        return buffer.size();
    }

    /** 当前批是否为空。 */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /** 当前批龄（空批 0——诚实零）。 */
    public long oldestAge(long nowMillis) {
        return buffer.isEmpty() ? 0 : nowMillis - oldestAtMillis;
    }

    /** 累计 offer 数（含在批未冲——守恒对账面）。 */
    public long totalOffered() {
        return totalOffered;
    }

    /** 累计冲出条数。 */
    public long totalFlushed() {
        return totalFlushed;
    }

    /** 冲批次数。 */
    public long flushCount() {
        return flushCount;
    }
}
