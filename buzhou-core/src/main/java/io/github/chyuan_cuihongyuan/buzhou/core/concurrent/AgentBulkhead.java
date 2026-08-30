package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 每 agent 并发 Turn 隔离舱（spec 84 §A / T323，resilience4j Bulkhead 借鉴；
 * 全局旋钮模式——BuzhouMetricsHolder/Netty ResourceLeakDetector 先例）：
 * spawn 闸（SpawnGate）限<b>会话数</b>，本舱限<b>同时在飞的模型 Turn 数</b>——
 * 热点 agent 不能吃光全实例模型吞吐。未配置上限的 agent = NOOP 舱（零开销，
 * 默认全不限——零行为变化）。
 *
 * <p>拒绝语义：达到上限 → {@code acquire-timeout}（默认 0 = fail-fast）内等空位，
 * 仍无则抛 {@link BuzhouException}（QUOTA_EXCEEDED——NON_RETRYABLE，调用方按容量
 * 拒绝分流：重试/路由/降载）。计数器 {@code buzhou.bulkhead.rejected}。
 */
public final class AgentBulkhead {

    private static final AtomicReference<AgentBulkhead> GLOBAL =
            new AtomicReference<>(unlimited());

    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final Map<String, Integer> limits;
    private final Duration acquireTimeout;

    private AgentBulkhead(Map<String, Integer> limits, Duration acquireTimeout) {
        this.limits = Map.copyOf(limits);
        this.acquireTimeout = acquireTimeout == null ? Duration.ZERO : acquireTimeout;
        limits.forEach((agent, limit) ->
                semaphores.put(agent, new Semaphore(Math.max(1, limit), true)));
    }

    /** 按上限表构造（agent → 并发 Turn 上限；acquireTimeout null/0 = fail-fast）。 */
    public static AgentBulkhead of(Map<String, Integer> perAgentLimits, Duration acquireTimeout) {
        return new AgentBulkhead(perAgentLimits == null ? Map.of() : perAgentLimits,
                acquireTimeout);
    }

    /** 不限（默认全局实例——未配置时所有 agent 零开销直通）。 */
    public static AgentBulkhead unlimited() {
        return new AgentBulkhead(Map.of(), Duration.ZERO);
    }

    public static AgentBulkhead global() {
        return GLOBAL.get();
    }

    /** 装配/测试替换（null = 回到不限；@AfterEach 清理纪律）。 */
    public static void install(AgentBulkhead bulkhead) {
        GLOBAL.set(bulkhead == null ? unlimited() : bulkhead);
    }

    /** 该 agent 配置的并发上限（未配置 = Integer.MAX_VALUE 语义）。 */
    public int limitOf(String agentName) {
        Integer limit = limits.get(agentName);
        return limit == null ? Integer.MAX_VALUE : limit;
    }

    /** 该 agent 当前在飞 Turn 数（观测面）。 */
    public int inFlight(String agentName) {
        Semaphore semaphore = semaphores.get(agentName);
        return semaphore == null ? 0 : limitOf(agentName) - semaphore.availablePermits();
    }

    /** 已配置上限的 agent 视图（agent → limit，只读；健康面/看板用——spec 92 §A）。 */
    public Map<String, Integer> configuredAgents() {
        return Map.copyOf(limits);
    }

    /**
     * 取一个 Turn 名额（未配置上限 = NOOP 舱零开销）；超限按 acquire-timeout 等待，
     * 仍无空位抛 QUOTA_EXCEEDED。返回 lease（try-with-resources 释放）。
     */
    public Lease acquire(String agentName) {
        Semaphore semaphore = semaphores.get(agentName);
        if (semaphore == null) {
            return Lease.NOOP;
        }
        boolean acquired;
        try {
            acquired = acquireTimeout.isZero() || acquireTimeout.isNegative()
                    ? semaphore.tryAcquire()
                    : semaphore.tryAcquire(Math.max(1, acquireTimeout.toMillis()),
                            TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquired = semaphore.tryAcquire();
        }
        if (!acquired) {
            BuzhouMetricsHolder.metrics().counter("buzhou.bulkhead.rejected");
            recordRejection(agentName);
            throw new BuzhouException(ErrorCode.QUOTA_EXCEEDED,
                    "agent 并发 Turn 上限已到：agent=" + agentName
                            + "，limit=" + limitOf(agentName)
                            + "（修法：调大 buzhou.bulkhead.agents.<agent> 或错峰）");
        }
        return new Lease(semaphore);
    }

    /** per-agent 拒绝计数表（spec 117 §A / T415；256 封顶折 __overflow__）。 */
    private final java.util.concurrent.ConcurrentHashMap<String, AtomicLong> rejections =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_TRACKED_AGENTS = 256;
    private static final String OVERFLOW_MARKER = "__overflow__";

    private void recordRejection(String agent) {
        AtomicLong counter = rejections.get(agent);
        if (counter != null) {
            counter.incrementAndGet();
            return;
        }
        if (rejections.size() >= MAX_TRACKED_AGENTS) {
            rejections.computeIfAbsent(OVERFLOW_MARKER, k -> new AtomicLong())
                    .incrementAndGet();
            return;
        }
        rejections.computeIfAbsent(agent, k -> new AtomicLong()).incrementAndGet();
    }

    /** top-N 被拒 agent（count 降序、同 count 字典序——输出稳定；含 overflow 折叠行）。 */
    public List<Map.Entry<String, Long>> topRejections(int n) {
        return rejections.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                .toList();
    }

    /** Turn 名额（close 释放；NOOP 单例零开销）。 */
    public static final class Lease implements AutoCloseable {

        static final Lease NOOP = new Lease(null);

        private final Semaphore semaphore;

        private Lease(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (semaphore != null) {
                semaphore.release();
            }
        }
    }
}
