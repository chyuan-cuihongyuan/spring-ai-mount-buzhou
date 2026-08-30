package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.session.ObservabilityJsonlExporter;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 194 §B / T556：组合小轮红队——exportManifestGzip 解压与明文逐字节一致
 * （gzip 族管线）；VirtualKeys.resetAll 整窗换窗（用量+耗尽态清、限额保留）。
 */
class ManifestGzipAndResetAllTest {

    @Test
    void manifestGzipRoundTripsIdentical() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        ((InMemoryObservabilityStore) stores.observabilityStore()).saveSpans(
                List.of(new SpanRecord("c1", null, "sess-c", 1, "MODEL",
                        "chat", Instant.parse("2026-08-29T00:00:00Z"),
                        Instant.parse("2026-08-29T00:00:01Z"), SpanStatus.OK, Map.of())));
        ObservabilityJsonlExporter exporter = new ObservabilityJsonlExporter(
                stores.observabilityStore());

        java.io.StringWriter plain = new java.io.StringWriter();
        assertThat(exporter.exportManifest(plain)).isEqualTo(1);
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        assertThat(exporter.exportManifestGzip(gzipped)).isEqualTo(1);

        String inflated = new String(
                new GZIPInputStream(new ByteArrayInputStream(gzipped.toByteArray()))
                        .readAllBytes(), StandardCharsets.UTF_8);
        assertThat(inflated).isEqualTo(plain.toString()); // 逐字节一致
    }

    @Test
    void resetAllClearsUsageAndExhaustionButKeepsLimits() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("a", 100);
        keys.register("b", 100);
        keys.trySpend("a", 100); // 耗尽
        assertThat(keys.isExhausted("a")).isTrue();

        keys.resetAll();
        assertThat(keys.isExhausted("a")).isFalse();
        assertThat(keys.usage("a").usedTokens()).isZero();
        assertThat(keys.trySpend("b", 100)).isTrue(); // 限额保留——窗口内照常扣
        assertThat(keys.distinct()).isEqualTo(2);
    }
}
