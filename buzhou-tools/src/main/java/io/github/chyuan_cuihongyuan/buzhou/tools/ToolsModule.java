package io.github.chyuan_cuihongyuan.buzhou.tools;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.tools.command.CommandBlacklist;
import io.github.chyuan_cuihongyuan.buzhou.tools.command.RunCommandTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.command.SandboxRunCommandTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.file.ReadFileTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.file.WriteFileTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.http.HttpRequestTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.http.SsrfGuard;
import io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoAttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoStore;
import io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoTool;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内置原子工具模块入口（spec 06）：read_file / todo 默认开；write_file / run_command /
 * http_request 默认关（绑定级 opt-in，默认挂 HITL 守卫——守卫名单经
 * {@link #enabledDangerousToolNames()} 暴露给装配侧注册进 GuardModule）。
 *
 * <p>用法：
 * <pre>{@code
 * ToolsModule tools = ToolsModule.builder(stores.sessionStateStore())
 *         .fromYml(yml).sandboxRoot(Path.of("/var/agent")).build();
 * RuntimeConfig config = RuntimeConfig.merge(tools.configure(), guard.configure(), ...);
 * // 危险工具 HITL 接线：
 * tools.enabledDangerousToolNames().forEach(name -> guardBuilder.dangerousTool(name, ...));
 * // 写侧 Onload 接线：
 * tools.longContentParamDecls().forEach(d ->
 *         spillGuardBuilder.longContentParam(d.toolName(), d.contentParam(), d.pathParam()));
 * }</pre>
 *
 * <p>深度用法文档以内置 Skill 承载（本模块 {@code META-INF/skills/} 下），工具 Schema 保持瘦。
 */
public final class ToolsModule {

    private final boolean enabled;
    private final List<ToolCallback> tools;
    private final List<String> enabledDangerousToolNames;
    /** spec 1525：serial-groups yml 通道（构造期从 Builder 拷贝——configure 合并用）。 */
    private final Map<String, String> ymlSerialGroups;
    private final TodoStore todoStore;
    private final TodoAttachmentRenderer todoAttachmentRenderer;

    private ToolsModule(Builder builder) {
        this.enabled = builder.enabled;
        FileSandbox sandbox = new FileSandbox(builder.sandboxRoot, builder.allowedPaths);
        List<ToolCallback> t = new ArrayList<>();
        if (builder.readFileEnabled) {
            t.add(new ReadFileTool(sandbox));
        }
        if (builder.todoEnabled && builder.stateStore != null) {
            this.todoStore = new TodoStore(builder.stateStore);
            t.add(new TodoTool(this.todoStore));
        } else {
            this.todoStore = null;
        }
        if (builder.writeFileEnabled) {
            t.add(new WriteFileTool(sandbox));
        }
        if (builder.runCommandEnabled) {
            if (builder.commandBackend != null) {
                // spec 17 / impl-60：沙箱委托版（前置校验在 tools，执行隔离归 backend）。
                t.add(new SandboxRunCommandTool(sandbox,
                        builder.blacklist == null ? CommandBlacklist.defaults() : builder.blacklist,
                        builder.commandBackend, builder.runCommandTimeout, builder.runCommandMaxTimeout));
            } else {
                if ("sandbox".equals(builder.commandBackendMode)) {
                    throw new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException(
                            "buzhou.tools.command.backend=sandbox 但容器内没有 CommandBackend 实现",
                            "引入 buzhou-guard 并注册 CommandSandbox bean（guard 自动桥接为 CommandBackend），"
                                    + "或把 backend 改回 builtin");
                }
                t.add(new RunCommandTool(sandbox,
                        builder.blacklist == null ? CommandBlacklist.defaults() : builder.blacklist,
                        builder.runCommandTimeout, builder.runCommandMaxTimeout,
                        java.util.Set.of(), null,
                        builder.runCommandMaxOutputBytes == null
                                ? io.github.chyuan_cuihongyuan.buzhou.tools.command.RunCommandTool.DEFAULT_MAX_OUTPUT_BYTES
                                : builder.runCommandMaxOutputBytes));
            }
        }
        if (builder.httpRequestEnabled) {
            t.add(new HttpRequestTool(
                    new SsrfGuard(builder.ssrfBlockPrivateRanges, builder.ssrfAllowlist),
                    builder.httpRequestTimeout,
                    builder.httpMaxPerHost > 0
                            ? new io.github.chyuan_cuihongyuan.buzhou.tools.http.PerHostConcurrencyGuard(
                                    builder.httpMaxPerHost)
                            : null));
        }
        this.tools = List.copyOf(t);
        this.ymlSerialGroups = Map.copyOf(builder.ymlSerialGroups);
        // spec 1504 / T2259：危险名单注解驱动——扫描已装配工具的 @BuzhouTool.destructive
        // （保装配序；行为等价替代既往三处手工登记，新工具标注即自动入 HITL 清单）
        List<String> dangerous = new ArrayList<>();
        for (ToolCallback tool : this.tools) {
            BuzhouTool meta = tool.getClass().getAnnotation(BuzhouTool.class);
            if (meta != null && meta.destructive()) {
                dangerous.add(meta.name());
            }
        }
        this.enabledDangerousToolNames = List.copyOf(dangerous);
        this.todoAttachmentRenderer = this.todoStore == null ? null
                : new TodoAttachmentRenderer(this.todoStore);
    }

    public static Builder builder(SessionStateStore stateStore) {
        return new Builder(stateStore);
    }

    /** 从 yml map（前缀 buzhou.tools）解析配置（spec 06 配置项表）。 */
    public static Builder fromYml(SessionStateStore stateStore, Map<String, Object> ymlConfig) {
        return builder(stateStore).fromYml(ymlConfig);
    }

    /** 写侧长内容参数声明（write_file content/contentPath、http_request body/bodyPath）。 */
    public List<LongContentParamDecl> longContentParamDecls() {
        List<LongContentParamDecl> decls = new ArrayList<>();
        for (ToolCallback tool : tools) {
            String name = tool.getToolDefinition().name();
            if (name.equals("write_file")) {
                decls.add(new LongContentParamDecl("write_file", "content", "contentPath"));
            } else if (name.equals("http_request")) {
                decls.add(new LongContentParamDecl("http_request", "body", "bodyPath"));
            }
        }
        return List.copyOf(decls);
    }

    /** 已启用的危险工具名（供装配侧注册进 GuardModule 的 HITL 清单）。 */
    public List<String> enabledDangerousToolNames() {
        return enabledDangerousToolNames;
    }

    /** todo 清单 Attachment 渲染器（供 memory 注入视图构建方注入）；todo 未启用时返回 null。 */
    public TodoAttachmentRenderer todoAttachmentRenderer() {
        return todoAttachmentRenderer;
    }

    public RuntimeConfig configure() {
        if (!enabled) {
            return new RuntimeConfig(List.of(), Set.of(), Set.of(), null, List.of());
        }
        // @BuzhouTool 元数据 → RuntimeConfig（spec 06 注册模型）
        Set<String> idempotent = new HashSet<>();
        Map<String, String> serialGroups = new HashMap<>();
        for (ToolCallback tool : tools) {
            BuzhouTool meta = tool.getClass().getAnnotation(BuzhouTool.class);
            if (meta == null) {
                continue;
            }
            if (meta.idempotent()) {
                idempotent.add(meta.name());
            }
            if (!meta.serialGroup().isBlank()) {
                serialGroups.put(meta.name(), meta.serialGroup());
            }
        }
        // spec 1525：yml 显式 serial-groups 覆盖注解通道（同名 yml 优先）
        serialGroups.putAll(this.ymlSerialGroups);
        return new RuntimeConfig(List.of(), Set.of(), idempotent, null, tools, serialGroups,
                List.of());
    }

    public static final class Builder {

        private final SessionStateStore stateStore;
        private boolean enabled = true;
        // 默认开关矩阵（spec 06）：无害默认开、危险默认关（绑定级 opt-in）
        private boolean readFileEnabled = true;
        private boolean todoEnabled = true;
        private boolean writeFileEnabled = false;
        /** spec 1525 / T2301：serial-groups yml 通道（design-incompleteness F2 残留收口）——
         *  工具名 → 组名（注解通道之上的显式覆盖）。 */
        private Map<String, String> ymlSerialGroups = Map.of();
        private boolean runCommandEnabled = false;
        private boolean httpRequestEnabled = false;
        private Path sandboxRoot = Path.of(System.getProperty("user.dir"));
        private List<Path> allowedPaths = List.of();
        private Duration runCommandTimeout = Duration.ofSeconds(60);
        private Duration runCommandMaxTimeout = Duration.ofMinutes(10);
        /** spec 43 §A / T157：run_command 输出内存兜底上限（null = 工具默认 5MB）。 */
        private Long runCommandMaxOutputBytes;
        private CommandBlacklist blacklist;
        /** spec 17 / impl-60：沙箱委托（null = 内置 ProcessBuilder 版）。 */
        private io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend commandBackend;
        /** spec 17 / impl-60：backend 档位声明（builtin|sandbox；sandbox 无实现时 fail-fast）。 */
        private String commandBackendMode = "builtin";
        private Duration httpRequestTimeout = Duration.ofSeconds(30);
        /** spec 1603 / T2357：per-host 并发上限（0 = 关——默认零行为）。 */
        private int httpMaxPerHost;
        private boolean ssrfBlockPrivateRanges = true;
        private List<String> ssrfAllowlist = List.of();

        private Builder(SessionStateStore stateStore) {
            this.stateStore = stateStore;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder readFileEnabled(boolean v) {
            this.readFileEnabled = v;
            return this;
        }

        public Builder todoEnabled(boolean v) {
            this.todoEnabled = v;
            return this;
        }

        /** 危险工具 opt-in（推荐绑定级配置驱动，勿全局放开——spec 06 配置项节）。 */
        public Builder writeFileEnabled(boolean v) {
            this.writeFileEnabled = v;
            return this;
        }

        public Builder runCommandEnabled(boolean v) {
            this.runCommandEnabled = v;
            return this;
        }

        public Builder httpRequestEnabled(boolean v) {
            this.httpRequestEnabled = v;
            return this;
        }

        /** 文件沙箱根（默认应用工作目录）。 */
        public Builder sandboxRoot(Path root) {
            this.sandboxRoot = root;
            return this;
        }

        /** 沙箱追加白名单路径。 */
        public Builder allowedPaths(List<Path> paths) {
            this.allowedPaths = paths == null ? List.of() : List.copyOf(paths);
            return this;
        }

        public Builder runCommandTimeout(Duration timeout) {
            this.runCommandTimeout = timeout;
            return this;
        }

        public Builder runCommandMaxTimeout(Duration maxTimeout) {
            this.runCommandMaxTimeout = maxTimeout;
            return this;
        }

        /** spec 17 / impl-60：注入沙箱委托 backend（null = 内置 ProcessBuilder 版）。 */
        public Builder commandBackend(io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend backend) {
            this.commandBackend = backend;
            return this;
        }

        /** 整体替换命令黑名单（默认条目见 {@link CommandBlacklist#DEFAULT_PATTERNS}）。 */
        public Builder commandBlacklist(CommandBlacklist blacklist) {
            this.blacklist = blacklist;
            return this;
        }

        /** spec 1603 / T2358：per-host 并发上限（yml {@code http-max-per-host}；0 = 关）。 */
        public Builder httpMaxPerHost(int maxPerHost) {
            this.httpMaxPerHost = maxPerHost;
            return this;
        }

        public Builder httpRequestTimeout(Duration timeout) {
            this.httpRequestTimeout = timeout;
            return this;
        }

        public Builder ssrfBlockPrivateRanges(boolean v) {
            this.ssrfBlockPrivateRanges = v;
            return this;
        }

        public Builder ssrfAllowlist(List<String> entries) {
            this.ssrfAllowlist = entries == null ? List.of() : List.copyOf(entries);
            return this;
        }

        public Builder fromYml(Map<String, Object> ymlConfig) {
            if (ymlConfig == null || ymlConfig.isEmpty()) {
                return this;
            }
            this.enabled = boolOf(ymlConfig.get("enabled"), this.enabled);
            this.readFileEnabled = boolOf(sub(ymlConfig, "read-file").get("enabled"), this.readFileEnabled);
            this.todoEnabled = boolOf(sub(ymlConfig, "todo").get("enabled"), this.todoEnabled);
            this.writeFileEnabled = boolOf(sub(ymlConfig, "write-file").get("enabled"), this.writeFileEnabled);
            this.runCommandEnabled = boolOf(sub(ymlConfig, "run-command").get("enabled"), this.runCommandEnabled);
            this.httpRequestEnabled = boolOf(sub(ymlConfig, "http-request").get("enabled"),
                    this.httpRequestEnabled);

            Map<String, Object> fs = sub(ymlConfig, "file-sandbox");
            if (fs.get("root") instanceof String s && !s.isBlank()) {
                this.sandboxRoot = Path.of(s);
            }
            if (fs.get("allowed-paths") instanceof List<?> list) {
                this.allowedPaths = list.stream().map(String::valueOf).map(Path::of).toList();
            }

            Map<String, Object> rc = sub(ymlConfig, "run-command");
            if (rc.get("timeout-seconds") instanceof Number n) {
                this.runCommandTimeout = Duration.ofSeconds(n.longValue());
            }
            // spec 43 §A / T157 / impl-128：max-output-bytes（非正数 fail-fast——兜底上限配错不如不配）
            if (rc.get("max-output-bytes") instanceof Number n) {
                if (n.longValue() <= 0) {
                    throw new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException(
                            "buzhou.tools.run-command.max-output-bytes（" + n.longValue() + "）非法",
                            "正整数字节数（缺省 5MB）");
                }
                this.runCommandMaxOutputBytes = n.longValue();
            }
            if (rc.get("blacklist") instanceof List<?> list) {
                this.blacklist = new CommandBlacklist(
                        list.stream().map(String::valueOf).toList());
            }

            // spec 17 / impl-60：command.backend（builtin|sandbox；默认 builtin）。
            Map<String, Object> cmd = sub(ymlConfig, "command");
            if (cmd.get("backend") instanceof String mode && !mode.isBlank()) {
                String normalized = mode.trim().toLowerCase(java.util.Locale.ROOT);
                if (!normalized.equals("builtin") && !normalized.equals("sandbox")) {
                    throw new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException(
                            "buzhou.tools.command.backend（" + mode + "）非法",
                            "取值只能是 builtin 或 sandbox");
                }
                this.commandBackendMode = normalized;
            }

            Map<String, Object> hr = sub(ymlConfig, "http-request");
            if (hr.get("timeout-seconds") instanceof Number n) {
                this.httpRequestTimeout = Duration.ofSeconds(n.longValue());
            }
            // spec 1603 / T2358：max-per-host（声明即启用 per-host 并发闸——缺省 0=关）
            if (hr.get("max-per-host") instanceof Number maxPerHost) {
                this.httpMaxPerHost = maxPerHost.intValue();
            }
            Map<String, Object> ssrf = sub(hr, "ssrf");
            this.ssrfBlockPrivateRanges = boolOf(ssrf.get("block-private-ranges"),
                    this.ssrfBlockPrivateRanges);
            if (ssrf.get("allowlist") instanceof List<?> list) {
                this.ssrfAllowlist = list.stream().map(String::valueOf).toList();
            }
            // spec 1525 / T2301：serial-groups yml 通道（name → group；F2 残留——此前
            // 仅 @BuzhouTool 注解通道，yml 键全仓零读取）
            if (ymlConfig.get("serial-groups") instanceof Map<?, ?> groups) {
                Map<String, String> parsed = new HashMap<>();
                groups.forEach((k, v) -> {
                    if (k != null && v instanceof String g && !g.isBlank()) {
                        parsed.put(String.valueOf(k), g);
                    }
                });
                this.ymlSerialGroups = Map.copyOf(parsed);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> sub(Map<String, Object> map, String key) {
            return map.get(key) instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        }

        private static boolean boolOf(Object value, boolean fallback) {
            return value instanceof Boolean b ? b : fallback;
        }

        public ToolsModule build() {
            return new ToolsModule(this);
        }
    }
}
