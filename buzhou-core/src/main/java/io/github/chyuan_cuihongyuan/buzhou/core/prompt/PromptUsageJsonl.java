package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * 提示词使用快照 JSONL 导出（spec 424 / T740，418/421 追加快照同族）：
 * {@link #appendSnapshot(Path, PromptUsageStats)} 把当前统计行追加落盘
 * （{at,name,version,count}；快照不清零——趋势对比用两次快照差）。打开/写
 * 失败上抛——导出是显式动作该红。宿主用定时任务周期落盘（DelayedJobQueue
 * 组合即得）。
 */
public final class PromptUsageJsonl {

    private PromptUsageJsonl() {
    }

    /** 追加一轮快照（返回写入行数）。 */
    public static int appendSnapshot(Path path, PromptUsageStats stats) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Instant at = Instant.now();
        var rows = stats.snapshot();
        try (BufferedWriter writer = Files.newBufferedWriter(absolute,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (PromptUsageStats.Row row : rows) {
                writer.write("{\"at\":\"" + at + "\",\"name\":\"" + row.name()
                        + "\",\"version\":" + row.version()
                        + ",\"count\":" + row.count() + "}");
                writer.newLine();
            }
        }
        return rows.size();
    }
}
