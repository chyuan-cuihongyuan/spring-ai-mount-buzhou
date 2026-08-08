package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * read_file — 整读沙箱内文件（无害，默认开）。
 *
 * <p>不内建 offset/limit：范围读取归 {@code read_range}（spec 06 推演 #2，瘦 Schema 原则）。
 * 超阈值结果由 Spill offload Hook 统一处理，本工具不截断。
 */
@BuzhouTool(name = "read_file", idempotent = true)
public class ReadFileTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileSandbox sandbox;

    public ReadFileTool(FileSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("read_file")
                .description("读取沙箱内文件全文。超长结果会自动落盘并给回读指针，届时用 read_range 分段续读。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "path":{"type":"string","description":"沙箱 root 相对路径，或白名单内绝对路径"}
                        },"required":["path"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String raw = args.path("path").asText("");
            Path path = sandbox.resolve(raw);
            if (!Files.isRegularFile(path)) {
                return "read_file 失败：文件不存在：" + raw;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "read_file 失败：" + e.getMessage();
        }
    }
}
