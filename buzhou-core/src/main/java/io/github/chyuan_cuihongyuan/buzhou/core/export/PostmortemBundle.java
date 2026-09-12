package io.github.chyuan_cuihongyuan.buzhou.core.export;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.CostAttributionLedger;
import io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimeline;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 事故复盘一键包（spec 521 / T793——317 ExportBundle 合流打包的事故域
 * 预设组合）：一个调用产出标准复盘 ZIP——机制状态变迁时间线（405）+
 * 错误签名族 top（83）+ 成本双维 rollup（334）。源可缺席（缺席条目跳过
 * ——317 单源故障隔离同语义）。
 *
 * <p>诚实边界：只聚合既有观测面的快照（不采集新数据）；行内容为既有
 * 有界面（timeline 256 封顶/签名 256 封顶/rollup 64 值封顶）。
 */
public final class PostmortemBundle {

    private HealthTimeline timeline;
    private ErrorSignatures signatures;
    private CostAttributionLedger costLedger;

    public PostmortemBundle timeline(HealthTimeline timeline) {
        this.timeline = timeline;
        return this;
    }

    public PostmortemBundle signatures(ErrorSignatures signatures) {
        this.signatures = signatures;
        return this;
    }

    public PostmortemBundle costLedger(CostAttributionLedger costLedger) {
        this.costLedger = costLedger;
        return this;
    }

    /** 打包标准复盘 ZIP（manifest 首条目对账——317 同口径）。 */
    public List<ExportBundle.ManifestEntry> compose(Path zip) throws java.io.IOException {
        LinkedHashMap<String, ExportBundle.ExportSource> sources = new LinkedHashMap<>();
        if (timeline != null) {
            sources.put("postmortem.timeline.jsonl", out -> {
                long lines = 0;
                for (HealthTimeline.Entry e : timeline.entries()) {
                    out.write(jsonLine(
                            quote("mechanism", e.mechanism()),
                            quote("from", String.valueOf(e.from())),
                            quote("to", String.valueOf(e.to())),
                            quote("at", String.valueOf(e.at()))) + "\n");
                    lines++;
                }
                return lines;
            });
        }
        if (signatures != null) {
            sources.put("postmortem.error-signatures.jsonl", out -> {
                long lines = 0;
                for (Map.Entry<String, Long> e : signatures.top(256)) {
                    out.write(jsonLine(
                            quote("signature", e.getKey()),
                            quote("count", String.valueOf(e.getValue()))) + "\n");
                    lines++;
                }
                return lines;
            });
        }
        if (costLedger != null) {
            for (CostAttributionLedger.Dimension dimension : CostAttributionLedger.Dimension.values()) {
                String name = "postmortem.cost-" + dimension.name().toLowerCase()
                        .replace('_', '-') + ".jsonl";
                sources.put(name, out -> {
                    long lines = 0;
                    for (CostAttributionLedger.Attribution a : costLedger.rollup(dimension)) {
                        out.write(jsonLine(
                                quote("dimension", a.dimension().name()),
                                quote("value", a.value()),
                                quote("microUsd", String.valueOf(a.microUsd()))) + "\n");
                        lines++;
                    }
                    return lines;
                });
            }
        }
        sources.put("postmortem.summary.json", out -> {
            out.write("{\n"
                    + "  \"timelineEntries\": " + (timeline == null ? 0 : timeline.entries().size()) + ",\n"
                    + "  \"signatureFamilies\": " + (signatures == null ? 0 : signatures.top(256).size()) + ",\n"
                    + "  \"costTotalMicroUsd\": " + (costLedger == null ? 0
                            : costLedger.totalMicroUsd(CostAttributionLedger.Dimension.MODEL)) + "\n"
                    + "}\n");
            return 1;
        });
        return ExportBundle.bundle(zip, sources);
    }

    /** 极简 JSON 行拼装（值域为既有有界面——机制名/枚举/计数；引号转义兜底）。 */
    private static String jsonLine(String... keyValuePairs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValuePairs.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(keyValuePairs[i]);
        }
        return sb.append('}').toString();
    }

    private static String quote(String key, String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + key + "\":\"" + safe + "\"";
    }
}
