package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 543 / T827：span 状态分布——kind×status 计数、大小写归一、null
 * status 归 UNSET、store 便捷重载。
 */
class SpanStatusDistributionTest {

    private static SpanRecord span(String kind, String status) {
        return new SpanRecord("sp", null, "s1", 1, kind, "n",
                Instant.EPOCH, Instant.EPOCH, status, Map.of());
    }

    @Test
    void countsByKindAndStatusNormalized() {
        var dist = SpanStatusDistribution.analyze(List.of(
                span("TOOL", "OK"), span("TOOL", "ERROR"),
                span("tool", "ok"), span("TURN", "OK"),
                span("MODEL", null)));
        assertThat(dist.get("TOOL").get("OK")).isEqualTo(2);
        assertThat(dist.get("TOOL").get("ERROR")).isEqualTo(1);
        assertThat(dist.get("TURN").get("OK")).isEqualTo(1);
        assertThat(dist.get("MODEL").get("UNSET")).isEqualTo(1);
    }

    @Test
    void storeOverloadReadsSingleSession() {
        ObservabilityStore store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou
                .inMemoryStores().observabilityStore();
        store.saveSpans(List.of(span("TOOL", "OK"), span("TOOL", "ERROR")));
        var dist = SpanStatusDistribution.analyze(store, "s1");
        assertThat(dist.get("TOOL").get("OK")).isEqualTo(1);
        assertThat(dist.get("TOOL").get("ERROR")).isEqualTo(1);
    }

    @Test
    void nullSpansFailFast() {
        List<SpanRecord> nullList = null;
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> SpanStatusDistribution.analyze(nullList))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
