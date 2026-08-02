package io.github.chyuan_cuihongyuan.buzhou.core.session;

public interface AgentRuntime {

    AgentSession spawn(String appId, String agentName);

    AgentSession spawn(String appId, String agentName, String sessionId);

    AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options);
}
