package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DefaultAgentRuntime implements AgentRuntime {

    private static final Duration LEASE_TTL = Duration.ofSeconds(90);

    private final ChatModel chatModel;
    private final BuzhouStores stores;
    private final HarnessAssembler assembler;
    private final ToolCallback[] tools;
    private final RuntimeConfig config;
    private final String ownerId = UUID.randomUUID().toString();

    public DefaultAgentRuntime(ChatModel chatModel, BuzhouStores stores,
                               HarnessAssembler assembler, RuntimeConfig config,
                               ToolCallback... tools) {
        this.chatModel = chatModel;
        this.stores = stores;
        this.assembler = assembler;
        this.config = config == null ? RuntimeConfig.defaults() : config;
        this.tools = tools == null ? new ToolCallback[0] : tools.clone();
    }

    @Override
    public AgentSession spawn(String appId, String agentName) {
        return spawn(appId, agentName, UUID.randomUUID().toString(), SpawnOptions.defaults());
    }

    @Override
    public AgentSession spawn(String appId, String agentName, String sessionId) {
        return spawn(appId, agentName, sessionId, SpawnOptions.defaults());
    }

    @Override
    public AgentSession spawn(String appId, String agentName, String sessionId, SpawnOptions options) {
        LeaseAcquireResult lease = stores.sessionLeaseStore().tryAcquire(sessionId, ownerId, LEASE_TTL);
        if (!lease.acquired()) {
            if (!options.steal()) {
                throw new SessionAlreadyActiveException(sessionId);
            }
            lease = stores.sessionLeaseStore().steal(sessionId, ownerId, LEASE_TTL);
        }
        SessionResourceRegistry registry = new SessionResourceRegistry();
        long fencingToken = lease.fencingToken();
        registry.register("session-lease",
                () -> stores.sessionLeaseStore().release(sessionId, ownerId, fencingToken));
        java.util.concurrent.ExecutorService executor =
                java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        registry.register("session-executor", executor::shutdownNow);
        config.sessionCustomizers().forEach(c -> c.customize(registry, appId, agentName, sessionId));
        ToolCallback[] allTools = java.util.stream.Stream.concat(
                        java.util.Arrays.stream(tools),
                        config.autoTools().stream())
                .toArray(ToolCallback[]::new);
        AgentSession session = assembler.assemble(appId, agentName, sessionId, chatModel, stores, registry,
                registry::closeAll, config.hooks(), config.disabledHookNames(),
                config.idempotentToolNames(), config.viewProcessor(), executor,
                config.serialGroups(), allTools);
        options.listeners().forEach(session::addEventListener);
        return session;
    }
}
