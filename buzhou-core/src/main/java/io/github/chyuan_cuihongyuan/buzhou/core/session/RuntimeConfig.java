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
        List<SessionResourceCustomizer> sessionCustomizers) {

    public RuntimeConfig {
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
        disabledHookNames = disabledHookNames == null ? Set.of() : Set.copyOf(disabledHookNames);
        idempotentToolNames = idempotentToolNames == null ? Set.of() : Set.copyOf(idempotentToolNames);
        autoTools = autoTools == null ? List.of() : List.copyOf(autoTools);
        serialGroups = serialGroups == null ? Map.of() : Map.copyOf(serialGroups);
        sessionCustomizers = sessionCustomizers == null ? List.of() : List.copyOf(sessionCustomizers);
    }

    public RuntimeConfig(List<BuzhouHook> hooks, Set<String> disabledHookNames,
                         Set<String> idempotentToolNames, MemoryViewProcessor viewProcessor,
                         List<ToolCallback> autoTools) {
        this(hooks, disabledHookNames, idempotentToolNames, viewProcessor, autoTools, Map.of(),
                List.of());
    }

    public static RuntimeConfig defaults() {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of());
    }

    public static RuntimeConfig merge(RuntimeConfig... configs) {
        List<BuzhouHook> hooks = new java.util.ArrayList<>();
        Set<String> disabled = new java.util.HashSet<>();
        Set<String> idempotent = new java.util.HashSet<>();
        MemoryViewProcessor viewProcessor = null;
        List<ToolCallback> autoTools = new java.util.ArrayList<>();
        Map<String, String> serialGroups = new java.util.HashMap<>();
        List<SessionResourceCustomizer> customizers = new java.util.ArrayList<>();
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
        }
        return new RuntimeConfig(hooks, disabled, idempotent, viewProcessor, autoTools,
                serialGroups, customizers);
    }
}
