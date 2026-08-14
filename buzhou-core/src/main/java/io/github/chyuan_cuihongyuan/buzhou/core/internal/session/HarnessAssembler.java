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
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
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

    /** impl-28：单 Turn 并发许可默认值（既有硬编码收敛为命名常量；后续切片可提升为可配）。 */
    private static final int DEFAULT_MAX_CONCURRENCY_PER_TURN = 8;
    /** impl-28：单工具派发默认超时（既有硬编码收敛为命名常量；与 Deadline 取 min 后生效）。 */
    private static final Duration DEFAULT_TOOL_TIMEOUT = Duration.ofSeconds(60);

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
                List.of(), null, tools);
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
                                 TurnLoopPolicy turnLoopPolicy,
                                 ToolCallback... tools) {
        return assemble(appId, agentName, sessionId, chatModel, stores, registry, onClose,
                hooks, disabledHookNames, idempotentToolNames, viewProcessor, executor, serialGroups,
                assemblyCustomizers, turnLoopPolicy, null, tools);
    }

    /**
     * impl-33 / spec 13 §core-3：完整装配入口——{@code leaseGuard} 贯穿两处：
     * {@link BoundedToolCallingAdvisor}（Turn 轮间续租 + fence，在飞工具结果落库前截断）与
     * {@link DefaultAgentSession}（Turn 提交点 fence + LeaseLost 中止语义）。null = 无租约语义
     * （{@code Buzhou.enhance} 等非会话路径既有行为不变）。
     */
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
                                 TurnLoopPolicy turnLoopPolicy,
                                 SessionLeaseGuard leaseGuard,
                                 ToolCallback... tools) {
        return assemble(appId, agentName, sessionId, chatModel, stores, registry, onClose,
                hooks, disabledHookNames, idempotentToolNames, viewProcessor, executor, serialGroups,
                assemblyCustomizers, turnLoopPolicy, leaseGuard, null, tools);
    }

    /**
     * impl-34 / spec 13 §core-4：完整装配入口——{@code eventDispatchConfig}（null = SYNC
     * 既有内联分发）流入会话层；BUFFERED 时事件经有界队列异步分发。
     */
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
                                 TurnLoopPolicy turnLoopPolicy,
                                 SessionLeaseGuard leaseGuard,
                                 io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig,
                                 ToolCallback... tools) {
        return assemble(appId, agentName, sessionId, chatModel, stores, registry, onClose,
                hooks, disabledHookNames, idempotentToolNames, viewProcessor, executor, serialGroups,
                assemblyCustomizers, turnLoopPolicy, leaseGuard, eventDispatchConfig, null, tools);
    }

    /**
     * impl-35 / spec 13 §stores-6：完整装配入口 + 级联清理协调器——
     * {@code sessionCleaner} 非 null 时会话 {@code delete()} 先 close 再一次级联删存储；
     * null 时退化为 close()（既有装配路径行为不变）。
     */
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
                                 TurnLoopPolicy turnLoopPolicy,
                                 SessionLeaseGuard leaseGuard,
                                 io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig eventDispatchConfig,
                                 io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner sessionCleaner,
                                 ToolCallback... tools) {
        HookEnvironment env = new HookEnvironment(sessionId, agentName, stores.sessionStateStore());
        HookChain chain = new HookChain(hooks, disabledHookNames);

        SpanContextCarrier spanContextCarrier = new SpanContextCarrier();
        // impl-06/07：manager 先于 customizer 构造——恢复/审计类机制模块经装配上下文挂接事件日志
        HarnessToolCallingManager toolManager = new HarnessToolCallingManager(
                org.springframework.ai.model.tool.DefaultToolCallingManager.builder().build(),
                executor, DEFAULT_MAX_CONCURRENCY_PER_TURN, DEFAULT_TOOL_TIMEOUT,
                serialGroups, spanContextCarrier, sessionId);
        DefaultSessionAssemblyContext assemblyCtx = new DefaultSessionAssemblyContext(
                appId, agentName, sessionId, stores, registry, spanContextCarrier, toolManager);
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

        BuzhouChatMemory memory = new BuzhouChatMemory(stores.messageStore());
        memory.setViewProcessor(viewProcessor);
        // impl-07：悬空修复优先回放事件日志中已落盘的 COMPLETED 结局（exactly-once，不重跑工具）
        memory.setRepairer(new DanglingCallRepairer(
                toolsByName(allTools), idempotentToolNames, toolManager.toolCallLog(),
                (sid, event) -> env.emit(new io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent(
                        "dangling.repaired",
                        Map.of("messageId", event.messageId(),
                                "toolCalls", event.danglingToolCalls(),
                                "action", event.action()),
                        Instant.now()))));

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        List<Advisor> advisors = new ArrayList<>();
        // 有界 Turn（T17）：默认策略给 think→tool 递归上硬上界；policy=null 时用框架默认（40 轮）
        advisors.add(new BoundedToolCallingAdvisor(toolManager, turnLoopPolicy, sessionId, agentName,
                event -> env.emit(event), leaseGuard));
        // impl-33：写路径 fence——history 每次落库前校验 fencingToken（leaseGuard 为 null 时为
        // 无租约路径 no-op）；方法引用而非直接传 guard，避免 memory 包反向依赖 session 包内部类
        advisors.add(new BuzhouMemoryAdvisor(memory,
                leaseGuard == null ? null : leaseGuard::checkFence));
        advisors.add(new HookAdvisor(chain, env));
        // 机制模块注入的 advisor（如 ObservabilityAdvisor）
        advisors.addAll(assemblyCtx.advisors());
        builder.defaultAdvisors(advisors);
        if (allTools.length > 0) {
            builder.defaultToolCallbacks(Arrays.asList(allTools));
        }
        // impl-28 / spec 13 §core-2：有效 Turn 预算（min(turnDeadline, loopTimeout)）交给会话层
        // 做模型调用兜底；未配置为 null（既有直调不设限行为）
        Duration turnBudget = turnLoopPolicy == null ? null : turnLoopPolicy.effectiveTurnBudget();
        return new DefaultAgentSession(appId, agentName, sessionId, builder.build(), registry, onClose,
                chain, env, toolManager, spanContextCarrier, assemblyCtx.observers(), turnBudget,
                leaseGuard, eventDispatchConfig, sessionCleaner);
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
