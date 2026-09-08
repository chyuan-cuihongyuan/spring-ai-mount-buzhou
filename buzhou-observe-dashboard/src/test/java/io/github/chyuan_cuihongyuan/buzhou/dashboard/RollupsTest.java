package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 412 §Testing / T715–T716：时间桶预聚合——跨会话聚合数值；epoch 对齐
 * + 空桶补齐 + 升序；桶数上界；时长解析双形态。
 */
class RollupsTest {

    private static final Instant T0 = Instant.parse("2026-09-08T10:00:00Z");

    private static SpanRecord span(String id, String sid, String kind, long startMs,
            String status, Map<String, Object> attrs) {
        return new SpanRecord(id, null, sid, 1, kind, kind.toLowerCase(),
                T0.plusMillis(startMs), T0.plusMillis(startMs + 100), status, attrs);
    }

    @Test
    void shouldAggregateAcrossSessionsWithEpochAlignedEmptyBuckets() {
        ObservabilityStore store = io.github.chyuan_cuihongyuan.buzhou.core.Buzhou
                .inMemoryStores().observabilityStore();
        // s1：桶 0（10:00:00）内 1 turn + 1 model + 1 tool-error
        store.saveSpans(List.of(
                span("a1", "s1", "TURN", 0, "OK", Map.of()),
                span("a2", "s1", "MODEL_CALL", 10_000, "OK", Map.of(
                        "usage.prompt_tokens", 100, "usage.completion_tokens", 20)),
                span("a3", "s1", "TOOL_CALL", 20_000, "ERROR", Map.of())));
        // s2：桶 2（10:02）内 1 model-error（token 也计）
        store.saveSpans(List.of(
                span("b1", "s2", "MODEL_CALL", 120_000, "ERROR", Map.of(
                        "usage.prompt_tokens", 5, "usage.completion_tokens", 0))));
        DashboardQueryService service = new DashboardQueryService(store);

        List<DashboardQueryService.TimeBucket> buckets = service.rollups(
                T0, T0.plus(Duration.ofMinutes(4)), Duration.ofMinutes(1));

        assertThat(buckets).hasSize(4); // 空桶补齐
        assertThat(buckets).extracting(DashboardQueryService.TimeBucket::start)
                .containsExactly(T0, T0.plusSeconds(60), T0.plusSeconds(120),
                        T0.plusSeconds(180)); // 升序 + epoch 对齐（T0 恰为整分）
        DashboardQueryService.TimeBucket first = buckets.get(0);
        assertThat(first.turns()).isEqualTo(1);
        assertThat(first.modelCalls()).isEqualTo(1);
        assertThat(first.toolCalls()).isEqualTo(1);
        assertThat(first.errors()).isEqualTo(1); // tool error
        assertThat(first.promptTokens()).isEqualTo(100);
        assertThat(first.completionTokens()).isEqualTo(20);

        DashboardQueryService.TimeBucket third = buckets.get(2);
        assertThat(third.modelCalls()).isEqualTo(1);
        assertThat(third.errors()).isEqualTo(1); // model error
        assertThat(third.promptTokens()).isEqualTo(5);

        DashboardQueryService.TimeBucket empty = buckets.get(1);
        assertThat(empty.turns()).isZero();
        assertThat(empty.promptTokens()).isZero();
    }

    @Test
    void shouldFailFastWhenBucketCountExceedsCap() {
        DashboardQueryService service = new DashboardQueryService(
                io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores().observabilityStore());
        assertThatThrownBy(() -> service.rollups(Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-08T00:00:00Z"), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超上界");
        assertThatThrownBy(() -> service.rollups(T0, T0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class); // to > from
    }

    @Test
    void shouldParseBucketShorthandAndIso() {
        assertThat(io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer
                .parseBucket("5m")).isEqualTo(Duration.ofMinutes(5));
        assertThat(io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer
                .parseBucket("1h")).isEqualTo(Duration.ofHours(1));
        assertThat(io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer
                .parseBucket("30s")).isEqualTo(Duration.ofSeconds(30));
        assertThat(io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer
                .parseBucket("2d")).isEqualTo(Duration.ofDays(2));
        assertThat(io.github.chyuan_cuihongyuan.buzhou.dashboard.internal.DashboardHttpServer
                .parseBucket("PT30M")).isEqualTo(Duration.ofMinutes(30));
        assertThatThrownBy(() -> io.github.chyuan_cuihongyuan.buzhou.dashboard.internal
                .DashboardHttpServer.parseBucket("5x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> io.github.chyuan_cuihongyuan.buzhou.dashboard.internal
                .DashboardHttpServer.parseBucket(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
