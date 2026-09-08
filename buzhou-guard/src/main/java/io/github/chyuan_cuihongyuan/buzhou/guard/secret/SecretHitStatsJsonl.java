package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * 秘密命中快照 JSONL 导出（spec 418 / T728，PiiHitStatsJsonl 同族）：
 * {@link #appendSnapshot(Path)} 把当前统计行追加落盘（{at,name,side,count}；
 * 快照不清零——趋势对比用两次快照差）。打开/写失败上抛——导出是显式
 * 动作该红。宿主用定时任务周期落盘（DelayedJobQueue 组合即得）。
 */
public final class SecretHitStatsJsonl {

    private SecretHitStatsJsonl() {
    }

    /** 追加一轮快照（返回写入行数）。 */
    public static int appendSnapshot(Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Instant at = Instant.now();
        var hits = SecretHitStats.global().snapshot();
        try (BufferedWriter writer = Files.newBufferedWriter(absolute,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            for (SecretHitStats.Hit hit : hits) {
                writer.write("{\"at\":\"" + at + "\",\"name\":\"" + hit.name()
                        + "\",\"side\":\"" + hit.side() + "\",\"count\":" + hit.count() + "}");
                writer.newLine();
            }
        }
        return hits.size();
    }
}
