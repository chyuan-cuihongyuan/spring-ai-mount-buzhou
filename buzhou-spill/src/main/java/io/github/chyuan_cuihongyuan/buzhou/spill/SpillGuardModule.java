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
            h.add(new SpillOffloadHook(builder.spillService, readOnlyRegistry,
                    builder.spillFileResolver, builder.thresholdChars, builder.toolPolicies));
        }
        if (builder.editingToolsEnabled) {
            t.add(new CopyFileTool(sandbox, builder.readonlyRoots));
            t.add(new StrReplaceTool(sandbox));
        }
        this.hooks = List.copyOf(h);
        this.tools = List.copyOf(t);
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

    public RuntimeConfig configure() {
        return new RuntimeConfig(hooks, Set.of(), Set.of(), null, tools, Map.of(),
                List.of((registry, appId, agentName, sessionId) ->
                        registry.register("guard-readonly-evict",
                                () -> readOnlyRegistry.evict(sessionId))));
    }

    public static final class Builder {

        private final SpillService spillService;
        private final Function<SpillUri, Path> spillFileResolver;
        private final Path sandboxRoot;
        private int thresholdChars = SpillOffloadHook.DEFAULT_THRESHOLD_CHARS;
        private Map<String, Object> toolPolicies = Map.of();
        private final Map<String, List<LongContentParamPair>> longContentParams = new LinkedHashMap<>();
        private final List<Path> readonlyRoots = new ArrayList<>();
        private final List<Path> additionalAllowedRoots = new ArrayList<>();
        private Map<String, String> editToolPathParams = null;
        private boolean onloadEnabled = true;
        private boolean copyOnWriteEnabled = true;
        private boolean offloadEnabled = true;
        private boolean editingToolsEnabled = true;

        private Builder(SpillService spillService, Function<SpillUri, Path> spillFileResolver,
                        Path sandboxRoot) {
            this.spillService = spillService;
            this.spillFileResolver = spillFileResolver;
            this.sandboxRoot = sandboxRoot;
            longContentParam("str_replace", "newStr", "newStrPath");
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

        public Builder onloadEnabled(boolean enabled) {
            this.onloadEnabled = enabled;
            return this;
        }

        public Builder copyOnWriteEnabled(boolean enabled) {
            this.copyOnWriteEnabled = enabled;
            return this;
        }

        public Builder offloadEnabled(boolean enabled) {
            this.offloadEnabled = enabled;
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
