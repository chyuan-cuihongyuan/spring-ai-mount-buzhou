package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投递序号围栏（spec 159 / T517，Kafka producer seq + consumer gap detection
 * 借鉴）：per 订阅流追踪上次序号——CONTINUE（连续/首见/无 seq 兼容）/
 * GAP（跳号——缺的显形，接收方该对账）/ DUPLICATE（重投——at-least-once 正常，
 * 幂等键去重既有）/ RESET（seq 变小——发送方重启新纪元，基线重置）。
 *
 * <p>单订阅流内串行假设（webhook dispatcher 单线程投递天然成立）；无锁。
 */
public final class SequenceFence {

    /** 围栏裁决。 */
    public enum Verdict { CONTINUE, GAP, DUPLICATE, RESET }

    /** GAP 详情（缺号区间——对账拉取的依据）。 */
    public record GapDetail(long expectedNext, long seen) {
    }

    private record Baseline(long lastSeen, boolean initialized) {
    }

    private final Map<String, Baseline> baselines = new ConcurrentHashMap<>();

    /**
     * 观察一次投递的 seq，给出裁决并更新基线。
     *
     * @param subscriptionId 订阅流（各自独立基线）
     * @param seq            信封 seq（null = 旧信封——兼容放行不改基线）
     */
    public Verdict observe(String subscriptionId, Long seq) {
        if (seq == null) {
            return Verdict.CONTINUE; // 旧发送方兼容
        }
        Baseline baseline = baselines.get(subscriptionId);
        if (baseline == null || !baseline.initialized()) {
            baselines.put(subscriptionId, new Baseline(seq, true));
            return Verdict.CONTINUE; // 首见建基线
        }
        if (seq == baseline.lastSeen()) {
            return Verdict.DUPLICATE; // 重投——基线不变
        }
        if (seq < baseline.lastSeen()) {
            baselines.put(subscriptionId, new Baseline(seq, true));
            return Verdict.RESET; // 新纪元——基线重置
        }
        baselines.put(subscriptionId, new Baseline(seq, true));
        return seq == baseline.lastSeen() + 1 ? Verdict.CONTINUE : Verdict.GAP;
    }

    /** 最近一次缺口详情（对账拉取依据；无缺口/未建基线 = null）。 */
    public GapDetail lastGap(String subscriptionId) {
        Baseline baseline = baselines.get(subscriptionId);
        return baseline == null ? null : new GapDetail(baseline.lastSeen(), baseline.lastSeen());
    }
}
