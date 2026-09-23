package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 热点 Key 探测器（spec 5019 / T6139 / impl 2170）——确定性
 * 采样 + 阈值告警思想（Redis/网关热点 key 面）：`record(key)`
 * 按**全局序号 mod sampleRate** 命中采样一次计数（确定性、
 * 无随机——同访问序列同采样点），采样计数 ≥ `hotThreshold`
 * 即热点。全量计数（内存随 key 基数爆炸）与拍脑袋告警
 * （阈值无观测依据）的病解。
 *
 * <p>与 CountMinSketch（R2 频次估计）互补：概率估计 vs
 * 确定性采样告警。
 */
public final class HotKeyDetector {

    private final int sampleRate;
    private final long hotThreshold;
    private final Map<String, Long> sampledCounts = new TreeMap<>();
    private long globalSequence;

    /** 定构（sampleRate>0、hotThreshold>0 否则 fail-fast）。 */
    public HotKeyDetector(int sampleRate, long hotThreshold) {
        if (sampleRate <= 0 || hotThreshold <= 0) {
            throw new IllegalArgumentException("sampleRate>0 且 hotThreshold>0："
                    + sampleRate + "/" + hotThreshold);
        }
        this.sampleRate = sampleRate;
        this.hotThreshold = hotThreshold;
    }

    /** 记账一次访问（全局序号命中采样点才计数）。 */
    public void record(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key 非空");
        }
        long slot = globalSequence % sampleRate;
        globalSequence++;
        if (slot != sampleRate - 1) {
            return;   // 非采样点
        }
        sampledCounts.merge(key, 1L, Long::sum);
    }

    /** 该 key 是否已达热点阈值。 */
    public boolean isHot(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key 非空");
        }
        return sampledCounts.getOrDefault(key, 0L) >= hotThreshold;
    }

    /** 热点 key 全集（字典序确定性）。 */
    public List<String> hotKeys() {
        return sampledCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= hotThreshold)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 该 key 的采样计数读数。 */
    public long sampledCountOf(String key) {
        return sampledCounts.getOrDefault(key, 0L);
    }

    /** 全局访问序号读数。 */
    public long globalSequence() {
        return globalSequence;
    }

    /** 采样率读数。 */
    public int sampleRate() {
        return sampleRate;
    }
}
