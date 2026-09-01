package io.github.chyuan_cuihongyuan.buzhou.resilience.shadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

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

/**
 * 影子对照明细 JSONL 导出（spec 309 / T609，W&B lineage 借鉴——对照数据落盘
 * 可溯）：全局监听 {@code shadow.compared} 事件逐条追加 JSONL（at + payload
 * 全字段，字符串值节选封顶）。每行 flush（tail -f 可观察）；IO 失败吞 +
 * 计数（旁路语义不放大——明细缺行好过主链故障）；{@link #close()} 关句柄。
 */
public final class ShadowComparisonJsonl implements SessionEventListener, AutoCloseable {

    /** 字符串值节选封顶（防整文倾倒撑爆明细文件）。 */
    static final int EXCERPT_CAP = 1024;
    private static final String FAILED_COUNTER = "buzhou.shadow.jsonl-failed";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path path;
    private final BufferedWriter writer;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    /** 打开（父目录自动创建；打开失败上抛——启动期 fail-fast，坏路径该红）。 */
    public ShadowComparisonJsonl(Path path) throws IOException {
        this.path = Objects.requireNonNull(path, "path");
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void onEvent(SessionEvent event) {
        if (event == null || !ShadowTrafficController.EVENT_COMPARED.equals(event.type())) {
            return; // 只导对照明细——其他事件不落盘
        }
        String line;
        try {
            line = MAPPER.writeValueAsString(lineOf(event));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            countFailure();
            return;
        }
        try {
            synchronized (this) {
                writer.write(line);
                writer.write('\n');
                writer.flush();
            }
            written.incrementAndGet();
        } catch (IOException e) {
            countFailure(); // 吞——旁路语义：明细缺行好过主链故障
        }
    }

    private Map<String, Object> lineOf(SessionEvent event) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("at", event.occurredAt().toString());
        event.payload().forEach((key, value) ->
                out.put(key, value instanceof String text ? excerpt(text) : value));
        return out;
    }

    static String excerpt(String text) {
        return text.length() <= EXCERPT_CAP ? text : text.substring(0, EXCERPT_CAP);
    }

    private void countFailure() {
        failed.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter(FAILED_COUNTER, 1);
    }

    /** 已落盘行数（观测/测试面）。 */
    public long written() {
        return written.get();
    }

    /** 落盘失败次数（观测/测试面）。 */
    public long failed() {
        return failed.get();
    }

    /** 明细文件路径（观测面）。 */
    public Path path() {
        return path;
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            writer.close();
        }
    }
}
