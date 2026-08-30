package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * per-tool 熔断器（spec 131 / T477，resilience4j CircuitBreaker 语义）：
 * CLOSED（失败率滑窗）→ 失败率 ≥ 阈值 → OPEN（冷却拒调用）→ 冷却耗尽 →
 * HALF_OPEN（N 次探测）→ 全成 CLOSED（窗清零）/ 任一败 OPEN 重新冷却。
 *
 * <p>与 spec 15 模型熔断（模型面）正交——本类管<b>工具面</b>。结局判定归调用方
 * （{@code ToolCircuitBreakerHook} 走 ToolFeedbackType 结构化标记）；本类只做
 * 状态机与许可。未注册工具状态惰性创建；线程安全（per-tool 单锁）。
 *
 * <p>全局旋钮风格对齐 {@link AgentBulkhead}（观测面 snapshot + 计数环形滑窗，
 * 不存调用明细）。
 */
public final class ToolCircuitBreaker {

    /** 熔断状态。 */
    public enum State { CLOSED, OPEN, HALF_OPEN }

    /** 观测行（state / 窗内成败 / 拒绝次数 / OPEN 剩余冷却毫秒）。 */
    public record View(State state, long windowSuccess, long windowFailure,
                       long blocked, long cooldownRemainingMillis) {
    }

    /** 配置（全部有界正值；failureRateThreshold ∈ (0,100]。 */
    public record Config(int windowSize, double failureRateThresholdPercent,
                         Duration cooldown, int halfOpenTrials) {
        public Config {
            if (windowSize < 2 || !(failureRateThresholdPercent > 0
                    && failureRateThresholdPercent <= 100) || cooldown == null
                    || cooldown.isZero() || cooldown.isNegative() || halfOpenTrials < 1) {
                throw new IllegalArgumentException(
                        "熔断配置非法（windowSize>=2、阈值∈(0,100]、cooldown 正、halfOpenTrials>=1）");
            }
        }

        public static Config defaults() {
            return new Config(20, 50.0, Duration.ofSeconds(60), 3);
        }
    }

    private static final class ToolState {
        long windowSuccess;
        long windowFailure;
        long blocked;
        State state = State.CLOSED;
        Instant openedAt;
        int halfOpenGranted;
        int halfOpenSuccess;

        long windowTotal() {
            return windowSuccess + windowFailure;
        }
    }

    private final Config config;
    private final Clock clock;
    private final Map<String, ToolState> tools = new java.util.concurrent.ConcurrentHashMap<>();

    public ToolCircuitBreaker() {
        this(Config.defaults(), Clock.systemUTC());
    }

    public ToolCircuitBreaker(Config config, Clock clock) {
        this.config = config == null ? Config.defaults() : config;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 是否放行一次调用：CLOSED 放行；OPEN 冷却耗尽转 HALF_OPEN 并放行首次探测，
     * 未耗尽拒绝；HALF_OPEN 探测名额内放行、超额拒绝。拒绝计 blocked。
     */
    public boolean tryAcquirePermission(String toolName) {
        ToolState state = tools.computeIfAbsent(toolName, k -> new ToolState());
        synchronized (state) {
            switch (state.state) {
                case CLOSED -> {
                    return true;
                }
                case OPEN -> {
                    long elapsed = Duration.between(state.openedAt, clock.instant()).toMillis();
                    if (elapsed >= config.cooldown().toMillis()) {
                        state.state = State.HALF_OPEN;
                        state.halfOpenGranted = 1;
                        state.halfOpenSuccess = 0;
                        return true;
                    }
                    state.blocked++;
                    return false;
                }
                case HALF_OPEN -> {
                    if (state.halfOpenGranted < config.halfOpenTrials()) {
                        state.halfOpenGranted++;
                        return true;
                    }
                    state.blocked++;
                    return false;
                }
                default -> {
                    return true;
                }
            }
        }
    }

    /** 记一次成功：HALF_OPEN 攒满探测全成 → CLOSED 清窗；CLOSED 计窗并判率（与失败对称）。 */
    public void recordSuccess(String toolName) {
        ToolState state = tools.computeIfAbsent(toolName, k -> new ToolState());
        synchronized (state) {
            if (state.state == State.HALF_OPEN) {
                state.halfOpenSuccess++;
                if (state.halfOpenSuccess >= config.halfOpenTrials()) {
                    toClosed(state);
                }
                return;
            }
            if (state.state == State.CLOSED) {
                recordInWindow(state, true);
                judgeAndTrip(state);
            }
        }
    }

    /** 记一次失败：HALF_OPEN 一败即重开；CLOSED 计窗并判率。 */
    public void recordFailure(String toolName) {
        ToolState state = tools.computeIfAbsent(toolName, k -> new ToolState());
        synchronized (state) {
            if (state.state == State.HALF_OPEN) {
                toOpen(state);
                return;
            }
            if (state.state == State.CLOSED) {
                recordInWindow(state, false);
                judgeAndTrip(state);
            }
        }
    }

    /** 窗满且失败率 ≥ 阈值 → OPEN（成功/失败记录后对称判定——满窗后的成功也可能触发）。 */
    private void judgeAndTrip(ToolState state) {
        if (state.windowTotal() >= config.windowSize()
                && failureRatePercent(state) >= config.failureRateThresholdPercent()) {
            toOpen(state);
        }
    }

    /** 观测面：单工具状态（未注册 = CLOSED 零值行）。 */
    public View stateOf(String toolName) {
        ToolState state = tools.get(toolName);
        if (state == null) {
            return new View(State.CLOSED, 0, 0, 0, 0);
        }
        synchronized (state) {
            long remaining = state.state == State.OPEN && state.openedAt != null
                    ? Math.max(0, config.cooldown().toMillis()
                            - Duration.between(state.openedAt, clock.instant()).toMillis())
                    : 0;
            return new View(state.state, state.windowSuccess, state.windowFailure,
                    state.blocked, remaining);
        }
    }

    /** 观测面：全部工具快照（稳定序）。 */
    public Map<String, View> snapshot() {
        Map<String, View> out = new LinkedHashMap<>();
        new java.util.TreeSet<>(tools.keySet()).forEach(name -> out.put(name, stateOf(name)));
        return out;
    }

    private void recordInWindow(ToolState state, boolean success) {
        if (state.windowTotal() >= config.windowSize()) {
            // 满窗滑动：按比例折半（近似滑窗——计数环不存明细）
            state.windowSuccess = state.windowSuccess / 2;
            state.windowFailure = state.windowFailure / 2;
        }
        if (success) {
            state.windowSuccess++;
        } else {
            state.windowFailure++;
        }
    }

    private static double failureRatePercent(ToolState state) {
        long total = state.windowTotal();
        return total == 0 ? 0.0 : state.windowFailure * 100.0 / total;
    }

    private void toOpen(ToolState state) {
        state.state = State.OPEN;
        state.openedAt = clock.instant();
        state.halfOpenGranted = 0;
        state.halfOpenSuccess = 0;
    }

    private void toClosed(ToolState state) {
        state.state = State.CLOSED;
        state.windowSuccess = 0;
        state.windowFailure = 0;
        state.halfOpenGranted = 0;
        state.halfOpenSuccess = 0;
        state.openedAt = null;
    }
}
