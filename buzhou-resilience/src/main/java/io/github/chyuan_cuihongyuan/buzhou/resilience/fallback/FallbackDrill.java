package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;

/**
 * 降级链演练（spec 195 / T567，Envoy 主动健康检查借鉴）：宿主注册 per-model
 * 轻量验证探针——成功记 lastVerifiedAt、失败记 lastFailedAt（<b>不清
 * lastVerified</b>——「上次验证成功」事实保留）；isFresh/filter 按新鲜度取
 * 「验证过的备胎」；异常吞不上抛（演练不扰生产）。与离群驱逐（149）互补：
 * 驱逐管坏了的出局，演练管好的持证上岗。
 */
public final class FallbackDrill {

    /** 模型验证态。 */
    public record DrillState(Instant lastVerifiedAt, Instant lastFailedAt) {
    }

    private final Clock clock;
    private final Map<String, Callable<Boolean>> probes = new ConcurrentHashMap<>();
    private final Map<String, DrillState> states = new ConcurrentHashMap<>();

    public FallbackDrill() {
        this(Clock.systemUTC());
    }

    public FallbackDrill(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /** 注册演练探针（每模型的便宜验证方式——宿主最清楚；覆盖式）。 */
    public void register(String modelName, Callable<Boolean> probe) {
        if (modelName == null || modelName.isBlank() || probe == null) {
            throw new IllegalArgumentException("modelName/probe 非空");
        }
        probes.put(modelName, probe);
    }

    /**
     * 演练一个模型：成功记新、失败记败（旧 lastVerified 保留）；异常/false 都算
     * 失败且不上抛。返回是否演练成功。
     */
    public boolean drill(String modelName) {
        Callable<Boolean> probe = probes.get(modelName);
        if (probe == null) {
            return false; // 未注册探针——无从验证
        }
        boolean ok;
        try {
            ok = Boolean.TRUE.equals(probe.call());
        } catch (RuntimeException e) {
            ok = false;
        } catch (Exception e) {
            ok = false;
        }
        Instant now = clock.instant();
        boolean succeeded = ok;
        states.merge(modelName, new DrillState(succeeded ? now : null, succeeded ? null : now),
                (old, fresh) -> new DrillState(
                        succeeded ? now : old.lastVerifiedAt(),
                        succeeded ? old.lastFailedAt() : now));
        return ok;
    }

    /** 演练全部已注册模型（返回 成功集合/失败集合）。 */
    public DrillOutcome drillAll() {
        List<String> ok = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        probes.keySet().forEach(name -> {
            if (drill(name)) {
                ok.add(name);
            } else {
                failed.add(name);
            }
        });
        return new DrillOutcome(List.copyOf(ok), List.copyOf(failed));
    }

    /** 一轮演练结果。 */
    public record DrillOutcome(List<String> ok, List<String> failed) {
    }

    /** 是否「近期验证过」（无验证记录 = false——从未验证不算新鲜）。 */
    public boolean isFresh(String modelName, Duration maxAge) {
        DrillState state = states.get(modelName);
        return state != null && state.lastVerifiedAt() != null
                && Duration.between(state.lastVerifiedAt(), clock.instant())
                        .compareTo(maxAge) < 0;
    }

    /** 新鲜备胎视图（保序剔除过期未验者——挂 FallbackChain.models() 之后）。 */
    public List<NamedFallbackModel> filter(List<NamedFallbackModel> candidates,
                                           Duration maxAge) {
        List<NamedFallbackModel> fresh = new ArrayList<>();
        for (NamedFallbackModel candidate : candidates) {
            if (isFresh(candidate.name(), maxAge)) {
                fresh.add(candidate);
            }
        }
        return fresh;
    }

    /** 验证态观测面。 */
    public Map<String, DrillState> states() {
        return Map.copyOf(states);
    }
}
