package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MemoryViewProcessor;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Set;

public record RuntimeConfig(
        List<BuzhouHook> hooks,
        Set<String> disabledHookNames,
        Set<String> idempotentToolNames,
        MemoryViewProcessor viewProcessor,
        List<ToolCallback> autoTools) {

    public RuntimeConfig {
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
        disabledHookNames = disabledHookNames == null ? Set.of() : Set.copyOf(disabledHookNames);
        idempotentToolNames = idempotentToolNames == null ? Set.of() : Set.copyOf(idempotentToolNames);
        autoTools = autoTools == null ? List.of() : List.copyOf(autoTools);
    }

    public static RuntimeConfig defaults() {
        return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of());
    }
}
