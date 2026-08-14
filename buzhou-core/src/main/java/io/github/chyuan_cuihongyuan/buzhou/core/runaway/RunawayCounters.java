package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 失控检测的内存计数器（单例，按 sessionId 隔离）。
 *
 * <p>轮次级计数（步数 / 工具调用数 / wall-clock 起点 / 按工具计数 / 重复指纹）单进程内存，
 * {@code beforeTurn} 重置。会话级累计计数 <b>不</b> 在此——那些走 {@code SessionStateStore}
 * 持久化（跨崩溃保留，见 {@code RunawayHook}），与 {@code recovery.autoresume.attempts} 同先例。
 *
 * <p>{@link RunawayHook}（写）与 {@link RunawayBudgetRenderer}（读）共享同一实例：注入视图在
 * memory advisor(+400) 构建、步数在 hook(+600) {@code beforeModel} 递增——故 renderer 本步读到的是
 * 「上一步末」计数（一步滞后，可接受；模型看到「进入本次调用时的预算」）。
 *
 * <p>线程安全：单轮内模型调用串行（步数无并发），工具调用可经扇出并行（工具计数用原子操作）。
 */
public final class RunawayCounters {

    private final ConcurrentHashMap<String, TurnState> states = new ConcurrentHashMap<>();

    /** 会话级计数 RMW 同步锁（按 sessionId 隔离，防并行工具扇出下 read-modify-write 竞争）。 */
    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    Object sessionLock(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    /** 取或建本会话的轮次状态（renderer / hook 内部用）。 */
    TurnState turnState(String sessionId) {
        return states.computeIfAbsent(sessionId, k -> new TurnState());
    }

    /** {@code beforeTurn} 重置轮次级计数并记录轮次起点（wall-clock 基准）。 */
    public void resetTurn(String sessionId) {
        turnState(sessionId).reset();
    }

    /** 会话销毁时清理（可选，避免无界增长）。 */
    public void remove(String sessionId) {
        states.remove(sessionId);
    }

    /** 本轮已用步数（renderer 计算剩余预算用）。 */
    public int steps(String sessionId) {
        TurnState s = states.get(sessionId);
        return s == null ? 0 : s.steps.get();
    }

    /** 本轮 wall-clock 起点（renderer / 测试观测用）。 */
    public Instant turnStart(String sessionId) {
        TurnState s = states.get(sessionId);
        return s == null ? null : s.turnStart;
    }

    /**
     * 单会话轮次级可变状态。
     *
     * <p>步数 / 工具调用数 / 按工具计数用原子类型（工具扇出可并行）；wall-clock 起点、软阈值已发标记
     * 用 volatile（beforeTurn 单写、beforeModel/renderer 多读）；指纹环缓冲在 beforeTool 串行写入段保护。
     */
    static final class TurnState {
        final AtomicInteger steps = new AtomicInteger();
        final AtomicInteger toolCalls = new AtomicInteger();
        final ConcurrentHashMap<String, AtomicInteger> perTool = new ConcurrentHashMap<>();
        volatile Instant turnStart;
        final Deque<String> fingerprintRing = new ArrayDeque<>();
        volatile boolean softThresholdEmitted;

        void reset() {
            steps.set(0);
            toolCalls.set(0);
            perTool.clear();
            turnStart = Instant.now();
            fingerprintRing.clear();
            softThresholdEmitted = false;
        }
    }
}
