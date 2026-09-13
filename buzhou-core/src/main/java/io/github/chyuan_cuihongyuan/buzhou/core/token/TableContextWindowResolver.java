package io.github.chyuan_cuihongyuan.buzhou.core.token;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ContextWindowResolver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置模型上下文窗口表 + yml 覆盖（前缀匹配内置表，未知模型回退 32K 且每模型
 * 只告警一次）。
 *
 * <p>spec 707 / T965：自 {@code core.internal.token} 迁出——跨模块复用类不入
 * internal（边界守卫 ModuleBoundaryGuardTest 口径）。
 */
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
    /** impl-781 / spec 1028：三路解析分布计数（守恒：三者和 == resolveWindow 调用数）。 */
    private final java.util.concurrent.atomic.AtomicLong overrideHits =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong builtInHits =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong fallbackHits =
            new java.util.concurrent.atomic.AtomicLong();
    /** 已解析模型 → 窗值（有界：模型名源自应用配置）。 */
    private final ConcurrentHashMap<String, Integer> resolvedWindows =
            new ConcurrentHashMap<>();

    public TableContextWindowResolver(Map<String, Integer> overrides) {
        this.overrides = overrides == null ? Map.of() : overrides;
    }

    @Override
    public int resolveWindow(String modelName) {
        if (modelName == null) {
            fallbackHits.incrementAndGet();
            return DEFAULT_WINDOW;
        }
        Integer override = overrides.get(modelName);
        if (override != null) {
            overrideHits.incrementAndGet();
            resolvedWindows.put(modelName, override);
            return override;
        }
        String lower = modelName.toLowerCase();
        for (Map.Entry<String, Integer> entry : BUILT_IN.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                builtInHits.incrementAndGet();
                resolvedWindows.put(modelName, entry.getValue());
                return entry.getValue();
            }
        }
        if (warnedModels.add(modelName)) {
            LOG.log(Level.WARNING,
                    "Unknown model window, falling back to 32K: " + modelName);
        }
        fallbackHits.incrementAndGet();
        resolvedWindows.put(modelName, DEFAULT_WINDOW);
        return DEFAULT_WINDOW;
    }

    /** 解析分布只读快照（override/内置/回退三路计数 + 已解析模型窗；spec 1028）。 */
    public WindowResolutionStats stats() {
        return new WindowResolutionStats(overrideHits.get(), builtInHits.get(),
                fallbackHits.get(), Map.copyOf(resolvedWindows));
    }

    /** 解析分布计数行（不可变）。 */
    public record WindowResolutionStats(long overrideHits, long builtInHits, long fallbackHits,
                                        Map<String, Integer> resolvedWindows) {
        public WindowResolutionStats {
            resolvedWindows = Map.copyOf(resolvedWindows);
        }

        /** 三路守恒：和 == 解析总数。 */
        public long total() {
            return overrideHits + builtInHits + fallbackHits;
        }
    }
}
