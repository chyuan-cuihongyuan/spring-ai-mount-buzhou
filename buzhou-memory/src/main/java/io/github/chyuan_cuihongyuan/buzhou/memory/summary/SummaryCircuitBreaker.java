package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 摘要熔断器（spec 01 / spec 13 §stores-7 ticket 32 修复）：连续失败达阈值后开闸
 * （跳过摘要生成、走无摘要降级视图），但不再<b>永久</b>熔断——
 * <ul>
 *   <li><b>半开试探</b>：失败窗口（{@code failureWindow}，默认 PT10M）过后放行一次试探
 *       （同一时刻只放行一个，探针在途时其余请求仍拒绝）；</li>
 *   <li><b>成功清零</b>：试探成功 → 计数清零回到闭合态；试探失败 → 失败重计并重新关窗；</li>
 *   <li><b>计数随会话清理</b>：{@link #removeSession(String)} 提供会话级清理入口
 *       （供会话关闭级联清理挂接；core 的 SessionCleaner 协调器落地前，长期闲置条目由
 *       窗口过期的惰性淘汰兜底——见 {@link #allows(String)} 内的清扫）。</li>
 * </ul>
 * 阈值与窗口均可配（构造参数 / {@code buzhou.memory.summary-circuit-breaker.*}）。
 */
public class SummaryCircuitBreaker {

    /** 默认连续失败阈值（原硬编码 3 抽为常量； {@code memory.summary-circuit-breaker.failure-threshold}）。 */
    public static final int DEFAULT_FAILURE_THRESHOLD = 3;

    /** 默认失败恢复窗口（spec 13 §stores-7：半开试探前的等待时长）。 */
    public static final Duration DEFAULT_FAILURE_WINDOW = Duration.ofMinutes(10);

    private final int failureThreshold;
    private final Duration failureWindow;
    private final Clock clock;

    /** 每会话熔断状态（failures / 最近失败时刻 / 半开探针在途标记）。 */
    private final Map<String, SessionBreakerState> statesBySession = new ConcurrentHashMap<>();

    public SummaryCircuitBreaker(int failureThreshold) {
        this(failureThreshold, DEFAULT_FAILURE_WINDOW);
    }

    public SummaryCircuitBreaker(int failureThreshold, Duration failureWindow) {
        this(failureThreshold, failureWindow, Clock.systemUTC());
    }

    /** 全参构造（测试可注时钟驱动窗口流逝）。 */
    public SummaryCircuitBreaker(int failureThreshold, Duration failureWindow, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.failureWindow = failureWindow == null || failureWindow.isNegative()
                ? DEFAULT_FAILURE_WINDOW : failureWindow;
        this.clock = clock;
    }

    /**
     * 该会话当前是否放行摘要生成：闭合态放行；开闸后窗口未过拒绝；
     * 窗口过后放行<b>一次</b>试探（半开，探针在途时后续请求仍拒绝）。
     */
    public boolean allows(String sessionId) {
        evictStaleEntries();
        // 无失败记录的会话直接放行（不建状态条目，避免计数 map 随会话数无界增长）
        SessionBreakerState state = statesBySession.get(sessionId);
        if (state == null || state.failures.get() < failureThreshold) {
            return true;
        }
        // 开闸态：窗口过后半开放行一次试探（CAS 保证同一时刻仅一个探针）
        if (Instant.now(clock).isAfter(state.lastFailureAt.plus(failureWindow))) {
            return state.probeInFlight.compareAndSet(false, true);
        }
        return false;
    }

    /** 成功（含半开试探成功）：计数清零回到闭合态。 */
    public void onSuccess(String sessionId) {
        statesBySession.remove(sessionId);
    }

    /** 失败：计数累加、刷新失败时刻（重新关窗）；半开探针在途标记复位。 */
    public void onFailure(String sessionId) {
        statesBySession.computeIfAbsent(sessionId, k -> new SessionBreakerState()).recordFailure(
                Instant.now(clock));
    }

    /**
     * 会话级清理入口（spec 13 §stores-7「计数随会话清理」）：会话关闭时由清理协调器调用，
     * 防止计数 map 随会话数无界增长。
     */
    public void removeSession(String sessionId) {
        statesBySession.remove(sessionId);
    }

    /** 当前跟踪的会话数（测试 / 运维可观测）。 */
    public int trackedSessions() {
        return statesBySession.size();
    }

    /** 惰性淘汰扫描间隔：每 N 次 allows 触发一次全表清扫，摊薄热路径成本（包内可见供测试引用）。 */
    static final int EVICT_SCAN_INTERVAL = 1024;

    private final java.util.concurrent.atomic.AtomicInteger callCounter = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * 惰性淘汰：长期闲置（失败后窗口早已过期且无探针在途）的条目周期性清扫，
     * 防计数 map 随会话无界增长；顺带使「陈旧的部分失败计数」过期
     * （近似滑动窗语义——远古失败不再累积到开闸判定）。
     */
    private void evictStaleEntries() {
        if (callCounter.incrementAndGet() % EVICT_SCAN_INTERVAL != 0 || statesBySession.isEmpty()) {
            return;
        }
        Instant now = Instant.now(clock);
        statesBySession.entrySet().removeIf(entry -> {
            SessionBreakerState state = entry.getValue();
            return state.failures.get() > 0 && !state.probeInFlight.get()
                    && now.isAfter(state.lastFailureAt.plus(failureWindow));
        });
    }

    /** 单会话熔断状态：失败计数 + 最近失败时刻 + 半开探针标记。 */
    private static final class SessionBreakerState {

        final AtomicInteger failures = new AtomicInteger();
        final AtomicBoolean probeInFlight = new AtomicBoolean(false);
        volatile Instant lastFailureAt = Instant.EPOCH;

        void recordFailure(Instant at) {
            failures.incrementAndGet();
            lastFailureAt = at;
            probeInFlight.set(false);
        }
    }
}
