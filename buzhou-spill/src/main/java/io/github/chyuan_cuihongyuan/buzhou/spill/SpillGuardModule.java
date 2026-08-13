package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SpillGuardModule {

    private final List<BuzhouHook> hooks;
    private final List<ToolCallback> tools;
    private final SessionReadOnlyRegistry readOnlyRegistry;
    private final HotTailViewProcessor hotTailProcessor;

    private SpillGuardModule(Builder builder) {
        this.readOnlyRegistry = new SessionReadOnlyRegistry();
        FileSandbox sandbox = new FileSandbox(builder.sandboxRoot, builder.additionalAllowedRoots);
        List<BuzhouHook> h = new ArrayList<>();
        List<ToolCallback> t = new ArrayList<>();
        if (builder.copyOnWriteEnabled) {
            h.add(new CopyOnWriteGuardHook(readOnlyRegistry, builder.readonlyRoots,
                    builder.editToolPathParams));
        }
        if (builder.onloadEnabled) {
            h.add(new OnloadHook(sandbox, builder.longContentParams));
        }
        if (builder.offloadEnabled) {
            // token-aware 全局阈值（T20）：thresholdTokens 优先于 thresholdChars（×4 折算字符）
            int effectiveThreshold = builder.thresholdTokens > 0
                    ? builder.thresholdTokens * SpillThresholds.CHARS_PER_TOKEN_ESTIMATE
                    : builder.thresholdChars;
            h.add(new SpillOffloadHook(builder.spillService, readOnlyRegistry,
                    builder.spillFileResolver, effectiveThreshold, builder.toolPolicies,
                    builder.offloadOnFail));
        }
        if (builder.editingToolsEnabled) {
            t.add(new CopyFileTool(sandbox, builder.readonlyRoots));
            t.add(new StrReplaceTool(sandbox));
        }
        this.hooks = List.copyOf(h);
        this.tools = List.copyOf(t);
        // T21 hot-tail/cold-storage：近期 N 条工具结果全量内联、旧结果视图级溢出。
        // 启用即建议关闭即时 offload（互斥），否则大结果产生时即被替换、hot-tail 无从保留全量。
        this.hotTailProcessor = builder.hotTailKeepInline > 0
                ? new HotTailViewProcessor(builder.spillService, builder.hotTailKeepInline,
                        builder.hotTailMaxInlineChars, builder.spillFileResolver, readOnlyRegistry,
                        builder.toolPolicies, builder.thresholdTokens > 0
                                ? builder.thresholdTokens * SpillThresholds.CHARS_PER_TOKEN_ESTIMATE
                                : builder.thresholdChars)
                : null;
    }

    public static Builder builder(SpillService spillService,
                                  Function<SpillUri, Path> spillFileResolver, Path sandboxRoot) {
        return new Builder(spillService, spillFileResolver, sandboxRoot);
    }

    public static Builder fromModule(SpillModule spill, Path sandboxRoot) {
        return builder(spill.service(), spill.store()::dataPathOf, sandboxRoot);
    }

    public SessionReadOnlyRegistry readOnlyRegistry() {
        return readOnlyRegistry;
    }

    /** hot-tail 视图处理器（未启用 hot-tail 时为 null；供与 memory 视图手动组合）。 */
    public HotTailViewProcessor hotTailProcessor() {
        return hotTailProcessor;
    }

    public RuntimeConfig configure() {
        return new RuntimeConfig(hooks, Set.of(), Set.of(), hotTailProcessor, tools, Map.of(),
                List.of((registry, appId, agentName, sessionId) ->
                        registry.register("guard-readonly-evict",
                                () -> readOnlyRegistry.evict(sessionId))),
                List.of(), null);
    }

    public static final class Builder {

        private final SpillService spillService;
        private final Function<SpillUri, Path> spillFileResolver;
        private final Path sandboxRoot;
        private int thresholdChars = SpillOffloadHook.DEFAULT_THRESHOLD_CHARS;
        private int thresholdTokens = 0;
        private int hotTailKeepInline = 0;
        private long hotTailMaxInlineChars = 0;
        private Map<String, Object> toolPolicies = Map.of();
        private final Map<String, List<LongContentParamPair>> longContentParams = new LinkedHashMap<>();
        private final List<Path> readonlyRoots = new ArrayList<>();
        private final List<Path> additionalAllowedRoots = new ArrayList<>();
        private Map<String, String> editToolPathParams = null;
        private boolean onloadEnabled = true;
        private boolean offloadExplicit = false;
        private boolean copyOnWriteEnabled = true;
        private boolean offloadEnabled = true;
        private boolean editingToolsEnabled = true;
        private io.github.chyuan_cuihongyuan.buzhou.core.hook.OnFail offloadOnFail =
                io.github.chyuan_cuihongyuan.buzhou.core.hook.OnFail.FILTER;

        private Builder(SpillService spillService, Function<SpillUri, Path> spillFileResolver,
                        Path sandboxRoot) {
            this.spillService = spillService;
            this.spillFileResolver = spillFileResolver;
            this.sandboxRoot = sandboxRoot;
            longContentParam("str_replace", "newStr", "newStrPath");
        }

        /** 读侧溢出失败的 on_fail 动词（T19）：FILTER=降级透传（默认，既有语义）；REFRAIN=保守拒答替代。 */
        public Builder offloadOnFail(io.github.chyuan_cuihongyuan.buzhou.core.hook.OnFail onFail) {
            this.offloadOnFail = onFail;
            return this;
        }

        /** token-aware 全局溢出阈值（T20）：按 token 计（>0 时优先生效，×4 折算字符）。 */
        public Builder thresholdTokens(int tokens) {
            this.thresholdTokens = tokens;
            return this;
        }

        /** T21 hot-tail：近期 N 条工具结果全量内联（>0 启用 hot-tail 视图处理器）。 */
        public Builder hotTail(int keepInlineToolResults) {
            this.hotTailKeepInline = keepInlineToolResults;
            // 互斥强制：启用 hot-tail 即关闭即时 offload（否则大结果产生时即被替换、
            // hot-tail 无从保留近期全量内联），除非调用方已显式开启 offload。
            if (keepInlineToolResults > 0 && !offloadExplicit) {
                this.offloadEnabled = false;
            }
            return this;
        }

        /** T21 hot-tail 大小预算：内联 TOOL 内容总字符上限（<=0 不限；超限从最旧补溢出）。 */
        public Builder hotTailMaxInlineChars(long maxInlineChars) {
            this.hotTailMaxInlineChars = maxInlineChars;
            return this;
        }

        public Builder thresholdChars(int chars) {
            this.thresholdChars = chars;
            return this;
        }

        public Builder toolPolicies(Map<String, Object> policies) {
            this.toolPolicies = policies;
            return this;
        }

        public Builder longContentParam(String toolName, String contentParam, String pathParam) {
            this.longContentParams.computeIfAbsent(toolName, k -> new ArrayList<>())
                    .add(new LongContentParamPair(contentParam, pathParam));
            return this;
        }

        public Builder readonlyRoot(Path path) {
            this.readonlyRoots.add(path);
            return this;
        }

        public Builder allowedRoot(Path path) {
            this.additionalAllowedRoots.add(path);
            return this;
        }

        public Builder editToolPathParams(Map<String, String> params) {
            this.editToolPathParams = params;
            return this;
        }

        public Builder offloadEnabled(boolean enabled) {
            this.offloadEnabled = enabled;
            this.offloadExplicit = true;
            return this;
        }

        public Builder onloadEnabled(boolean enabled) {
            this.onloadEnabled = enabled;
            return this;
        }

        public Builder copyOnWriteEnabled(boolean enabled) {
            this.copyOnWriteEnabled = enabled;
            return this;
        }

        public Builder editingToolsEnabled(boolean enabled) {
            this.editingToolsEnabled = enabled;
            return this;
        }

        public SpillGuardModule build() {
            return new SpillGuardModule(this);
        }
    }
}
