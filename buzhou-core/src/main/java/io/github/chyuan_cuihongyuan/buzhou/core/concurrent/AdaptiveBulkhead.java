package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应并发闸（spec 145 / T499，TCP AIMD + K8s HPA 借鉴）：per-agent 动态上限——
 * 连续 N 次成功<b>加性</b> +1（封顶 maxLimit）；任一失败<b>积性</b>减半（地板 1）。
 * acquire 超动态上限 fail-fast（QUOTA_EXCEEDED——与 {@link AgentBulkhead} 同词汇）；
 * Lease AutoCloseable 归还。
 *
 * <p>与静态 {@link AgentBulkhead} 并存（替代装配位，不改既有类）；无等待语义
 * （fail-fast 信号质量优先）。喂数：recordSuccess/recordFailure 公共 API。
 */
public final class AdaptiveBulkhead {

    /** 配置：初始/上限/加性步进阈值（每 N 连续成功 +1）。 */
    public record Config(int initialLimit, int maxLimit, int successesPerStep) {
        public Config {
            if (initialLimit < 1 || maxLimit < initialLimit || successesPerStep < 1) {
                throw new IllegalArgumentException(
                        "自适应配置非法（initial>=1、max>=initial、step>=1）");
            }
        }

        public static Config defaults() {
            return new Config(4, 32, 8);
        }
    }

    /** 观测行：动态上限 / 在飞 / 拒绝 / 调整次数。 */
    public record View(int limit, int inFlight, long blocked, long adjustments) {
    }

    /** 一次并发占用（AutoCloseable 归还）。 */
    public final class Lease implements AutoCloseable {
        private final String agent;
        private volatile boolean closed;

        Lease(String agent) {
            this.agent = agent;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                release(agent);
            }
        }
    }

    private static final String ADJUSTED_COUNTER = "buzhou.adaptive-bulkhead.adjusted";

    private static final class AgentState {
        int limit;
        int inFlight;
        long blocked;
        long adjustments;
        int consecutiveSuccesses;

        AgentState(int limit) {
            this.limit = limit;
        }
    }

    private final Config config;
    private final Map<String, AgentState> agents = new ConcurrentHashMap<>();
    private final AtomicLong totalAdjustments = new AtomicLong();

    public AdaptiveBulkhead() {
        this(Config.defaults());
    }

    public AdaptiveBulkhead(Config config) {
        this.config = config == null ? Config.defaults() : config;
    }

    /** 占一个并发位：超动态上限抛 QUOTA_EXCEEDED（fail-fast）。 */
    public Lease acquire(String agentName) {
        AgentState state = agents.computeIfAbsent(agentName,
                k -> new AgentState(config.initialLimit()));
        synchronized (state) {
            if (state.inFlight >= state.limit) {
                state.blocked++;
                throw new BuzhouException(ErrorCode.QUOTA_EXCEEDED,
                        "自适应并发上限（agent=" + agentName + "，动态上限=" + state.limit
                                + "，在飞=" + state.inFlight + "）——fail-fast 不等待");
            }
            state.inFlight++;
            return new Lease(agentName);
        }
    }

    /** 记成功：连续 N 次 +1（封顶 max；任何失败清零连成功计数）。 */
    public void recordSuccess(String agentName) {
        AgentState state = agents.computeIfAbsent(agentName,
                k -> new AgentState(config.initialLimit()));
        synchronized (state) {
            state.consecutiveSuccesses++;
            if (state.consecutiveSuccesses >= config.successesPerStep()
                    && state.limit < config.maxLimit()) {
                state.limit++;
                state.consecutiveSuccesses = 0;
                state.adjustments++;
                totalAdjustments.incrementAndGet();
                BuzhouMetricsHolder.metrics().counter(ADJUSTED_COUNTER, 1);
            }
        }
    }

    /** 记失败：积性减半（地板 1）——错误信号代价高，果断退。 */
    public void recordFailure(String agentName) {
        AgentState state = agents.computeIfAbsent(agentName,
                k -> new AgentState(config.initialLimit()));
        synchronized (state) {
            state.consecutiveSuccesses = 0;
            int newLimit = Math.max(1, state.limit / 2);
            if (newLimit != state.limit) {
                state.limit = newLimit;
                state.adjustments++;
                totalAdjustments.incrementAndGet();
                BuzhouMetricsHolder.metrics().counter(ADJUSTED_COUNTER, 1);
            }
        }
    }

    /** 动态上限（未注册 = initial）。 */
    public int limitOf(String agentName) {
        AgentState state = agents.get(agentName);
        return state == null ? config.initialLimit() : viewOf(state).limit();
    }

    /** 观测快照（稳定序）。 */
    public Map<String, View> snapshot() {
        Map<String, View> out = new java.util.TreeMap<>();
        agents.forEach((name, state) -> out.put(name, viewOf(state)));
        return out;
    }

    /** 全 agent 调整总次数。 */
    public long totalAdjustments() {
        return totalAdjustments.get();
    }

    private void release(String agentName) {
        AgentState state = agents.get(agentName);
        if (state != null) {
            synchronized (state) {
                state.inFlight = Math.max(0, state.inFlight - 1);
            }
        }
    }

    private static View viewOf(AgentState state) {
        synchronized (state) {
            return new View(state.limit, state.inFlight, state.blocked, state.adjustments);
        }
    }
}
