package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Optional;

/**
 * {@code read_range} 内置原子工具：范围回读 spill:// 溢出内容；接管 skill:// 技能资源路径
 * （spec 04 推演：资源读取复用 read_range，不新增专用工具）。
 *
 * <p>skill:// 解析委托装配期注入的 {@link SkillResourceResolver}（buzhou-skills 提供，
 * 含会话绑定校验，sessionId 取自 ToolContext 的 {@link HarnessToolCallingManager#SESSION_ID_KEY}）；
 * 资源仅支持 bytes 模式（offset/limit 区间截取）。超大资源内容随工具返回自动走 spill 溢出管道。
 */
public class ReadRangeTool implements ToolCallback {

    static final String SKILL_SCHEME = "skill://";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SpillService spillService;
    private final SkillResourceResolver skillResourceResolver;
    /** impl-16 / T44：句柄引用计数（成功回读刷新 TTL；null = 不启用）。 */
    private HandleLifecycleRegistry handleLifecycle;

    public ReadRangeTool(SpillService spillService) {
        this(spillService, null);
    }

    public ReadRangeTool(SpillService spillService, SkillResourceResolver skillResourceResolver) {
        this.spillService = spillService;
        this.skillResourceResolver = skillResourceResolver;
    }

    /** impl-16 / T44：注入句柄生命周期注册表（成功回读即刷新引用、句柄免于 TTL 逐出）。 */
    public void setHandleLifecycle(HandleLifecycleRegistry handleLifecycle) {
        this.handleLifecycle = handleLifecycle;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("read_range")
                .description("按路径范围读取内容：spill:// 溢出内容 / skill:// 技能资源。mode=bytes 字符区间（可配 window=head|tail|head_tail 取头尾窗口、中段显式省略标记） / json JSON path 抽取 / page 数组分页（skill:// 仅支持 bytes）。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"spill:// 或 skill://<name>/<relativePath> URI"},
                          "mode":{"type":"string","enum":["bytes","json","page"]},
                          "window":{"type":"string","enum":["head","tail","head_tail"],"description":"bytes 模式窗口风味：头/尾窗口 + 中段显式省略标记（原始字节完整保留可回取）"},
                          "offset":{"type":"integer","description":"bytes 模式起始偏移"},
                          "limit":{"type":"integer","description":"bytes 模式返回量；window 风味下为头窗口大小"},
                          "tailLimit":{"type":"integer","description":"window=head_tail 时的尾窗口大小（默认与 limit 对称）"},
                          "jsonPath":{"type":"string","description":"json 模式路径，如 $.a.b[0]"},
                          "cursor":{"type":"string","description":"page 模式续读游标"}
                        },"required":["path","mode"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String path = args.path("path").asText();
            String mode = args.path("mode").asText("bytes");
            if (path != null && path.startsWith(SKILL_SCHEME)) {
                return readSkillResource(path, mode, args,
                        HarnessToolCallingManager.sessionIdOf(toolContext));
            }
            RangeReadRequest request = switch (mode) {
                case "json" -> RangeReadRequest.json(args.path("jsonPath").asText("$"));
                case "page" -> RangeReadRequest.page(
                        args.hasNonNull("cursor") ? args.path("cursor").asText() : null,
                        args.hasNonNull("limit") ? args.path("limit").asInt() : 20);
                default -> windowOf(args).map(window -> RangeReadRequest.bytesWindow(window,
                        args.hasNonNull("limit") ? args.path("limit").asInt() : 20000,
                        args.hasNonNull("tailLimit") ? args.path("tailLimit").asInt()
                                : (args.hasNonNull("limit") ? args.path("limit").asInt() : 20000)))
                        .orElseGet(() -> RangeReadRequest.bytes(
                        args.hasNonNull("offset") ? args.path("offset").asInt() : 0,
                        args.hasNonNull("limit") ? args.path("limit").asInt() : 20000));
            };
            RangeReadResult result = spillService.readBack(path, request);
            // impl-16 / T44：成功回读置位标记（视图处理器吸收为引用、TTL 重启、句柄复活）
            if (handleLifecycle != null) {
                handleLifecycle.markRead(path);
            }
            return result.truncated()
                    ? result.content() + "\n[已截断，可用 offset/cursor 续读]"
                    : result.content();
        } catch (Exception e) {
            return "read_range 调用失败：" + e.getMessage();
        }
    }

    /** window 参数解析（非法值按无窗口处理，走既有 offset/limit 区间语义）。 */
    private static Optional<RangeReadRequest.Window> windowOf(JsonNode args) {
        if (!args.hasNonNull("window")) {
            return Optional.empty();
        }
        return switch (args.path("window").asText()) {
            case "head" -> Optional.of(RangeReadRequest.Window.HEAD);
            case "tail" -> Optional.of(RangeReadRequest.Window.TAIL);
            case "head_tail" -> Optional.of(RangeReadRequest.Window.HEAD_TAIL);
            default -> Optional.empty();
        };
    }

    /** skill://&lt;name&gt;/&lt;relativePath&gt; 解析：委托注入的 resolver，bytes 模式区间截取。 */
    private String readSkillResource(String path, String mode, JsonNode args, String sessionId) {
        if (!"bytes".equals(mode)) {
            return "skill:// 资源仅支持 bytes 模式（收到：" + mode + "）";
        }
        if (skillResourceResolver == null) {
            return "skill:// 资源读取未接线（需引入 buzhou-skills 并经 SpillModule.skillResourceResolver 注入）";
        }
        String rest = path.substring(SKILL_SCHEME.length());
        int slash = rest.indexOf('/');
        if (slash < 0 || slash == rest.length() - 1) {
            return "skill:// 路径缺少资源相对路径：" + path;
        }
        String skillName = rest.substring(0, slash);
        String relativePath = rest.substring(slash + 1);
        Optional<String> content = skillResourceResolver.resolve(sessionId, skillName, relativePath);
        if (content.isEmpty()) {
            return "技能资源不存在或未绑定：" + path;
        }
        String text = content.get();
        int offset = Math.max(0, args.hasNonNull("offset") ? args.path("offset").asInt() : 0);
        int limit = args.hasNonNull("limit") ? args.path("limit").asInt() : 20000;
        if (offset >= text.length()) {
            return "[已越界：offset=" + offset + "，资源长度=" + text.length() + "]";
        }
        int end = limit < 0 ? text.length() : Math.min(text.length(), offset + limit);
        String slice = text.substring(offset, end);
        return end < text.length() ? slice + "\n[已截断，可用 offset 续读]" : slice;
    }
}
