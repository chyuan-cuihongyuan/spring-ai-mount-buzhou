package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 范围读局部性分类读面（L 会话 1700 系 R43 = effort #1742 / spec 1742 /
 * 票 T2685 + T2686 / impl 1342）——RocksDB 块缓存局部性思想：范围读
 * （{@link RangeReadEngine}）偏移是顺序连续还是随机跳跃——顺序读可预取
 * 缓存友好，随机读是碎片化访问信号，局部性给出读模式画像。
 *
 * <p>实例面线程安全：`record(offset, length)` 逐次分类——与上一读
 * （offset == prevOffset+prevLength）连续 = 顺序；否则随机；首次读独立
 * 计。census 启顺序占比（总读 &lt;2 哨兵 −1）。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class RangeLocalityStats {

    private final Object lock = new Object();
    private long lastOffset = -1;
    private long lastEnd = -1;
    private long total;
    private long sequential;
    private long random;

    /** 记一次范围读（负值忽略）。 */
    public void record(long offset, long length) {
        if (offset < 0 || length < 0) {
            return;
        }
        synchronized (lock) {
            total++;
            if (lastOffset >= 0 && offset == lastEnd) {
                sequential++;
            } else if (lastOffset >= 0) {
                random++;
            }
            lastOffset = offset;
            lastEnd = offset + length;
        }
    }

    /**
     * @param reads          总读数
     * @param sequential     顺序连续读数（不含首读）
     * @param random         随机跳跃读数（不含首读）
     * @param sequentialShare 顺序占比 sequential/(reads−1)；可判对 &lt;2 哨兵 −1
     */
    public record LocalityCensus(long reads, long sequential, long random,
                                 double sequentialShare) {
    }

    /** 快照。 */
    public LocalityCensus census() {
        synchronized (lock) {
            long classifiable = total - 1;
            double share = classifiable <= 0 ? -1d : (double) sequential / classifiable;
            return new LocalityCensus(total, sequential, random, share);
        }
    }
}
