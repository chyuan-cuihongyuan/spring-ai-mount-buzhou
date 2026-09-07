package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

/**
 * 模型成本账单 OLAP JSONL 导出（spec 188 §A / T548；导出族第八员）：
 * 成本排行平铺一行一 JSON（{@code {"model":"...","microUsd":N,"usd":"..."}}）
 * ——microUsd 整数列为精确口径，usd 字符串列为人读口径（6 位小数）。
 * export → reset 循环 = 每窗口一份账单（spec 121 同纪律）。空表零行诚实。
 */
public final class ModelCostLedgerJsonl {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ModelCostLedgerJsonl() {
    }

    /** 导出全部在册成本（与 topByCost 同序）；返回行数。 */
    public static long export(ModelCostLedger ledger, Writer out) throws IOException {
        long lines = 0;
        try (JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
            for (ModelCostLedger.ModelCost cost : ledger.topByCost(Integer.MAX_VALUE)) {
                gen.writeStartObject();
                gen.writeStringField("model", cost.model());
                gen.writeNumberField("microUsd", cost.microUsd());
                gen.writeStringField("usd", java.math.BigDecimal
                        .valueOf(cost.microUsd(), 6).toPlainString());
                // spec 314 / T620：价目快照随单（最近一次记账时单价——调价后旧账可复算）
                ModelCostLedger.PricingSnapshot pricing = ledger.pricingOf(cost.model());
                if (pricing != null) {
                    gen.writeNumberField("inputPerMillion", pricing.inputPerMillion());
                    gen.writeNumberField("outputPerMillion", pricing.outputPerMillion());
                }
                gen.writeEndObject();
                gen.flush();
                out.write('\n');
                lines++;
            }
        }
        return lines;
    }
}
