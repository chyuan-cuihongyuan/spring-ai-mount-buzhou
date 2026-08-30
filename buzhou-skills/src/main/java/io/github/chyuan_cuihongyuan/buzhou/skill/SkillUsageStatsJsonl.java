package io.github.chyuan_cuihongyuan.buzhou.skill;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * 技能使用报表 OLAP JSONL 导出（spec 170 §A / T524，spec 140 fog「导出半边」；
 * 导出族第七员）：使用排行平铺一行一 JSON（{@code {"skill":"...","loads":N}}）
 * ——「什么在被用、什么在吃灰」进 DuckDB/ClickHouse 按窗口时序分析。
 * export → reset 循环 = 每窗口一份热度榜（spec 121 同纪律）。空表零行诚实。
 */
public final class SkillUsageStatsJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SkillUsageStatsJsonl() {
    }

    /** 导出全部在册使用（与 topUsed 同序：count 降序 + 名字典序）；返回行数。 */
    public static long export(SkillUsageStats stats, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (SkillUsageStats.SkillUsage usage : stats.topUsed(Integer.MAX_VALUE)) {
                gen.writeStartObject();
                gen.writeStringField("skill", usage.skill());
                gen.writeNumberField("loads", usage.loads());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
        }
        return lines;
    }
}
