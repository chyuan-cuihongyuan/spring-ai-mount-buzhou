package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillPairAudit;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * spill 配对完整性健康面（spec 728 / T1054 族——707 审计的接线，548 同型）：
 * mechanism=spill-pair；禁用报 UNKNOWN（BuzhouHealth 语义）；启用恒 UP——
 * 残缺 findings 是数据需关注非进程故障。
 *
 * <p>details 每次全目录树扫描（健康端点频率低可接受；超大目录宿主可降频）。
 */
public final class SpillPairHealth implements BuzhouHealth {

    private final boolean enabled;
    private final Path rootDir;

    public SpillPairHealth(boolean enabled, Path rootDir) {
        this.enabled = enabled;
        this.rootDir = rootDir;
    }

    @Override
    public String mechanism() {
        return "spill-pair";
    }

    @Override
    public Status status() {
        return enabled ? Status.UP : Status.UNKNOWN;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (!enabled) {
            details.put("disabled", true);
            return details;
        }
        SpillPairAudit.Report report = SpillPairAudit.audit(rootDir);
        long dataWithoutMeta = report.findings().stream()
                .filter(f -> f.kind().equals("DATA_WITHOUT_META")).count();
        long metaWithoutData = report.findings().stream()
                .filter(f -> f.kind().equals("META_WITHOUT_DATA")).count();
        details.put("dataFiles", (long) report.dataFiles());
        details.put("metaFiles", (long) report.metaFiles());
        details.put("dataBytes", report.dataBytes());
        details.put("dataWithoutMeta", dataWithoutMeta);
        details.put("metaWithoutData", metaWithoutData);
        return details;
    }
}
