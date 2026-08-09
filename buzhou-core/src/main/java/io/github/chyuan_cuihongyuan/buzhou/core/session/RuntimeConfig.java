package io.github.chyuan_cuihongyuan.buzhou.core.session;

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
        List<SessionAssemblyCustomizer> assemblyCustomizers) {

    public RuntimeConfig {
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
        disabledHookNames = disabledHookNames == null ? Set.of() : Set.copyOf(disabledHookNames);
        idempotentToolNames = idempotentToolNames == null ? Set.of() : Set.copyOf(idempotentToolNames);
        autoTools = autoTools == null ? List.of() : List.copyOf(autoTools);
        serialGroups = serialGroups == null ? Map.of() : Map.copyOf(serialGroups);
        sessionCustomizers = sessionCustomizers == null ? List.of() : List.copyOf(sessionCustomizers);
        assemblyCustomizers = assemblyCustomizers == null ? List.of() : List.copyOf(assemblyCustomizers);
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, Map.of(),
                List.of(), List.of());
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools, Map<String, String> serialGroups,
                         List<SessionResourceCustomizer> sessionCustomizers) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, serialGroups,
                sessionCustomizers, List.of());
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

    public static RuntimeConfig merge(RuntimeConfig... configs) {
        List<BuzhouHook> hooks = new java.util.ArrayList<>();
        Set<String> disabled = new java.util.HashSet<>();
        Set<String> idempotent = new java.util.HashSet<>();
        MemoryViewProcessor viewProcessor = null;
        List<ToolCallback> autoTools = new java.util.ArrayList<>();
        Map<String, String> serialGroups = new java.util.HashMap<>();
        List<SessionResourceCustomizer> customizers = new java.util.ArrayList<>();
        List<SessionAssemblyCustomizer> assemblyCustomizers = new java.util.ArrayList<>();
        for (RuntimeConfig config : configs) {
            if (config == null) {
                continue;
            }
            hooks.addAll(config.hooks());
            disabled.addAll(config.disabledHookNames());
            idempotent.addAll(config.idempotentToolNames());
            if (config.viewProcessor() != null) {
                viewProcessor = config.viewProcessor();
            }
            autoTools.addAll(config.autoTools());
            serialGroups.putAll(config.serialGroups());
            customizers.addAll(config.sessionCustomizers());
            assemblyCustomizers.addAll(config.assemblyCustomizers());
        }
        return new RuntimeConfig(hooks, disabled, idempotent, viewProcessor, autoTools,
                serialGroups, customizers, assemblyCustomizers);
    }
}
