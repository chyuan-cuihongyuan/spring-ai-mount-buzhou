package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 模型熔断器（spec 15「熔断器」，T81 / impl-56）：手写 CB，不引 resilience4j。
 *
 * <p><b>进程级注册表</b>：按 modelName 分桶（与 {@code ModelRateLimiter} 同口径），在
 * {@code ResilienceModule.configure()} 创建一次、经装配定制器闭包注入全部会话——provider 健康是
 * 进程级事实，不能按会话分桶。
 *
 * <p><b>三态结果计数</b>（对齐 resilience4j 的 failure/ignore 语义）：FAILURE = 终态失败且
 * category ∈ {@code failure-categories}（默认 NETWORK/SERVER/TIMEOUT）；RATE_LIMIT（背压归限流器）/
 * CONTENT（内容治理）/ AUTH（配置错误，跳闸只会遮蔽根因）/ UNKNOWN（不明原因不盲跳）为 IGNORED，
 * 不进窗口；正常返回为 SUCCESS。
 *
 * <p><b>状态机</b>：CLOSED 下计数窗口失败率 ≥ 阈值且样本 ≥ min-calls → OPEN（跳闸）；OPEN 冷却期内
 * 调用抛 {@link ModelCircuitOpenException}；冷却后放行单探测进 HALF_OPEN——探测 FAILURE 回 OPEN
 * （重计冷却），SUCCESS/IGNORED 回 CLOSED（窗口重置）。半开探测带超时逃生（探测调用卡死不锁死熔断器）。
 *
 * <p><b>冷却自适应（spec 25 / T104 / impl-79）</b>：连续跳闸（探测失败回 OPEN 不复位）驱动冷却
 * 指数退避——冷却 = {@code open-cooldown × min(2^(trips-1), backoff-cap)}（cap 默认 8）；
 * 半开探测成功回 CLOSED 即 trips=0 复位。反复跳闸场景下避免「冷却→探测→立刻再跳」的无效循环
 * 放大故障传导。跳闸事件 payload 携带 {@code consecutiveTrips} 与生效 {@code openDurationMs}；
 * 指标 {@code buzhou.resilience.circuit-backoff-multiplier}（按 model 分桶 gauge）。
 *
 * <p>线程安全：每模型一把监视器锁（模型调用频率下无争用热点；正确性优先）。
 */
public final class ModelCircuitBreaker {

    /** 状态迁移事件（modelName/from/to）——走当次调用会话通道。 */
    public static final String EVENT_STATE_CHANGED = "circuit.state-changed";
    /** OPEN/HALF_OPEN 占位期调用被拒事件（modelName/state/retryInMs）。 */
    public static final String EVENT_CALL_REJECTED = "circuit.call-rejected";

    private static final System.Logger LOGGER = System.getLogger(ModelCircuitBreaker.class.getName());

    private final ResilienceProperties.Circuit config;
    private final Set<String> failureCategories;
    private final ResilienceStats stats; // null 安全：编程式路径未传时静默
    private final java.time.Clock clock;
    private final ConcurrentHashMap<String, ModelCircuit> circuits = new ConcurrentHashMap<>();

    public ModelCircuitBreaker(ResilienceProperties.Circuit config, ResilienceStats stats) {
        this(config, stats, java.time.Clock.systemUTC());
    }

    /**
     * spec 41 §B / T154 / impl-125：时钟注入——冷却/半开/退避的时间行为经可配 Clock 驱动
     * （测试可零真实等待推进时间）；缺省 systemUTC 与既往行为一致。
     */
    public ModelCircuitBreaker(ResilienceProperties.Circuit config, ResilienceStats stats,
            java.time.Clock clock) {
        this.config = config;
        this.stats = stats;
        this.clock = clock == null ? java.time.Clock.systemUTC() : clock;
        this.failureCategories = config.failureCategories().stream()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 调用前置闸：CLOSED 放行；OPEN 冷却完毕升 HALF_OPEN 并放行为探测、未冷却抛
     * {@link ModelCircuitOpenException}；HALF_OPEN 探测占位（或探测超时逃生重放行）。
     */
    public void beforeCall(String modelName, Consumer<SessionEvent> emitter) {
        Admission admission = circuit(modelName).admit(emitter);
        if (admission.admitted()) {
            return;
        }
        if (stats != null) {
            stats.recordCircuitRejected();
        }
        metrics().counter("buzhou.resilience.circuit-rejected", "model", modelName);
        if (emitter != null) {
            emitter.accept(new SessionEvent(EVENT_CALL_REJECTED,
                    Map.of("modelName", modelName, "state", admission.state().name(),
                            "retryInMs", admission.retryInMs()),
                    Instant.now(clock)));
        }
        LOGGER.log(System.Logger.Level.INFO,
                "熔断器拒绝调用：model=" + modelName + "，state=" + admission.state()
                        + "，retryInMs=" + admission.retryInMs());
        throw new ModelCircuitOpenException(modelName, admission.state(),
                Duration.ofMillis(admission.retryInMs()));
    }

    /** 逻辑调用成功（含返回了 CONTENT 静默拒绝的响应——provider 可用性正常）。 */
    public void recordSuccess(String modelName, Consumer<SessionEvent> emitter) {
        circuit(modelName).record(Outcome.SUCCESS, emitter);
    }

    /** 逻辑调用终态失败：按类别映射 FAILURE / IGNORED 后入账（TIMEOUT 为字符串口径，非 ErrorCategory 枚举）。 */
    public void recordTerminal(String modelName, String category, Consumer<SessionEvent> emitter) {
        circuit(modelName).record(countsAsFailure(category) ? Outcome.FAILURE : Outcome.IGNORED, emitter);
    }

    /** 类别是否计入熔断失败（IGNORED 类别不进窗口）。 */
    public boolean countsAsFailure(String category) {
        return category != null && failureCategories.contains(category.toUpperCase(Locale.ROOT));
    }

    /** 探测/观测用：当前状态（未见过该模型 = CLOSED）。 */
    public CircuitState state(String modelName) {
        return circuit(modelName).snapshotState();
    }

    private ModelCircuit circuit(String modelName) {
        String key = modelName == null ? "unknown" : modelName;
        return circuits.computeIfAbsent(key, ModelCircuit::new);
    }

    private static BuzhouMetrics metrics() {
        return BuzhouMetricsHolder.metrics();
    }

    /** 逻辑调用三态结果。 */
    private enum Outcome {
        SUCCESS, FAILURE, IGNORED
    }

    /** 单模型熔断状态机（监视器锁串行化，模型调用频率下无热点）。 */
    private final class ModelCircuit {
        private final String modelName;
        private CircuitState state = CircuitState.CLOSED;
        private Instant openedAt;
        private Instant halfOpenSince;
        private int probesInFlight;
        /** spec 35 §A / T118：半开已连续成功探测数（达 halfOpenSuccessThreshold 才 CLOSE）。 */
        private int halfOpenSuccesses;
        /** 连续跳闸次数（探测成功回 CLOSED 复位为 0；spec 25 / T104）。 */
        private int consecutiveTrips;
        /** 本次跳闸的生效冷却（base × 退避倍数；CLOSED 复位为 base）。 */
        private long effectiveCooldownMs;
        /** 计数窗口：true=失败样本（ring buffer，满窗移出最老）。 */
        private boolean[] window;
        private int samples;
        private int failures;

        ModelCircuit(String modelName) {
            this.modelName = modelName;
            this.window = new boolean[config.windowSize()];
            this.effectiveCooldownMs = config.openCooldown().toMillis();
            metrics().gauge("buzhou.resilience.circuit-open",
                    () -> snapshotState() == CircuitState.OPEN ? 1 : 0, "model", modelName);
            metrics().gauge("buzhou.resilience.circuit-backoff-multiplier",
                    this::backoffMultiplier, "model", modelName);
        }

        synchronized Admission admit(Consumer<SessionEvent> emitter) {
            switch (state) {
                case OPEN:
                    if (elapsedSince(openedAt).toMillis() >= effectiveCooldownMs) {
                        transition(CircuitState.HALF_OPEN, emitter);
                        halfOpenSuccesses = 0;
                        probesInFlight = 1;
                        halfOpenSince = Instant.now(clock);
                        return Admission.admit();
                    }
                    long remaining = Math.max(0, effectiveCooldownMs - elapsedSince(openedAt).toMillis());
                    return Admission.reject(CircuitState.OPEN, remaining);
                case HALF_OPEN:
                    // 探测槽位 = 阈值总数；已成功数永久占用一槽，在飞占一槽——满员即拒
                    //（防半开打爆刚恢复的 provider；逃生窗口外重置槽位重放行）。
                    if (probesInFlight + halfOpenSuccesses >= config.halfOpenSuccessThreshold()
                            && elapsedSince(halfOpenSince).compareTo(probeEscapeAfter()) < 0) {
                        return Admission.reject(CircuitState.HALF_OPEN, effectiveCooldownMs);
                    }
                    // 探测超时逃生：上次探测调用卡死（未走到终态记录），重置占位重放行。
                    if (elapsedSince(halfOpenSince).compareTo(probeEscapeAfter()) >= 0) {
                        probesInFlight = 0;
                    }
                    probesInFlight++;
                    halfOpenSince = Instant.now(clock);
                    return Admission.admit();
                case CLOSED:
                default:
                    return Admission.admit();
            }
        }

        /** 逻辑调用终态入账：HALF_OPEN 解探测；CLOSED 入窗口（IGNORED 不入）；OPEN 丢弃旧世代样本。 */
        synchronized void record(Outcome outcome, Consumer<SessionEvent> emitter) {
            if (state == CircuitState.HALF_OPEN) {
                probesInFlight = Math.max(0, probesInFlight - 1);
                if (outcome == Outcome.FAILURE) {
                    transition(CircuitState.OPEN, emitter); // 任一探测失败即回 OPEN 重计退避
                    return;
                }
                halfOpenSuccesses++;
                if (halfOpenSuccesses >= config.halfOpenSuccessThreshold()) {
                    transition(CircuitState.CLOSED, emitter); // 连续 N 次成功才恢复
                }
                return;
            }
            if (state == CircuitState.OPEN) {
                return; // 跳闸前在飞调用的迟到结果：窗口已随跳闸重置，丢弃样本
            }
            if (outcome == Outcome.IGNORED) {
                return; // 非可用性失败（RATE_LIMIT/CONTENT/AUTH/UNKNOWN）：不进窗口
            }
            append(outcome == Outcome.FAILURE);
            double rate = samples == 0 ? 0.0 : (double) failures / samples;
            if (samples >= config.minCalls() && rate >= config.failureRateThreshold()) {
                transition(CircuitState.OPEN, emitter);
            }
        }

        synchronized CircuitState snapshotState() {
            return state;
        }

        private void append(boolean failure) {
            int size = window.length;
            if (samples >= size && window[samples % size]) {
                failures--; // 满窗移出最老样本
            }
            window[samples % size] = failure;
            if (failure) {
                failures++;
            }
            samples++;
        }

        private void transition(CircuitState to, Consumer<SessionEvent> emitter) {
            CircuitState from = state;
            state = to;
            resetWindow();
            if (to == CircuitState.OPEN) {
                consecutiveTrips++;
                long multiplier = config.backoffMultiplier(consecutiveTrips);
                effectiveCooldownMs = config.openCooldown().toMillis() * multiplier;
                openedAt = Instant.now(clock);
                probesInFlight = 0;
                if (stats != null) {
                    stats.recordCircuitTrip(modelName);
                    stats.updateCircuitBackoff(modelName, multiplier);
                }
                metrics().counter("buzhou.resilience.circuit-tripped", "model", modelName);
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型熔断器跳闸：model=" + modelName + "，" + from + " → OPEN（冷却 "
                                + effectiveCooldownMs + "ms，连续第 " + consecutiveTrips
                                + " 次，退避 ×" + multiplier + "）");
            } else if (to == CircuitState.CLOSED) {
                consecutiveTrips = 0;
                effectiveCooldownMs = config.openCooldown().toMillis();
                if (from != CircuitState.CLOSED) {
                    LOGGER.log(System.Logger.Level.INFO,
                            "模型熔断器恢复：model=" + modelName + "，" + from + " → CLOSED");
                }
            }
            if (stats != null) {
                stats.updateCircuitState(modelName, to);
            }
            if (emitter != null && from != to) {
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                payload.put("modelName", modelName);
                payload.put("from", from.name());
                payload.put("to", to.name());
                if (to == CircuitState.OPEN) {
                    payload.put("consecutiveTrips", consecutiveTrips);
                    payload.put("openDurationMs", effectiveCooldownMs);
                }
                emitter.accept(new SessionEvent(EVENT_STATE_CHANGED, payload, Instant.now(clock)));
            }
        }

        /** 当前退避倍数（观测/指标用；CLOSED 复位后为 1）。 */
        synchronized long backoffMultiplier() {
            return config.backoffMultiplier(Math.max(1, consecutiveTrips));
        }

        private void resetWindow() {
            window = new boolean[config.windowSize()];
            samples = 0;
            failures = 0;
        }

        private Duration probeEscapeAfter() {
            return Duration.ofMillis(effectiveCooldownMs * 2);
        }

        /** 观测/测试：半开已连续成功探测数。 */
        synchronized int halfOpenSuccesses() {
            return halfOpenSuccesses;
        }

        private Duration elapsedSince(Instant since) {
            return Duration.between(since, Instant.now(clock));
        }
    }

    /** 前置闸裁决结果。 */
    private record Admission(boolean admitted, CircuitState state, long retryInMs) {
        static Admission admit() {
            return new Admission(true, null, 0);
        }

        static Admission reject(CircuitState state, long retryInMs) {
            return new Admission(false, state, retryInMs);
        }
    }
}
