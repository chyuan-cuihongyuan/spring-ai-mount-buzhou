package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;

/**
 * 会话隔离检疫（spec 143 / T495，Erlang/OTP supervisor「let it crash + 指数退避」
 * 借鉴）：连续失败 ≥ 阈值 → 隔离至 releaseAt（冷却按跳闸次数指数升级，封顶 max），
 * 到时自动解除；隔离期 {@link #admitOrThrow} 抛
 * {@link BuzhouException}（SESSION_QUARANTINED，message 含剩余冷却与连败数）。
 *
 * <p>成功复位走 {@link #recordTurnSuccess} 公共 API（hook 面看不到「健康轮」全貌
 * ——不谎装自动复位）。per-session 单锁（对齐 ToolCircuitBreaker 风格）。
 */
public final class SessionQuarantine {

    /** 观测行：隔离中=剩余毫秒（非隔离=0）/ 连败数 / 跳闸次数。 */
    public record View(long remainingMillis, int consecutiveFailures, int trips) {
    }

    /** 配置（阈值≥1；base/max 正且 max≥base）。 */
    public record Config(int failureThreshold, Duration baseBackoff, Duration maxBackoff) {
        public Config {
            if (failureThreshold < 1 || baseBackoff == null || baseBackoff.isZero()
                    || baseBackoff.isNegative() || maxBackoff == null
                    || maxBackoff.isNegative() || maxBackoff.compareTo(baseBackoff) < 0) {
                throw new IllegalArgumentException(
                        "检疫配置非法（阈值>=1、backoff 正、max>=base）");
            }
        }

        public static Config defaults() {
            return new Config(3, Duration.ofSeconds(30), Duration.ofMinutes(10));
        }
    }

    private static final String TRIPPED_COUNTER = "buzhou.quarantine.tripped";

    private static final class SessionState {
        int consecutiveFailures;
        int trips;
        /** 隔离截止毫秒（0 = 未隔离——过去时刻恒放行）。 */
        long releaseAtMillis;
    }

    private final Config config;
    private final Clock clock;
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionQuarantine() {
        this(Config.defaults(), Clock.systemUTC());
    }

    public SessionQuarantine(Config config, Clock clock) {
        this.config = config == null ? Config.defaults() : config;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /** 准入：隔离中抛 SESSION_QUARANTINED（含剩余冷却与连败数）；到时自动解除放行。 */
    public void admitOrThrow(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return;
        }
        synchronized (state) {
            long now = clock.millis();
            if (now >= state.releaseAtMillis) {
                return; // 到时自动解除（连败计数保留——再败快跳且更冷）
            }
            long remaining = state.releaseAtMillis - now;
            throw new BuzhouException(ErrorCode.SESSION_QUARANTINED,
                    "会话隔离检疫中（连续失败 " + state.consecutiveFailures + " 次，第 "
                            + state.trips + " 次跳闸，剩余冷却 "
                            + (remaining + 999) / 1000 + "s；请稍后再试或修复失败根因）");
        }
    }

    /** 计一次失败：达到阈值即跳闸隔离（冷却指数升级）。 */
    public void recordTurnFailure(String sessionId) {
        SessionState state = sessions.computeIfAbsent(sessionId, k -> new SessionState());
        synchronized (state) {
            state.consecutiveFailures++;
            if (state.consecutiveFailures >= config.failureThreshold()) {
                state.trips++;
                long backoff = backoffMillis(state.trips);
                state.releaseAtMillis = clock.millis() + backoff;
                BuzhouMetricsHolder.metrics().counter(TRIPPED_COUNTER, 1);
            }
        }
    }

    /** 成功复位（宿主/装配在健康轮调用——清零连败；隔离冷却不受影响仍到时解除）。 */
    public void recordTurnSuccess(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.consecutiveFailures = 0;
        }
    }

    /** 观测面（稳定序）。 */
    public Map<String, View> snapshot() {
        Map<String, View> out = new TreeMap<>();
        long now = clock.millis();
        sessions.forEach((id, state) -> {
            synchronized (state) {
                long remaining = state.releaseAtMillis > now
                        ? state.releaseAtMillis - now : 0;
                out.put(id, new View(remaining,
                        state.consecutiveFailures, state.trips));
            }
        });
        return out;
    }

    /** 第 n 次跳闸冷却：base×2^(n-1) 封顶 max。 */
    private long backoffMillis(int trips) {
        long exponential = config.baseBackoff().toMillis() << Math.min(trips - 1, 20);
        return Math.min(exponential, config.maxBackoff().toMillis());
    }
}
