package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookedToolCallback;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouChatMemory;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouMemoryAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DanglingCallRepairer;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class HarnessAssembler {

    public AgentSession assemble(String appId, String agentName, String sessionId,
                                 ChatModel chatModel, BuzhouStores stores,
                                 SessionResourceRegistry registry,
                                 Runnable onClose,
                                 Collection<BuzhouHook> hooks,
                                 Set<String> disabledHookNames,
                                 Set<String> idempotentToolNames,
                                 MemoryViewProcessor viewProcessor,
                                 ExecutorService executor,
                                 Map<String, String> serialGroups,
                                 ToolCallback... tools) {
        return assemble(appId, agentName, sessionId, chatModel, stores, registry, onClose,
                hooks, disabledHookNames, idempotentToolNames, viewProcessor, executor, serialGroups,
                List.of(), tools);
    }

    public AgentSession assemble(String appId, String agentName, String sessionId,
                                 ChatModel chatModel, BuzhouStores stores,
                                 SessionResourceRegistry registry,
                                 Runnable onClose,
                                 Collection<BuzhouHook> hooks,
                                 Set<String> disabledHookNames,
                                 Set<String> idempotentToolNames,
                                 MemoryViewProcessor viewProcessor,
                                 ExecutorService executor,
                                 Map<String, String> serialGroups,
                                 List<SessionAssemblyCustomizer> assemblyCustomizers,
                                 ToolCallback... tools) {
        HookEnvironment env = new HookEnvironment(sessionId, agentName, stores.sessionStateStore());
        HookChain chain = new HookChain(hooks, disabledHookNames);

        SpanContextCarrier spanContextCarrier = new SpanContextCarrier();
        // 事件发射器复用 HookEnvironment 的延迟绑定发布者：装配期为 no-op，
        // DefaultAgentSession 构造后绑到 dispatchEvent——机制模块 advisor 注入的事件进会话既有通道。
        DefaultSessionAssemblyContext assemblyCtx = new DefaultSessionAssemblyContext(
                appId, agentName, sessionId, stores, registry, spanContextCarrier, env::emit);
        assemblyCtx.wrapToolCallbacks(t -> (ToolCallback) new HookedToolCallback(t, chain, env));
        // 机制模块（buzhou-observability）经 customizer 注入 advisor + 工具包装 + observer
        if (assemblyCustomizers != null) {
            assemblyCustomizers.forEach(c -> c.customize(assemblyCtx));
        }

        // 把已注册工具包装层叠应用到全部工具（autoTools + 传入 tools + customizer 注入的 extraTools）
        List<ToolCallback> allToolCallbacks = new ArrayList<>();
        if (tools != null) {
            allToolCallbacks.addAll(Arrays.asList(tools));
        }
        // MCP 等动态工具集经 SessionAssemblyCustomizer.addToolCallbacks 注入（spec 04 / ticket 22）
        allToolCallbacks.addAll(assemblyCtx.extraTools());
        List<ToolCallback> wrapped = applyWrappers(allToolCallbacks, assemblyCtx.toolWrappers());
        ToolCallback[] allTools = wrapped.toArray(new ToolCallback[0]);

        HarnessToolCallingManager toolManager = new HarnessToolCallingManager(
                org.springframework.ai.model.tool.DefaultToolCallingManager.builder().build(),
                executor, 8, Duration.ofSeconds(60), serialGroups, spanContextCarrier, sessionId);
        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        memory.setViewProcessor(viewProcessor);
        memory.setRepairer(new DanglingCallRepairer(
                toolsByName(allTools), idempotentToolNames,
                (sid, event) -> env.emit(new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                        "dangling.repaired",
                        Map.of("messageId", event.messageId(),
                                "toolCalls", event.danglingToolCalls(),
                                "action", event.action()),
                        Instant.now()))));

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(ToolCallingAdvisor.builder().toolCallingManager(toolManager).build());
        advisors.add(new BuzhouMemoryAdvisor(memory));
        advisors.add(new HookAdvisor(chain, env));
        // 机制模块注入的 advisor（如 ObservabilityAdvisor）
        advisors.addAll(assemblyCtx.advisors());
        builder.defaultAdvisors(advisors);
        if (allTools.length > 0) {
            builder.defaultToolCallbacks(Arrays.asList(allTools));
        }
        return new DefaultAgentSession(appId, agentName, sessionId, builder.build(), registry, onClose,
                chain, env, toolManager, spanContextCarrier, assemblyCtx.observers());
    }

    public ChatClient.Builder enhance(ChatClient.Builder builder, BuzhouStores stores) {
        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        return builder.defaultAdvisors(
                ToolCallingAdvisor.builder().build(),
                new BuzhouMemoryAdvisor(memory));
    }

    private Map<String, ToolCallback> toolsByName(ToolCallback[] tools) {
        Map<String, ToolCallback> map = new java.util.HashMap<>();
        if (tools != null) {
            for (ToolCallback tool : tools) {
                map.put(tool.getToolDefinition().name(), tool);
            }
        }
        return map;
    }

    /** 按注册顺序层叠应用包装函数：第一个最内层（先包工具本身），最后一个最外层。 */
    private List<ToolCallback> applyWrappers(List<ToolCallback> tools,
                                             List<java.util.function.UnaryOperator<ToolCallback>> wrappers) {
        List<ToolCallback> current = new ArrayList<>(tools);
        for (java.util.function.UnaryOperator<ToolCallback> wrapper : wrappers) {
            List<ToolCallback> next = new ArrayList<>(current.size());
            for (ToolCallback tool : current) {
                // 跳过已被 hook 包装的工具的二次 hook（避免双重 HookedToolCallback）
                next.add(wrapper.apply(tool));
            }
            current = next;
        }
        return current;
    }
}
