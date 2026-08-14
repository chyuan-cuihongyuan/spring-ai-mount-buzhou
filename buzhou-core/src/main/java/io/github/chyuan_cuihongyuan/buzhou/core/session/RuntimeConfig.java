package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RuntimeConfig(
        List<BuzhouHook> hooks,
        Set<String> disabledHookNames,
        Set<String> idempotentToolNames,
        MemoryViewProcessor viewProcessor,
        List<ToolCallback> autoTools,
        Map<String, String> serialGroups,
        List<SessionResourceCustomizer> sessionCustomizers,
        List<SessionAssemblyCustomizer> assemblyCustomizers,
        TurnLoopPolicy turnLoopPolicy,
        List<SessionCleanupContributor> sessionCleanupContributors) {

    public RuntimeConfig {
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
        disabledHookNames = disabledHookNames == null ? Set.of() : Set.copyOf(disabledHookNames);
        idempotentToolNames = idempotentToolNames == null ? Set.of() : Set.copyOf(idempotentToolNames);
        autoTools = autoTools == null ? List.of() : List.copyOf(autoTools);
        serialGroups = serialGroups == null ? Map.of() : Map.copyOf(serialGroups);
        sessionCustomizers = sessionCustomizers == null ? List.of() : List.copyOf(sessionCustomizers);
        assemblyCustomizers = assemblyCustomizers == null ? List.of() : List.copyOf(assemblyCustomizers);
        sessionCleanupContributors = sessionCleanupContributors == null
                ? List.of() : List.copyOf(sessionCleanupContributors);
    }

    /** impl-35 前的 9 槽形状（无清理贡献者）——保留委托，源 / 二进制兼容。 */
    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools, Map<String, String> serialGroups,
                         List<SessionResourceCustomizer> sessionCustomizers,
                         List<SessionAssemblyCustomizer> assemblyCustomizers,
                         TurnLoopPolicy turnLoopPolicy) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, serialGroups,
                sessionCustomizers, assemblyCustomizers, turnLoopPolicy, List.of());
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, Map.of(),
                List.of(), List.of(), null, List.of());
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools, Map<String, String> serialGroups,
                         List<SessionResourceCustomizer> sessionCustomizers) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, serialGroups,
                sessionCustomizers, List.of(), null, List.of());
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools, Map<String, String> serialGroups,
                         List<SessionResourceCustomizer> sessionCustomizers,
                         List<SessionAssemblyCustomizer> assemblyCustomizers) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, serialGroups,
                sessionCustomizers, assemblyCustomizers, null, List.of());
    }

    public static RuntimeConfig defaults() {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of());
    }

    // ---- 单维度便捷工厂：供装配层把零散扩展 bean 包成 RC 后并入 merge（位置参数可读性） ----

    /** 仅含 hooks（其余字段空）。 */
    public static RuntimeConfig hooks(List<BuzhouHook> hooks) {
        return new RuntimeConfig(hooks, Set.of(), Set.of(), null, List.of());
    }

    /** 仅含自动工具（autoTools）。 */
    public static RuntimeConfig autoTools(List<ToolCallback> autoTools) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, autoTools);
    }

    /** 仅含 memory view processor。 */
    public static RuntimeConfig viewProcessor(MemoryViewProcessor viewProcessor) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), viewProcessor, List.of());
    }

    /** 仅含会话资源定制器（sessionCustomizers）。 */
    public static RuntimeConfig sessionCustomizers(List<SessionResourceCustomizer> customizers) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of(), Map.of(), customizers);
    }

    /** 仅含会话装配定制器（assemblyCustomizers）。 */
    public static RuntimeConfig assemblyCustomizers(List<SessionAssemblyCustomizer> customizers) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of(), Map.of(), List.of(), customizers);
    }

    /** 仅含有界 Turn 策略（turnLoopPolicy）。 */
    public static RuntimeConfig turnLoopPolicy(TurnLoopPolicy turnLoopPolicy) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of(), Map.of(), List.of(), List.of(),
                turnLoopPolicy);
    }

    /** 仅含级联清理贡献者（impl-35 / spec 13 §stores-6——store 外会话数据并入一次级联）。 */
    public static RuntimeConfig cleanupContributors(List<SessionCleanupContributor> contributors) {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of(), Map.of(), List.of(),
                List.of(), null, contributors);
    }

    public static RuntimeConfig merge(RuntimeConfig... configs) {
        List<BuzhouHook> hooks = new java.util.ArrayList<>();
        Set<String> disabled = new java.util.HashSet<>();
        Set<String> idempotent = new java.util.HashSet<>();
        List<MemoryViewProcessor> viewProcessors = new java.util.ArrayList<>();
        List<ToolCallback> autoTools = new java.util.ArrayList<>();
        Map<String, String> serialGroups = new java.util.HashMap<>();
        List<SessionResourceCustomizer> customizers = new java.util.ArrayList<>();
        List<SessionAssemblyCustomizer> assemblyCustomizers = new java.util.ArrayList<>();
        List<SessionCleanupContributor> cleanupContributors = new java.util.ArrayList<>();
        TurnLoopPolicy turnLoopPolicy = null;
        for (RuntimeConfig config : configs) {
            if (config == null) {
                continue;
            }
            hooks.addAll(config.hooks());
            disabled.addAll(config.disabledHookNames());
            idempotent.addAll(config.idempotentToolNames());
            if (config.viewProcessor() != null) {
                viewProcessors.add(config.viewProcessor());
            }
            autoTools.addAll(config.autoTools());
            serialGroups.putAll(config.serialGroups());
            customizers.addAll(config.sessionCustomizers());
            assemblyCustomizers.addAll(config.assemblyCustomizers());
            cleanupContributors.addAll(config.sessionCleanupContributors());
            if (config.turnLoopPolicy() != null) {
                turnLoopPolicy = config.turnLoopPolicy();
            }
        }
        return new RuntimeConfig(hooks, disabled, idempotent, compose(viewProcessors), autoTools,
                serialGroups, customizers, assemblyCustomizers, turnLoopPolicy, cleanupContributors);
    }

    /**
     * 多个 viewProcessor 时按 merge 顺序<b>链式组合</b>（前一个的输出是后一个的输入），
     * 替代旧的「后者静默覆盖前者」——支持 spill（hot-tail 溢出）与 memory（压缩/注入）叠加。
     */
    private static MemoryViewProcessor compose(List<MemoryViewProcessor> processors) {
        if (processors.isEmpty()) {
            return null;
        }
        if (processors.size() == 1) {
            return processors.getFirst();
        }
        return (sessionId, stored, currentTurn) -> {
            List<io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage> view = stored;
            for (MemoryViewProcessor processor : processors) {
                view = processor.process(sessionId, view, currentTurn);
            }
            return view;
        };
    }
}
