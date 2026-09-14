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
import java.util.concurrent.atomic.AtomicLong;

/**
 * read_file — 整读沙箱内文件（无害，默认开）。
 *
 * <p>不内建 offset/limit：范围读取归 {@code read_range}（spec 06 推演 #2，瘦 Schema 原则）。
 * 超阈值结果由 Spill offload Hook 统一处理，本工具不截断。
 */
@BuzhouTool(name = "read_file", idempotent = true)
public class ReadFileTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** impl-49：单文件读入上限（8MB）；超限走分段/spill，不整读进堆。 */
    static final long MAX_READ_BYTES = 8L * 1024 * 1024;

    private final FileSandbox sandbox;

    // —— spec 1047 / impl 799：读量水位与拒绝分桶（Datadog DogStatsD read/write 对称计量
    // 思想；静态面理由同 WriteFileStats R46 先例）。守恒：attempts = reads + 三拒绝桶之和。
    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong READS = new AtomicLong();
    private static final AtomicLong BYTES_READ = new AtomicLong();
    private static final AtomicLong NOT_FILE_REJECTS = new AtomicLong();
    private static final AtomicLong OVERSIZE_REJECTS = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    /** 读量水位与拒绝分桶快照（spec 1047）。 */
    public record ReadFileStats(long attempts, long reads, long bytesRead,
                                long notFileRejects, long oversizeRejects, long failures) {

        /** 拒绝总数（三桶之和）。 */
        public long totalRejects() {
            return notFileRejects + oversizeRejects + failures;
        }
    }

    /** 只读快照（守恒 attempts = reads + totalRejects()——每入口恰落一桶）。 */
    public static ReadFileStats stats() {
        return new ReadFileStats(ATTEMPTS.get(), READS.get(), BYTES_READ.get(),
                NOT_FILE_REJECTS.get(), OVERSIZE_REJECTS.get(), FAILURES.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        READS.set(0);
        BYTES_READ.set(0);
        NOT_FILE_REJECTS.set(0);
        OVERSIZE_REJECTS.set(0);
        FAILURES.set(0);
    }

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
        ATTEMPTS.incrementAndGet();
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String raw = args.path("path").asText("");
            Path path = sandbox.resolve(raw);
            if (!Files.isRegularFile(path)) {
                NOT_FILE_REJECTS.incrementAndGet();
                return "read_file 失败：文件不存在：" + raw;
            }
            // impl-49：读入上限预检——超限返回可读错误而非整读进堆（OOM 向量）
            long size = Files.size(path);
            if (size > MAX_READ_BYTES) {
                OVERSIZE_REJECTS.incrementAndGet();
                return "read_file 失败：文件 " + size + " 字节超过读入上限 " + MAX_READ_BYTES
                        + " 字节；请用 read_range 语义分段读取";
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            READS.incrementAndGet();
            BYTES_READ.addAndGet(content.getBytes(StandardCharsets.UTF_8).length);
            return content;
        } catch (Exception e) {
            FAILURES.incrementAndGet();
            return "read_file 失败：" + e.getMessage();
        }
    }
}
