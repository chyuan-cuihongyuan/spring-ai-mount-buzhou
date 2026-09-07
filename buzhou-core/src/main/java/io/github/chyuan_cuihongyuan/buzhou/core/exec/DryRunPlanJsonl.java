package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * 干跑计划 JSONL 导出（spec 328 / T647，导出族新员——ModelCostLedgerJsonl
 * 同形）：一行一 {@link DryRunHook.PlannedCall}（toolCallId/tool/args）。
 * args 以<b>字符串快照列</b>写入（String.valueOf）——计划单是人审口径非
 * 机器回放，不可序列化值不炸导出（Observability 降级哲学的整体化）。
 * 空计划零行诚实；dropped &gt; 0 时追加 meta 尾行——截断可见。
 */
public final class DryRunPlanJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DryRunPlanJsonl() {
    }

    /** 导出全部计划条目（顺序即拦截序）；返回数据行数（不含 meta 尾行）。 */
    public static long export(DryRunHook hook, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (DryRunHook.PlannedCall call : hook.plan()) {
                gen.writeStartObject();
                gen.writeStringField("toolCallId", call.toolCallId());
                gen.writeStringField("tool", call.toolName());
                gen.writeStringField("args", String.valueOf(call.arguments()));
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
            if (hook.droppedCount() > 0) {
                gen.writeStartObject();
                gen.writeBooleanField("meta", true);
                gen.writeNumberField("dropped", hook.droppedCount());
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
            }
        }
        return lines;
    }
}
