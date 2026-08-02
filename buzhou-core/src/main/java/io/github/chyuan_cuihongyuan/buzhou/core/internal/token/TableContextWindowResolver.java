package io.github.chyuan_cuihongyuan.buzhou.core.internal.token;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ContextWindowResolver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TableContextWindowResolver implements ContextWindowResolver {

    private static final Logger LOG = System.getLogger(TableContextWindowResolver.class.getName());
    private static final int DEFAULT_WINDOW = 32768;

    private static final Map<String, Integer> BUILT_IN = new LinkedHashMap<>();

    static {
        BUILT_IN.put("gpt-5", 400000);
        BUILT_IN.put("gpt-4", 128000);
        BUILT_IN.put("o1", 200000);
        BUILT_IN.put("o3", 200000);
        BUILT_IN.put("claude", 200000);
        BUILT_IN.put("deepseek", 65536);
        BUILT_IN.put("qwen", 131072);
        BUILT_IN.put("qwq", 131072);
        BUILT_IN.put("gemini", 1000000);
        BUILT_IN.put("glm", 131072);
        BUILT_IN.put("kimi", 131072);
    }

    private final Map<String, Integer> overrides;
    private final Set<String> warnedModels = ConcurrentHashMap.newKeySet();

    public TableContextWindowResolver(Map<String, Integer> overrides) {
        this.overrides = overrides == null ? Map.of() : overrides;
    }

    @Override
    public int resolveWindow(String modelName) {
        if (modelName == null) {
            return DEFAULT_WINDOW;
        }
        Integer override = overrides.get(modelName);
        if (override != null) {
            return override;
        }
        String lower = modelName.toLowerCase();
        for (Map.Entry<String, Integer> entry : BUILT_IN.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (warnedModels.add(modelName)) {
            LOG.log(Level.WARNING,
                    "Unknown model window, falling back to 32K: " + modelName);
        }
        return DEFAULT_WINDOW;
    }
}
