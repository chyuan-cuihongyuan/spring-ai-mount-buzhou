package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 在线实验确定性分桶（spec 505 / T761，GrowthBook / Statsig 思想）：
 * sha256(experiment|unitKey) 低 32 位 floorMod 100 落桶 → 变体名（字典序）
 * 累积权重命中。权重和 ≤100、余量 = 未入组（null——GrowthBook 同语义）；
 * 跨实例零协调天然一致（415 亲和同思想）；哈希含实验名——同一 unit 在
 * 不同实验独立随机。未知实验 null 且零状态（有界——只数已声明变体）。
 *
 * <p>消费面归宿主：按 variant 选提示词/模型/参数；效果显著性计算归
 * 外部 OLAP（曝光经 snapshot/counter 出）。
 */
public final class ExperimentBucketer {

    /** 总桶数（百分比粒度）。 */
    static final int TOTAL_BUCKETS = 100;

    private final Map<String, Map<String, Integer>> experiments;
    /** spec 709 / T1018：可选到期时刻（实验 → 到期；无声明 = 永不过期）。 */
    private final Map<String, java.time.Instant> expiresAt;
    private final java.time.Clock clock;
    /** 到期已 WARN 过的实验（每实验一次——不刷屏）。 */
    private final java.util.Set<String> expiredWarned = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, AtomicLong>> exposures = new LinkedHashMap<>();

    public ExperimentBucketer(Map<String, Map<String, Integer>> experiments) {
        this(experiments, Map.of(), java.time.Clock.systemUTC());
    }

    /**
     * spec 709 / T1018（GrowthBook feature expiry 借鉴）：带到期声明构造——
     * assign() 惰性判定，过期实验按未入组处理（曝光计 __expired__ 独立桶）、
     * 返回 null；声明保留可审计。expiresAt 值 null fail-fast。
     */
    public ExperimentBucketer(Map<String, Map<String, Integer>> experiments,
                              Map<String, java.time.Instant> expiresAt, java.time.Clock clock) {
        Map<String, Map<String, Integer>> validated = new LinkedHashMap<>();
        experiments.forEach((name, variants) -> {
            if (variants == null || variants.isEmpty()) {
                throw new IllegalArgumentException("实验 " + name + " 变体权重非空");
            }
            int sum = 0;
            variants.forEach((variant, weight) -> {
                if (weight == null || weight < 0) {
                    throw new IllegalArgumentException(
                            "实验 " + name + " 变体 " + variant + " 权重非负");
                }
            });
            sum = variants.values().stream().mapToInt(Integer::intValue).sum();
            if (sum > TOTAL_BUCKETS) {
                throw new IllegalArgumentException("实验 " + name + " 权重和超 "
                        + TOTAL_BUCKETS + "（当前 " + sum + "）");
            }
            validated.put(name, Map.copyOf(variants));
            Map<String, AtomicLong> counters = new LinkedHashMap<>();
            new TreeMap<>(variants).keySet().forEach(v -> counters.put(v, new AtomicLong()));
            exposures.put(name, counters);
        });
        this.experiments = Map.copyOf(validated);
        Map<String, java.time.Instant> validatedExpiry = new java.util.LinkedHashMap<>();
        if (expiresAt != null) {
            expiresAt.forEach((name, at) -> {
                if (at == null) {
                    throw new IllegalArgumentException(
                            "实验 " + name + " 的 expiresAt 时刻为 null（不声明到期请勿放入该表）");
                }
                validatedExpiry.put(name, at);
            });
        }
        this.expiresAt = Map.copyOf(validatedExpiry);
        this.clock = clock == null ? java.time.Clock.systemUTC() : clock;
    }

    /** 已过期实验集合（声明保留、行为停用——审计面）。 */
    public java.util.Set<String> expiredExperiments() {
        java.time.Instant now = java.time.Instant.now(clock);
        java.util.Set<String> expired = new java.util.LinkedHashSet<>();
        expiresAt.forEach((name, at) -> {
            if (now.isAfter(at)) {
                expired.add(name);
            }
        });
        return expired;
    }

    /** 某实验到期时刻（无声明 = empty）。 */
    public java.util.Optional<java.time.Instant> expiresAt(String experiment) {
        return java.util.Optional.ofNullable(expiresAt.get(experiment));
    }

    /** 是否已到期（惰性判定共用口径）。 */
    private boolean expired(String experiment) {
        java.time.Instant at = expiresAt.get(experiment);
        return at != null && java.time.Instant.now(clock).isAfter(at);
    }

    /** 已声明实验数（观测）。 */
    public int experimentCount() {
        return experiments.size();
    }

    /**
     * 确定性分配：unitKey（如 appId|sessionId）→ 变体名；未入组/未知实验
     * 返回 null。同键恒同变体（跨实例、跨重启）。
     */
    public String assign(String experiment, String unitKey) {
        Map<String, Integer> variants = experiments.get(experiment);
        if (variants == null || unitKey == null || unitKey.isEmpty()) {
            return null;
        }
        if (expired(experiment)) {
            // spec 709：到期自动停——按未入组但计独立桶（不混自然余量口径）
            exposures.get(experiment).computeIfAbsent("__expired__", k -> new AtomicLong())
                    .incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.experiment.expired",
                    "experiment", experiment);
            if (expiredWarned.add(experiment)) {
                System.getLogger(ExperimentBucketer.class.getName()).log(
                        System.Logger.Level.WARNING,
                        "实验已到期自动停（声明保留、行为停用）：" + experiment
                                + "，到期 " + expiresAt.get(experiment));
            }
            return null;
        }
        int bucket = floorMod100(experiment, unitKey);
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : new TreeMap<>(variants).entrySet()) {
            cumulative += entry.getValue();
            if (bucket < cumulative) {
                recordExposure(experiment, entry.getKey());
                return entry.getKey();
            }
        }
        recordExposure(experiment, null);
        return null; // 未入组余量
    }

    /** sha256(experiment|unitKey) 低 32 位非负模 100。 */
    private static int floorMod100(String experiment, String unitKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (experiment + "|" + unitKey).getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.floorMod(value, TOTAL_BUCKETS);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private void recordExposure(String experiment, String variant) {
        Map<String, AtomicLong> counters = exposures.get(experiment);
        if (counters == null) {
            return;
        }
        if (variant != null) {
            counters.get(variant).incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.experiment.assigned",
                    "experiment", experiment, "variant", variant);
        } else {
            counters.computeIfAbsent("__unenrolled__", k -> new AtomicLong())
                    .incrementAndGet();
        }
    }

    /** 曝光快照（experiment → variant → 计数；未入组计 __unenrolled__）。 */
    public Map<String, Map<String, Long>> snapshot() {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        exposures.forEach((experiment, counters) -> {
            Map<String, Long> row = new LinkedHashMap<>();
            counters.forEach((variant, counter) -> row.put(variant, counter.get()));
            out.put(experiment, row);
        });
        return out;
    }

    /** 已声明变体名（字典序——观测/测试面）。 */
    public List<String> variantsOf(String experiment) {
        Map<String, Integer> variants = experiments.get(experiment);
        return variants == null ? List.of() : List.copyOf(new TreeMap<>(variants).keySet());
    }
}
