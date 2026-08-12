package io.github.chyuan_cuihongyuan.buzhou.core;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryUnitOfWork;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;

import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

public final class Buzhou {

    private Buzhou() {
    }

    public static BuzhouStores inMemoryStores() {
        return new BuzhouStores(
                new InMemoryMessageStore(),
                new InMemorySummaryStore(),
                new InMemorySessionStateStore(),
                new InMemorySessionLeaseStore(),
                new InMemoryObservabilityStore(),
                new InMemoryUnitOfWork());
    }

    public static AgentRuntime runtime(ChatModel chatModel, ToolCallback... tools) {
        return runtime(chatModel, inMemoryStores(), tools);
    }

    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores, ToolCallback... tools) {
        return runtime(chatModel, stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(), tools);
    }

    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig config,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), config, tools);
    }

    /**
     * 带崩溃恢复配置的 runtime（spec「崩溃中轮次恢复」）。
     *
     * @param recoveryConfig 恢复/持久化/幂等参数；{@code null} 等效关闭机制
     */
    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig config,
                                       io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig recoveryConfig,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), config, recoveryConfig, tools);
    }

    /**
     * 带背压配置的 runtime（spec「背压与多层限流」）。
     *
     * @param backpressureProperties 背压参数（spawn 闸 + 工具扇出闸）；{@code null} 等效不限
     */
    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig config,
                                       io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig recoveryConfig,
                                       io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouBackpressureProperties backpressureProperties,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), config, recoveryConfig,
                backpressureProperties, tools);
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder) {
        return enhance(builder, inMemoryStores());
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        return new HarnessAssembler().enhance(builder, stores);
    }
}
