package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * PII 命中报表 OLAP JSONL 导出（spec 166 §A / T519，spec 164 fog「报表导出」
 * 收口；ErrorSignaturesJsonl 同族第五员后的第六员）：合规命中排行平铺一行一
 * JSON（{@code {"name":"...","count":N}}）——「哪类敏感数据最常出现」进
 * DuckDB/ClickHouse 按窗口时序分析。export → reset 循环 = 每窗口一份合规
 * 报表（spec 121 同纪律）。静态面（stats 即数据源）；空表零行诚实。
 */
public final class PiiHitStatsJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PiiHitStatsJsonl() {
    }

    /** 导出全部在册命中（与 top() 同序：count 降序 + 名字典序）；返回行数。 */
    public static long export(PiiHitStats stats, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (PiiHitStats.Hit hit : stats.top(Integer.MAX_VALUE)) {
                gen.writeStartObject();
                gen.writeStringField("name", hit.name());
                gen.writeNumberField("count", hit.count());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
        }
        return lines;
    }
}
