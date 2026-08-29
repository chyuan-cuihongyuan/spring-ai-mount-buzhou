package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轮次心跳（spec 138 §A / T463，Temporal Activity heartbeat 借鉴）：在飞轮次的
 * 「最近进展」时刻表——hook 在模型调用/工具返回/事件分发点打点 {@link #beat}，
 * 停滞检测问「还在飞但多久没动静了」。与 TurnDeadline（绝对时限）与 RunawayHook
 * （行为失控计数）正交：心跳管<b>进展活性</b>——活着但不动的轮次 deadline 未到、
 * 计数也不涨，只有心跳能看见。
 *
 * <p><b>口径</b>：注册制（{@code register}/{@code clear} 与轮次起止对齐——表只
 * 持在飞轮次，天然有界 {@value #MAX_IN_FLIGHT} 封顶，满则 fail-fast：在飞数
 * 超界是上层闸失守的信号，响亮胜过静默驱逐）；未注册 id 的 beat 为 no-op
 * （诚实：不是在飞轮次就不算进展）；{@link #stalled} 只检候选、不枚举全表
 * （调用方持有在飞名单——本表不猜生命周期）。停滞检出计数
 * {@code buzhou.turn.stalled-detected}。
 */
public final class TurnHeartbeat {

    /** 在飞轮次表封顶（与 spawn 上限同量级——超过即上层闸失守）。 */
    public static final int MAX_IN_FLIGHT = 1024;

    /** 单个停滞轮次（sessionId + 停滞时长——quiet 越久越靠前）。 */
    public record Stalled(String sessionId, Duration quietFor) {
    }

    private final Map<String, Instant> lastBeat = new ConcurrentHashMap<>();

    /** 注册在飞轮次（轮次开始；重复注册幂等刷新时刻）。 */
    public void register(String sessionId, Instant now) {
        requireSessionId(sessionId);
        requireNow(now);
        if (lastBeat.putIfAbsent(sessionId, now) == null && lastBeat.size() > MAX_IN_FLIGHT) {
            lastBeat.remove(sessionId);
            throw new IllegalArgumentException(
                    "in-flight turn registry full (max " + MAX_IN_FLIGHT + ")");
        }
    }

    /** 轮次结束移除（幂等；未知 id no-op）。 */
    public void clear(String sessionId) {
        lastBeat.remove(sessionId);
    }

    /** 进展打点（未注册 id no-op——不是在飞轮次就不算进展）。 */
    public void beat(String sessionId, Instant now) {
        requireNow(now);
        lastBeat.replace(sessionId, now);
    }

    /** 最近心跳时刻（未注册返回 null——诚实空值）。 */
    public Instant lastBeat(String sessionId) {
        return lastBeat.get(sessionId);
    }

    /** 在飞数（健康/测试面）。 */
    public int inFlight() {
        return lastBeat.size();
    }

    /** 在飞会话 id 快照（巡检犬自轮询用——注册制事实表的全集视图，字典序稳定）。 */
    public java.util.List<String> registered() {
        return lastBeat.keySet().stream().sorted().toList();
    }

    /** 单会话停滞时长查询（spec 196 §A / T558：未注册/未超阈 null——单点排障面）。 */
    public Duration stalledSince(String sessionId, Duration quietThreshold, Instant now) {
        if (quietThreshold == null || quietThreshold.isNegative()) {
            throw new IllegalArgumentException("quietThreshold must be non-negative");
        }
        requireNow(now);
        Instant beat = lastBeat.get(sessionId);
        if (beat == null) {
            return null;
        }
        Duration quiet = Duration.between(beat, now);
        return quiet.compareTo(quietThreshold) > 0 ? quiet : null;
    }

    /**
     * 停滞检测：候选会话里「已注册且 quiet &gt; threshold」者按停滞时长降序返回
     * （最长停滞优先——排障视线先落最卡处）；每检出一个计 stalled-detected。
     */
    public List<Stalled> stalled(List<String> candidateSessionIds, Duration quietThreshold,
                                 Instant now) {
        if (quietThreshold == null || quietThreshold.isNegative()) {
            throw new IllegalArgumentException("quietThreshold must be non-negative");
        }
        requireNow(now);
        List<Stalled> out = new ArrayList<>();
        for (String sessionId : candidateSessionIds) {
            Instant beat = lastBeat.get(sessionId);
            if (beat == null) {
                continue; // 未注册：不猜生命周期
            }
            Duration quiet = Duration.between(beat, now);
            if (quiet.compareTo(quietThreshold) > 0) {
                out.add(new Stalled(sessionId, quiet));
                BuzhouMetricsHolder.metrics().counter("buzhou.turn.stalled-detected");
            }
        }
        out.sort(Comparator.comparing(Stalled::quietFor).reversed());
        return out;
    }

    private static void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    private static void requireNow(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
    }
}
