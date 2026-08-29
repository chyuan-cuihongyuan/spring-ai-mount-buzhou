package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 109 §B / T398：观测导出 gzip 红队——gzip 字节可解回；解压内容与未压缩导出
 * 逐行等值；结果计数一致；压缩确有收益（gzip 字节 < 明文）。spec 60/67 压缩面。
 */
class ObservabilityGzipExportTest {

    private static BuzhouStores seeded() {
        BuzhouStores stores = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores();
        for (int i = 1; i <= 5; i++) {
            String sid = "s-gzip-" + i;
            stores.observabilityStore().saveSpans(java.util.List.of(new SpanRecord(
                    UUID.randomUUID().toString(), null, sid, i, "TURN", "turn-" + i,
                    Instant.now(), Instant.now(), "OK", Map.of("i", i))));
        }
        return stores;
    }

    @Test
    void gzipRoundTripsToIdenticalJsonl() throws Exception {
        BuzhouStores stores = seeded();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(
                stores.observabilityStore());

        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        ObservabilityJsonlExporter.JsonlExportResult gzipResult =
                exporter.exportAllGzip(gzipped);
        StringWriter plain = new StringWriter();
        ObservabilityJsonlExporter.JsonlExportResult plainResult =
                exporter.exportAll(plain);

        // 解压内容逐行等值
        String unzipped = new String(new GZIPInputStream(
                new ByteArrayInputStream(gzipped.toByteArray())).readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(unzipped).isEqualTo(plain.toString());
        assertThat(gzipResult.spans()).isEqualTo(plainResult.spans()).isEqualTo(5);
        assertThat(gzipResult.sessions()).isEqualTo(plainResult.sessions()).isEqualTo(5);

        // 压缩确有收益（5 行量小但重复结构——gzip 压得住）
        assertThat(gzipped.size()).isLessThan(plain.toString().getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void sessionGzipMatchesPlainSessionExport() throws Exception {
        BuzhouStores stores = seeded();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(
                stores.observabilityStore());

        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        exporter.exportSessionGzip("s-gzip-1", gzipped);
        StringWriter plain = new StringWriter();
        exporter.exportSession("s-gzip-1", plain);

        String unzipped = new String(new GZIPInputStream(
                new ByteArrayInputStream(gzipped.toByteArray())).readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(unzipped).isEqualTo(plain.toString());
    }

    @Test
    void sinceVariantKeepsWaterlineSemantics() throws Exception {
        BuzhouStores stores = seeded();
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(
                stores.observabilityStore());
        Instant epoch = Instant.EPOCH;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObservabilityJsonlExporter.JsonlExportResult result =
                exporter.exportAllSinceGzip(out, epoch);

        String unzipped = new String(new GZIPInputStream(
                new ByteArrayInputStream(out.toByteArray())).readAllBytes(),
                StandardCharsets.UTF_8);
        assertThat(result.spans()).isEqualTo(5); // EPOCH 起全量
        assertThat(unzipped.lines()).hasSize(5);
    }
}
