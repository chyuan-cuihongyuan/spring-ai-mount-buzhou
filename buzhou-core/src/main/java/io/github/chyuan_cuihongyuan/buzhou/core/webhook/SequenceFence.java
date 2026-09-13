package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 投递序号围栏（spec 159 / T517 + spec 303 / T597 纪元化）：per 订阅流追踪
 * （纪元, 上次序号）——CONTINUE（连续/首见/无 seq 兼容）/ GAP（跳号——缺的
 * 显形，接收方该对账）/ DUPLICATE（重投——at-least-once 正常，幂等键去重既有）/
 * RESET（<b>显式</b>新纪元或同纪元 seq 倒退的防御性重基线）/ STALE（旧纪元
 * 迟到投递——安全丢弃，基线不动）。
 *
 * <p>纪元语义（spec 303，Kafka producer epoch 借鉴）：发送方每次启动持久递增
 * 纪元并显式写入信封 {@code epoch} 字段——重启不再靠「seq 变小」猜测；无
 * epoch 的旧发送方走原 seq 推断路径（兼容，逐位不变）。
 *
 * <p>单订阅流内串行假设（webhook dispatcher 单线程投递天然成立）；无锁。
 */
public final class SequenceFence {

    /** 围栏裁决。 */
    public enum Verdict { CONTINUE, GAP, DUPLICATE, RESET, STALE }

    /** GAP 详情（缺号区间——对账拉取的依据）。 */
    public record GapDetail(long expectedNext, long seen) {
    }

    private record Baseline(Long epoch, long lastSeen, boolean initialized) {
    }

    private final Map<String, Baseline> baselines = new ConcurrentHashMap<>();
    /** impl-795 / spec 1043：五态裁决分布（固定键有界——订阅流健康水位）。 */
    private final Map<String, java.util.concurrent.atomic.AtomicLong> verdictCounts =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 观察一次投递的 seq（旧发送方兼容路径——无纪元，seq 倒退推断为 RESET）。
     *
     * @param subscriptionId 订阅流（各自独立基线）
     * @param seq            信封 seq（null = 旧信封——兼容放行不改基线）
     */
    public Verdict observe(String subscriptionId, Long seq) {
        return observe(subscriptionId, null, seq);
    }

    /**
     * 观察一次（纪元, seq）投递，给出裁决并更新基线（spec 303 判定矩阵）。
     *
     * @param subscriptionId 订阅流（各自独立基线）
     * @param epoch          信封纪元（null = 旧发送方兼容路径）
     * @param seq            信封 seq（null = 旧信封——兼容放行不改基线）
     */
    public Verdict observe(String subscriptionId, Long epoch, Long seq) {
        Verdict verdict = judge(subscriptionId, epoch, seq);
        verdictCounts.computeIfAbsent(verdict.name(),
                k -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
        return verdict;
    }

    /**
     * 判定矩阵（纯函数语义：基线状态迁移按原实现保留——spec 303 判定矩阵逐位不变）。
     * 返回本次投递的裁决。
     */
    private Verdict judge(String subscriptionId, Long epoch, Long seq) {
        if (seq == null) {
            return Verdict.CONTINUE; // 旧信封兼容
        }
        Baseline baseline = baselines.get(subscriptionId);
        if (baseline == null || !baseline.initialized()) {
            baselines.put(subscriptionId, new Baseline(epoch, seq, true));
            return Verdict.CONTINUE; // 首见建基线
        }
        if (epoch == null) {
            // 旧发送方：seq 单独推断（159 既有语义逐位不变）
            if (seq.equals(baseline.lastSeen())) {
                return Verdict.DUPLICATE;
            }
            if (seq < baseline.lastSeen()) {
                baselines.put(subscriptionId, new Baseline(null, seq, true));
                return Verdict.RESET;
            }
            baselines.put(subscriptionId, new Baseline(null, seq, true));
            return seq == baseline.lastSeen() + 1 ? Verdict.CONTINUE : Verdict.GAP;
        }
        if (baseline.epoch() != null && epoch < baseline.epoch()) {
            return Verdict.STALE; // 旧纪元迟到——安全丢弃，基线不动
        }
        if (baseline.epoch() == null || epoch > baseline.epoch()) {
            // 显式新纪元（发送方重启）：重基线
            baselines.put(subscriptionId, new Baseline(epoch, seq, true));
            return Verdict.RESET;
        }
        // 同纪元：既有四态
        if (seq.equals(baseline.lastSeen())) {
            return Verdict.DUPLICATE;
        }
        if (seq < baseline.lastSeen()) {
            baselines.put(subscriptionId, new Baseline(epoch, seq, true));
            return Verdict.RESET; // 同纪元倒退——异常，防御性重基线
        }
        baselines.put(subscriptionId, new Baseline(epoch, seq, true));
        return seq == baseline.lastSeen() + 1 ? Verdict.CONTINUE : Verdict.GAP;
    }

    /** 最近一次缺口详情（对账拉取依据；无缺口/未建基线 = null）。 */
    public GapDetail lastGap(String subscriptionId) {
        Baseline baseline = baselines.get(subscriptionId);
        return baseline == null ? null : new GapDetail(baseline.lastSeen(), baseline.lastSeen());
    }

    /** 围栏裁决分布只读快照（五桶恒在——spec 1043 订阅流健康水位）。 */
    public FenceVerdictStats verdictStats() {
        Map<String, Long> byVerdict = new java.util.TreeMap<>();
        for (Verdict v : Verdict.values()) {
            byVerdict.put(v.name(), verdictCount(v.name()));
        }
        return new FenceVerdictStats(Map.copyOf(byVerdict));
    }

    private long verdictCount(String verdict) {
        return java.util.Optional.ofNullable(verdictCounts.get(verdict))
                .map(java.util.concurrent.atomic.AtomicLong::get).orElse(0L);
    }

    /** 围栏裁决分布行（不可变；byVerdict 恒含五态键）。 */
    public record FenceVerdictStats(Map<String, Long> byVerdict) {

        public FenceVerdictStats {
            byVerdict = Map.copyOf(byVerdict);
        }

        public long total() {
            return byVerdict.values().stream().mapToLong(Long::longValue).sum();
        }
    }
}
