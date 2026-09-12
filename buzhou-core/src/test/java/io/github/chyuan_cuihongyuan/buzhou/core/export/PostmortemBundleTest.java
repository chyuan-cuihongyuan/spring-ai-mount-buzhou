package io.github.chyuan_cuihongyuan.buzhou.core.export;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.CostAttributionLedger;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.health.HealthTimeline;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 521 / T793–794：事故复盘一键包——标准命名源打包（timeline/signatures/
 * cost 双维）、源缺席条目跳过、ZIP/manifest 落位、summary 汇总行。
 */
class PostmortemBundleTest {

    @Test
    void composeWritesStandardSourcesAndSummary() throws Exception {
        HealthTimeline timeline = new HealthTimeline();
        timeline.record(Map.of("model-circuit", BuzhouHealth.Status.UP), Instant.EPOCH);
        ErrorSignatures signatures = ErrorSignatures.create();
        signatures.record("tool", new IllegalStateException("connection reset"));
        signatures.record("tool", new IllegalStateException("connection reset"));
        CostAttributionLedger ledger = CostAttributionLedger.create();
        ledger.record("gpt-4o", "vk-a", 5000);

        PostmortemBundle bundle = new PostmortemBundle()
                .timeline(timeline).signatures(signatures).costLedger(ledger);
        Path zip = Files.createTempFile("postmortem", ".zip");
        List<ExportBundle.ManifestEntry> manifest = bundle.compose(zip);

        assertThat(Files.size(zip)).isGreaterThan(0);
        assertThat(manifest).extracting(ExportBundle.ManifestEntry::name)
                .contains("postmortem.timeline.jsonl", "postmortem.error-signatures.jsonl",
                        "postmortem.summary.json");
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            assertThat(zf.getEntry("postmortem.timeline.jsonl")).isNotNull();
            ZipEntry summary = zf.getEntry("postmortem.summary.json");
            String summaryText = new String(zf.getInputStream(summary).readAllBytes());
            assertThat(summaryText).contains("timelineEntries");
            assertThat(summaryText).contains("costTotalMicroUsd");
        }
        Files.deleteIfExists(zip);
    }

    @Test
    void absentSourcesSkippedNotFailing() throws Exception {
        PostmortemBundle bundle = new PostmortemBundle(); // 无任何源
        Path zip = Files.createTempFile("postmortem-empty", ".zip");
        List<ExportBundle.ManifestEntry> manifest = bundle.compose(zip);
        // 缺席源跳过——只剩 summary（317 单源故障隔离同语义）
        assertThat(manifest).hasSize(1);
        assertThat(manifest.getFirst().name()).isEqualTo("postmortem.summary.json");
        Files.deleteIfExists(zip);
    }
}
