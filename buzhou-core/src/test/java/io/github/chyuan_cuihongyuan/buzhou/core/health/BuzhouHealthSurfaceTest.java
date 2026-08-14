package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 健康面：端点快照聚合 + HealthIndicator 三态映射 + 异常详情兜底
 * （DOWN 仅当核心职能不可用；禁用报 UNKNOWN——聚合不误 DOWN）。
 */
class BuzhouHealthSurfaceTest {

    private static BuzhouHealth of(String mechanism, BuzhouHealth.Status status,
            Map<String, Object> details) {
        return new BuzhouHealth() {
            @Override
            public String mechanism() {
                return mechanism;
            }

            @Override
            public Status status() {
                return status;
            }

            @Override
            public Map<String, Object> details() {
                return details;
            }
        };
    }

    @Test
    void endpointAggregatesMechanismSnapshots() {
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(List.of(
                of("memory", BuzhouHealth.Status.UP, Map.of("probe", "state-store-roundtrip")),
                of("spill", BuzhouHealth.Status.DOWN, Map.of("rootDir", "/no/such/dir")),
                of("guard", BuzhouHealth.Status.UNKNOWN, Map.of("auditEnabled", false))));
        Map<String, Object> snapshot = endpoint.buzhouSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> mechanisms =
                (Map<String, Map<String, Object>>) snapshot.get("mechanisms");
        assertThat(mechanisms).containsKeys("memory", "spill", "guard");
        assertThat(mechanisms.get("memory").get("status")).isEqualTo("UP");
        assertThat(mechanisms.get("spill").get("status")).isEqualTo("DOWN");
        assertThat(mechanisms.get("guard").get("status")).isEqualTo("UNKNOWN");
        assertThat(mechanisms.get("spill").get("details"))
                .isEqualTo(Map.of("rootDir", "/no/such/dir"));
    }

    @Test
    void indicatorMapsThreeStatuses() {
        assertThat(new BuzhouHealthIndicator(
                of("m", BuzhouHealth.Status.UP, Map.of())).health().getStatus())
                .isEqualTo(Status.UP);
        assertThat(new BuzhouHealthIndicator(
                of("m", BuzhouHealth.Status.DOWN, Map.of())).health().getStatus())
                .isEqualTo(Status.DOWN);
        assertThat(new BuzhouHealthIndicator(
                of("m", BuzhouHealth.Status.UNKNOWN, Map.of())).health().getStatus())
                .isEqualTo(Status.UNKNOWN);
        // 详情异常不炸健康端点（detailsError 兜底）
        BuzhouHealth failingDetails = new BuzhouHealth() {
            @Override
            public String mechanism() {
                return "m";
            }

            @Override
            public BuzhouHealth.Status status() {
                return BuzhouHealth.Status.UP;
            }

            @Override
            public Map<String, Object> details() {
                throw new IllegalStateException("boom");
            }
        };
        Health health = new BuzhouHealthIndicator(failingDetails).health();
        assertThat(health.getDetails()).containsKey("detailsError");
    }
}
