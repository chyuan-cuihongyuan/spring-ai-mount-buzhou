package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * 成本归因报表 JSONL 导出（spec 334 / T660；导出族新员）：一行一归因行
 * {@code {"dimension":"VIRTUAL_KEY","value":"...","microUsd":N,"usd":"...",
 * "shareBp":N}}——microUsd 整数列为精确口径，usd 字符串列为人读口径，
 * shareBp 万分比。export → reset 循环 = 每窗口一份归因报表（导出族同
 * 纪律）。空账零行诚实。
 */
public final class CostAttributionJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CostAttributionJsonl() {
    }

    /** 导出一个维度的全部归因行（rollup 同序）；返回行数。 */
    public static long export(CostAttributionLedger ledger,
            CostAttributionLedger.Dimension dimension, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (CostAttributionLedger.Attribution attribution : ledger.rollup(dimension)) {
                gen.writeStartObject();
                gen.writeStringField("dimension", attribution.dimension().name());
                gen.writeStringField("value", attribution.value());
                gen.writeNumberField("microUsd", attribution.microUsd());
                gen.writeStringField("usd", attribution.usd());
                gen.writeNumberField("shareBp", attribution.shareBp());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
        }
        return lines;
    }
}
