package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;

public interface AgentRuntime {

    AgentSession spawn(String appId, String agentName);

    AgentSession spawn(String appId, String agentName, String sessionId);

    AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options);

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
