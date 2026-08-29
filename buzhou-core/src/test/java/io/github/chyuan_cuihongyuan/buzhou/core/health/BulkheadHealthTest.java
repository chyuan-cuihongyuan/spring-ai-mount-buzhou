package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 92 §B / T350：隔离舱健康面红队——未配置 UNKNOWN（disabled 详情，严格 DOWN
 * 纪律：未启用≠DOWN）；配置后 UP + per-agent limit/inFlight 有界详情；名额持有期间
 * inFlight 反映；端点聚合段。
 */
class BulkheadHealthTest {

    @AfterEach
    void cleanup() {
        AgentBulkhead.install(null);
    }

    @Test
    void unlimitedBulkheadIsUnknownWithDisabledDetails() {
        BulkheadHealth health = new BulkheadHealth(AgentBulkhead.unlimited());

        assertThat(health.mechanism()).isEqualTo("bulkhead");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsEntry("disabled", true);
    }

    @Test
    void configuredBulkheadIsUpWithPerAgentDetails() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("hot", 2, "cold", 1), Duration.ZERO);
        BulkheadHealth health = new BulkheadHealth(bulkhead);

        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsKey("agents");
        @SuppressWarnings("unchecked")
        Map<String, Object> agents = (Map<String, Object>) health.details().get("agents");
        assertThat(agents).containsKeys("hot", "cold");
        assertThat(agents.get("cold")).isEqualTo("inFlight=0/limit=1");

        // 名额持有期间 inFlight 反映
        try (AgentBulkhead.Lease lease = bulkhead.acquire("hot")) {
            assertThat(agents.get("hot")).isEqualTo("inFlight=0/limit=2"); // 详情是取时快照
            assertThat(bulkhead.inFlight("hot")).isEqualTo(1);
        }
    }

    @Test
    void endpointAggregatesBulkheadSection() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("hot", 1), Duration.ZERO);
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(
                List.of(new BulkheadHealth(bulkhead)));

        Map<String, Object> snapshot = endpoint.buzhouSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> mechanisms = (Map<String, Object>) snapshot.get("mechanisms");
        assertThat(mechanisms).containsKey("bulkhead");
        @SuppressWarnings("unchecked")
        Map<String, Object> section = (Map<String, Object>) mechanisms.get("bulkhead");
        assertThat(section.get("status")).isEqualTo("UP");
    }
}
