package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提示词使用快照 JSONL 导出（spec 424 / T740，418/421 追加快照同族）：
 * {@link #appendSnapshot(Path, PromptUsageStats)} 把当前统计行追加落盘
 * （{at,name,version,count}；快照不清零——趋势对比用两次快照差）。打开/写
 * 失败上抛——导出是显式动作该红。宿主用定时任务周期落盘（DelayedJobQueue
 * 组合即得）。
 *
 * <p>spec 642 / T934：4 参重载带大小轮转（append 前静态检查——默认 64MB×3
 * 保护；≤0 显式关）；既有 2 参面 = 默认轮转参数。
 *
 * <p>spec 644 / T938：行序列化走 Jackson（spec 60 纪律——name 含引号/换行
 * 天然转义，绝不手工拼接）。
 */
public final class PromptUsageJsonl {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private PromptUsageJsonl() {
    }

    /** 追加一轮快照（返回写入行数；spec 642 默认轮转参数）。 */
    public static int appendSnapshot(Path path, PromptUsageStats stats) throws IOException {
        return appendSnapshot(path, stats,
                io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_BYTES,
                io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_HISTORY);
    }

    /** spec 642：带轮转参数的快照追加（maxBytes/maxHistory ≤ 0 = 关轮转）。 */
    public static int appendSnapshot(Path path, PromptUsageStats stats,
            long maxBytes, int maxHistory) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Instant at = Instant.now();
        var rows = stats.snapshot();
        // 行体量先算（轮转判定用估算上界——行即写定长字段，UTF-8 估算足够）
        long incoming = rows.stream().mapToLong(r -> r.name().length() + 64L).sum();
        io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.rotateIfNeeded(
                absolute, incoming, maxBytes, maxHistory);
        try (BufferedWriter writer = Files.newBufferedWriter(absolute,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (PromptUsageStats.Row row : rows) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("at", at.toString());
                line.put("name", row.name());
                line.put("version", row.version());
                line.put("count", row.count());
                writer.write(MAPPER.writeValueAsString(line));
                writer.newLine();
            }
        }
        return rows.size();
    }
}
