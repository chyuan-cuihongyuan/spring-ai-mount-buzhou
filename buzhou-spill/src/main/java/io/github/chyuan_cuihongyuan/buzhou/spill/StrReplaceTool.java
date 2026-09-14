package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
/** spec 1529 / T2309：溢写内容局部替换工具——str_replace 对落盘溢写做原位编辑（免整文件重写）。 */

public class StrReplaceTool implements ToolCallback {

    private static final Logger LOG = System.getLogger(StrReplaceTool.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileSandbox sandbox;

    // —— spec 1054 / impl 806：编辑判定读面（Anthropic text editor str_replace 的
    // notFound/ambiguous 失败模式分布思想；静态面理由同 R46–R53 先例）。
    // 守恒：attempts = successes + 五拒绝桶之和。
    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong SUCCESSES = new AtomicLong();
    private static final AtomicLong PARAM_REJECTS = new AtomicLong();
    private static final AtomicLong MISSING_FILE_REJECTS = new AtomicLong();
    private static final AtomicLong NOT_FOUND_REJECTS = new AtomicLong();
    private static final AtomicLong AMBIGUOUS_REJECTS = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    /** 编辑判定分布快照（spec 1054）。 */
    public record StrReplaceStats(long attempts, long successes, long paramRejects,
                                  long missingFileRejects, long notFoundRejects,
                                  long ambiguousRejects, long failures) {

        /** 拒绝总数（五桶之和）。 */
        public long totalRejects() {
            return paramRejects + missingFileRejects + notFoundRejects
                    + ambiguousRejects + failures;
        }
    }

    /** 只读快照（守恒 attempts = successes + totalRejects()）。 */
    public static StrReplaceStats stats() {
        return new StrReplaceStats(ATTEMPTS.get(), SUCCESSES.get(), PARAM_REJECTS.get(),
                MISSING_FILE_REJECTS.get(), NOT_FOUND_REJECTS.get(),
                AMBIGUOUS_REJECTS.get(), FAILURES.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        SUCCESSES.set(0);
        PARAM_REJECTS.set(0);
        MISSING_FILE_REJECTS.set(0);
        NOT_FOUND_REJECTS.set(0);
        AMBIGUOUS_REJECTS.set(0);
        FAILURES.set(0);
    }

    public StrReplaceTool(FileSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("str_replace")
                .description("精确替换工作副本文件中的唯一匹配文本。直改只读快照会被拦截，请先 copy_file 建副本。"
                        + "长替换内容推荐改走 newStrPath 让框架自动加载，避免长内容拼入参时自截断。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"目标文件（必须为工作副本）"},
                          "oldStr":{"type":"string","description":"待替换原文，须在文件中唯一出现"},
                          "newStr":{"type":"string","description":"替换内容"},
                          "newStrPath":{"type":"string","description":"长替换内容的互补路径参数，非空时框架自动加载全文覆盖 newStr"}
                        },"required":["path","oldStr"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        ATTEMPTS.incrementAndGet();
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            if (args.hasNonNull("newStrPath")) {
                LOG.log(Level.WARNING,
                        "str_replace 收到 newStrPath，OnloadHook 应已剥离该参数——Hook 可能未生效");
            }
            String pathRaw = args.path("path").asText("");
            String oldStr = args.hasNonNull("oldStr") ? args.path("oldStr").asText() : null;
            String newStr = args.hasNonNull("newStr") ? args.path("newStr").asText() : null;
            if (newStr == null) {
                PARAM_REJECTS.incrementAndGet();
                return "str_replace 失败：缺少 newStr 参数（或经 newStrPath 由框架加载）";
            }
            if (oldStr == null || oldStr.isEmpty()) {
                PARAM_REJECTS.incrementAndGet();
                return "str_replace 失败：oldStr 不能为空";
            }
            Path target = sandbox.resolve(pathRaw);
            if (!Files.isRegularFile(target)) {
                MISSING_FILE_REJECTS.incrementAndGet();
                return "str_replace 失败：目标文件不存在：" + pathRaw;
            }
            String content = Files.readString(target);
            int occurrences = countOccurrences(content, oldStr);
            if (occurrences == 0) {
                NOT_FOUND_REJECTS.incrementAndGet();
                return "str_replace 失败：未找到待替换原文（oldStr），请核对文件内容";
            }
            if (occurrences > 1) {
                AMBIGUOUS_REJECTS.incrementAndGet();
                return "str_replace 失败：oldStr 在文件中不唯一（出现 " + occurrences
                        + " 次），请补充更多上下文以唯一匹配";
            }
            Files.writeString(target, content.replace(oldStr, newStr));
            SUCCESSES.incrementAndGet();
            return "替换成功：" + target;
        } catch (Exception e) {
            FAILURES.incrementAndGet();
            return "str_replace 失败：" + e.getMessage();
        }
    }

    private static int countOccurrences(String content, String needle) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
