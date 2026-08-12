package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;

public interface AgentRuntime {

    AgentSession spawn(String appId, String agentName);

    AgentSession spawn(String appId, String agentName, String sessionId);

    AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options);

    /**
     * 注册运行时级事件监听器（会话建立<b>前</b>的事件，如背压 spawn 闸排队 / 拒绝）。
     *
     * <p>与 {@link AgentSession#addEventListener(SessionEventListener)} 区别：后者只收会话建立后的事件；
     * 本方法收的是 spawn 闸在会话装配前发射的事件（{@code backpressure.spawn-queued} /
     * {@code backpressure.spawn-rejected}），以及 drain 早期事件。运行时级监听器收到的事件
     * <b>不携带 sessionId 归因</b>（会话尚未建立）。
     *
     * <p>默认 no-op，保证既有实现源码 / 二进制兼容（additive）。
     *
     * @param listener 事件监听器；{@code null} 忽略
     */
    default void addRuntimeEventListener(SessionEventListener listener) {
    }

    /**
     * 优雅停机（drain）：拒新 spawn、对在途会话按 drain 协议处置（等完当前轮次 / 超时强杀）后 close。
     *
     * <p>幂等：并发或重复调用只生效一次，后续调用得到同一结果（同一 {@link DrainResult}）。
     * drain 开始后 {@link #spawn} 立即抛 {@link RuntimeDrainingException}——拒绝即调用方的路由信号。
     *
     * <p>本方法为 additive default：未实现 drain 的运行时抛 {@link UnsupportedOperationException}；
     * {@code DefaultAgentRuntime} 已实现完整 drain 协议（spec「06 优雅停机」）。
     *
     * @param timeout drain 总预算（超时后对仍在轮次中的会话走取消传播强杀）；{@code null} 取保守默认
     * @return drain 结果（等完数 / 强杀数 / 总耗时）
     */
    default DrainResult drain(Duration timeout) {
        throw new UnsupportedOperationException(
                "drain not supported by this AgentRuntime: " + getClass().getName());
    }
}
