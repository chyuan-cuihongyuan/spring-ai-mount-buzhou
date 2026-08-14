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
     * impl-33 / spec 13 §core-3：带租约参数的编程式入口（Spring 装配等价物 =
     * {@code buzhou.lease-ttl} / {@code buzhou.lease-renew-interval}）。
     *
     * @param leaseTtl           租约 TTL；null = 默认 90s
     * @param leaseRenewInterval 后台续租间隔；null = TTL/3
     */
    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig config,
                                       java.time.Duration leaseTtl, java.time.Duration leaseRenewInterval,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), config,
                leaseTtl, leaseRenewInterval, tools);
    }

    /**
     * impl-30 / spec 13 §core-1：带租约 + 停机排空预算的编程式入口（Spring 装配等价物 =
     * {@code buzhou.lease-ttl} / {@code buzhou.lease-renew-interval} /
     * {@code buzhou.lifecycle.timeout-per-shutdown-phase}）。
     *
     * @param shutdownTimeout 停机排空预算；null = 默认 30s
     */
    public static AgentRuntime runtime(ChatModel chatModel, BuzhouStores stores,
                                       io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig config,
                                       java.time.Duration leaseTtl, java.time.Duration leaseRenewInterval,
                                       java.time.Duration shutdownTimeout,
                                       ToolCallback... tools) {
        return new DefaultAgentRuntime(chatModel, stores, new HarnessAssembler(), config,
                leaseTtl, leaseRenewInterval, shutdownTimeout, tools);
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder) {
        return enhance(builder, inMemoryStores());
    }

    public static ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        return new HarnessAssembler().enhance(builder, stores);
    }
}
