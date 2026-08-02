package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookedToolCallback;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouChatMemory;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouMemoryAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class HarnessAssembler {

    public AgentSession assemble(String appId, String agentName, String sessionId,
                                 ChatModel chatModel, BuzhouStores stores,
                                 SessionResourceRegistry registry,
                                 Runnable onClose,
                                 Collection<BuzhouHook> hooks,
                                 Set<String> disabledHookNames,
                                 ToolCallback... tools) {
        HookEnvironment env = new HookEnvironment(sessionId, stores.sessionStateStore());
        HookChain chain = new HookChain(hooks, disabledHookNames);
        ToolCallback[] hookedTools = hookTools(tools, chain, env);

        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        ToolCallingAdvisor.builder().build(),
                        new BuzhouMemoryAdvisor(memory),
                        new HookAdvisor(chain, env));
        if (hookedTools.length > 0) {
            builder.defaultToolCallbacks(Arrays.asList(hookedTools));
        }
        return new DefaultAgentSession(appId, agentName, sessionId, builder.build(), registry, onClose,
                chain, env);
    }

    public ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        return builder.defaultAdvisors(
                ToolCallingAdvisor.builder().build(),
                new BuzhouMemoryAdvisor(memory));
    }

    private ToolCallback[] hookTools(ToolCallback[] tools, HookChain chain, HookEnvironment env) {
        if (tools == null) {
            return new ToolCallback[0];
        }
        return Arrays.stream(tools)
                .map(tool -> (ToolCallback) new HookedToolCallback(tool, chain, env))
                .toArray(ToolCallback[]::new);
    }
}
