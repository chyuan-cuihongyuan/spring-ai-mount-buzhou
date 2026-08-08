package io.github.chyuan_cuihongyuan.buzhou.spill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class CopyFileTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FileSandbox writeSandbox;
    private final List<Path> additionalReadableRoots;

    public CopyFileTool(FileSandbox writeSandbox, List<Path> additionalReadableRoots) {
        this.writeSandbox = writeSandbox;
        this.additionalReadableRoots = (additionalReadableRoots == null ? List.<Path>of() : additionalReadableRoots)
                .stream().map(SessionReadOnlyRegistry::normalize).toList();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("copy_file")
                .description("复制文件生成工作副本。要编辑只读快照/只读区的内容，先复制到工作区再改。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "srcPath":{"type":"string","description":"来源路径（可为只读区）"},
                          "destPath":{"type":"string","description":"目标路径（限沙箱工作区）"},
                          "overwrite":{"type":"boolean","description":"目标已存在时是否覆盖，默认 false"}
                        },"required":["srcPath","destPath"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String srcRaw = args.path("srcPath").asText("");
            String destRaw = args.path("destPath").asText("");
            boolean overwrite = args.path("overwrite").asBoolean(false);
            Path src = resolveSource(srcRaw);
            if (!Files.isRegularFile(src)) {
                return "copy_file 失败：来源文件不存在：" + srcRaw;
            }
            Path dest = writeSandbox.resolveForWrite(destRaw);
            if (Files.exists(dest) && !overwrite) {
                return "copy_file 失败：目标已存在（如需覆盖请设 overwrite=true）：" + dest;
            }
            if (dest.getParent() != null) {
                Files.createDirectories(dest.getParent());
            }
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            return "已复制到工作副本：" + dest;
        } catch (Exception e) {
            return "copy_file 失败：" + e.getMessage();
        }
    }

    private Path resolveSource(String raw) {
        try {
            return writeSandbox.resolve(raw);
        } catch (io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException e) {
            Path candidate = SessionReadOnlyRegistry.normalize(Path.of(raw));
            boolean readable = additionalReadableRoots.stream().anyMatch(candidate::startsWith);
            if (!readable) {
                throw e;
            }
            return candidate;
        }
    }
}
