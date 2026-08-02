package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;

public class StrReplaceTool implements ToolCallback {

    private static final Logger LOG = System.getLogger(StrReplaceTool.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileSandbox sandbox;

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
                return "str_replace 失败：缺少 newStr 参数（或经 newStrPath 由框架加载）";
            }
            if (oldStr == null || oldStr.isEmpty()) {
                return "str_replace 失败：oldStr 不能为空";
            }
            Path target = sandbox.resolve(pathRaw);
            if (!Files.isRegularFile(target)) {
                return "str_replace 失败：目标文件不存在：" + pathRaw;
            }
            String content = Files.readString(target);
            int occurrences = countOccurrences(content, oldStr);
            if (occurrences == 0) {
                return "str_replace 失败：未找到待替换原文（oldStr），请核对文件内容";
            }
            if (occurrences > 1) {
                return "str_replace 失败：oldStr 在文件中不唯一（出现 " + occurrences
                        + " 次），请补充更多上下文以唯一匹配";
            }
            Files.writeString(target, content.replace(oldStr, newStr));
            return "替换成功：" + target;
        } catch (Exception e) {
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
