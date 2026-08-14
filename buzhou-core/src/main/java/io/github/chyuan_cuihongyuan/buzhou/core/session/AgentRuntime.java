package io.github.chyuan_cuihongyuan.buzhou.core.session;

public interface AgentRuntime {

    AgentSession spawn(String appId, String agentName);

    AgentSession spawn(String appId, String agentName, String sessionId);

    AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options);

    /**
     * 会话 fork（spec 20 / T88 / impl-63）：从源会话最后消息复制历史到新会话（Message 全量 +
     * Summary 最新一版；SessionState 不复制——预算重置 = 重试/探索语义）。走完整 spawn 管线。
     * 默认抛 UnsupportedOperationException（实现按需提供）。
     */
    default AgentSession fork(String sourceSessionId, String appId, String agentName, String newSessionId) {
        throw new UnsupportedOperationException("本 AgentRuntime 实现不支持会话 fork");
    }
}
