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

    /**
     * 会话导出（spec 28 / T107 / impl-82）：messages + 最新 Summary + State 打包为可移植
     * 文档（{@link SessionExport#toJson()} JSON 跨环境）。spill 证据不内嵌（引用随消息
     * metadata 导出，内容走 spill 侧运维导出——runbook）；空会话拒绝。
     */
    default SessionExport exportSession(String sessionId) {
        throw new UnsupportedOperationException("本 AgentRuntime 实现不支持会话导出");
    }

    /**
     * 会话导入（spec 28）：默认新 sessionId 重映射（跨环境 Id 撞车防护；引用一致重写）；
     * {@code keepIds=true} 保留原 Id 且目标已存在消息时 fail-fast
     * （{@link SessionImportException}）。导入是数据恢复——不建立租约/活跃会话，
     * 后续以该 Id spawn 续用。返回导入后的 sessionId。
     */
    default String importSession(SessionExport export, boolean keepIds) {
        throw new UnsupportedOperationException("本 AgentRuntime 实现不支持会话导入");
    }
}
