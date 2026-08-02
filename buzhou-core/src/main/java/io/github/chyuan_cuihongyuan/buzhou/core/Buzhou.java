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
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(),
                List.of(), Set.of(), tools);
    }

    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       List<BuzhouHook> hooks, Set<String> disabledHookNames,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(),
                hooks, disabledHookNames, tools);
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder) {
        return enhance(builder, inMemoryStores());
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        return new HarnessAssembler().enhance(builder, stores);
    }
}
