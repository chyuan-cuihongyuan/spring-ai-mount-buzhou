package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * 错误签名 OLAP JSONL 导出（spec 112 §A / T405，spec 83 fog 项收口；导出四族
 * spec 88/94/60/67 的第五员）：top 错误族平铺一行一 JSON
 * （{@code {"signature":"...","count":N}}）——错误族趋势进 DuckDB/ClickHouse
 * 与观测/评估表按时间轴 join。静态面（registry 即数据源）；空表零行。
 */
public final class ErrorSignaturesJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorSignaturesJsonl() {
    }

    /** 导出全部在册签名（count 降序——与 top() 同序）；返回行数。 */
    public static long export(ErrorSignatures registry, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (Map.Entry<String, Long> entry : registry.snapshot().entrySet()) {
                gen.writeStartObject();
                gen.writeStringField("signature", entry.getKey());
                gen.writeNumberField("count", entry.getValue());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
        }
        return lines;
    }
}
