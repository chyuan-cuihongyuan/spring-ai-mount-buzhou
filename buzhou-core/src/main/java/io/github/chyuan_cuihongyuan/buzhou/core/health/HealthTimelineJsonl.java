package io.github.chyuan_cuihongyuan.buzhou.core.health;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 健康时间线 JSONL 导出（spec 405 / T702）：逐变迁追加一行
 * {at, mechanism, from, to}——每行 flush（tail -f 可观察）；IO 失败吞 +
 * 计数（旁路语义不放大——明细缺行好过主链故障）；{@link #close()} 关句柄。
 */
public final class HealthTimelineJsonl implements Consumer<HealthTimeline.Entry>, AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FAILED_COUNTER = "buzhou.health.timeline.jsonl-failed";

    private final Path path;
    private final BufferedWriter writer;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    /** 打开（父目录自动创建；打开失败上抛——启动期 fail-fast，坏路径该红）。 */
    public HealthTimelineJsonl(Path path) throws IOException {
        this.path = Objects.requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void accept(HealthTimeline.Entry entry) {
        if (entry == null) {
            return;
        }
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("at", entry.at().toString());
        line.put("mechanism", entry.mechanism());
        line.put("from", entry.from() == null ? "" : entry.from().name());
        line.put("to", entry.to() == null ? "" : entry.to().name());
        try {
            writer.write(MAPPER.writeValueAsString(line));
            writer.newLine();
            writer.flush();
            written.incrementAndGet();
        } catch (Exception e) {
            failed.incrementAndGet();
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter(FAILED_COUNTER);
        }
    }

    public Path path() {
        return path;
    }

    public long written() {
        return written.get();
    }

    public long failed() {
        return failed.get();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
