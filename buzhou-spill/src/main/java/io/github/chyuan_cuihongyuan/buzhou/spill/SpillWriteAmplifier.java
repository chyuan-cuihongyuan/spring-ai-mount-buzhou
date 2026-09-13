package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Spill 写放大读数（spec 815 / T1131，RocksDB compaction stats 写放大
 * {@code bytes_written / bytes_logical} 借鉴）：对 spill 落盘做逻辑字节 vs
 * 物理写入字节记账——一次 {@code store} 写 data+meta 双文件、{@code markLinked}
 * 再重写 meta：内容 1 字节落盘 3 字节的放大链路可见。
 *
 * <p>记账脑（喂点=store/markLinked 调用方或装配装饰）：total  counters +
 * 最近 {@value #WINDOW} 次写放大样本环（近窗 P50/P95 最近秩——总量是累计
 * 平均，近窗反映当前行为）。逻辑 0 写入忽略（不制造 ∞ 放大假象）。
 */
public final class SpillWriteAmplifier {

    /** 近窗样本容量。 */
    public static final int WINDOW = 64;

    /** 不可变读数。 */
    public record Stats(long writes, long logicalBytes, long physicalBytes,
                        double amplificationRatio, double recentRatio, double recentP95Ratio) {
    }

    private record Sample(double ratio) {
    }

    private final Deque<Sample> window = new ArrayDeque<>(WINDOW);
    private final ReentrantLock lock = new ReentrantLock();
    private long writes;
    private long logicalTotal;
    private long physicalTotal;

    /**
     * 记一次落盘写（logical=内容字节数，physical=实际写入字节含 meta/密文
     * 膨胀；逻辑 ≤0 忽略——不制造 ∞ 假象）。
     */
    public void recordWrite(long logicalBytes, long physicalBytes) {
        if (logicalBytes <= 0 || physicalBytes < 0) {
            return;
        }
        double ratio = (double) physicalBytes / logicalBytes;
        lock.lock();
        try {
            writes++;
            logicalTotal += logicalBytes;
            physicalTotal += physicalBytes;
            if (window.size() >= WINDOW) {
                window.pollFirst();
            }
            window.addLast(new Sample(ratio));
        } finally {
            lock.unlock();
        }
    }

    /** 只读统计（无写入时三 ratio 全 0）。 */
    public Stats stats() {
        lock.lock();
        try {
            List<Double> ratios = new ArrayList<>(window.size());
            for (Sample s : window) {
                ratios.add(s.ratio());
            }
            ratios.sort(Double::compare);
            return new Stats(writes, logicalTotal, physicalTotal,
                    logicalTotal == 0 ? 0 : (double) physicalTotal / logicalTotal,
                    ratios.isEmpty() ? 0 : avg(ratios),
                    ratios.isEmpty() ? 0 : ratios.get(nearestRankIndex(ratios.size(), 95)));
        } finally {
            lock.unlock();
        }
    }

    private static double avg(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static int nearestRankIndex(int n, int p) {
        int rank = (int) Math.ceil(p / 100.0 * n);
        return Math.min(Math.max(rank, 1), n) - 1;
    }
}
