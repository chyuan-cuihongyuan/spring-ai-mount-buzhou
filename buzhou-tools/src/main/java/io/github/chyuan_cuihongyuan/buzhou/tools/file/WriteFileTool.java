package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * write_file — 写入沙箱内文件（危险，默认关、绑定级 opt-in、默认挂 HITL 守卫）。
 *
 * <p>{@code content} 为写侧长内容参数：长内容推荐走 {@code contentPath} 由 Onload Hook
 * 加载全文覆盖（Onload 失败 BLOCK 阻断，写侧失败语义非对称，ticket 24）。
 * 本工具自身不读 {@code contentPath}——若调用时该参数仍在，说明 Onload Hook 未装配/未生效。
 */
@BuzhouTool(name = "write_file", serialGroup = "file")
public class WriteFileTool implements ToolCallback {

    private static final Logger LOG = System.getLogger(WriteFileTool.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileSandbox sandbox;

    public WriteFileTool(FileSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("write_file")
                .description("写入沙箱内文件（不存在则创建，已存在则覆盖）。"
                        + "长内容推荐走 contentPath 让框架自动加载，避免长内容拼入参时自截断。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"目标路径（沙箱内）"},
                          "content":{"type":"string","description":"写入内容"},
                          "contentPath":{"type":"string","description":"互补路径参数，非空时框架自动加载全文覆盖 content"}
                        },"required":["path"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            if (args.hasNonNull("contentPath")) {
                LOG.log(Level.WARNING,
                        "write_file 收到 contentPath，OnloadHook 应已剥离该参数——Hook 可能未生效");
            }
            String raw = args.path("path").asText("");
            String content = args.hasNonNull("content") ? args.path("content").asText() : null;
            if (content == null) {
                return "write_file 失败：缺少 content 参数（或经 contentPath 由框架加载）";
            }
            Path target = sandbox.resolveForWrite(raw);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return "已写入：" + target + "（" + content.length() + " 字符）";
        } catch (Exception e) {
            return "write_file 失败：" + e.getMessage();
        }
    }
}
