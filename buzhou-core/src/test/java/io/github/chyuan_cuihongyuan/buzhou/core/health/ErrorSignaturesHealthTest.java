package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 85 §B / T326：错误签名健康面红队——恒 UP（观测面不 DOWN）；top-5 有界 +
  count 降序；空表零错误详情可用；快照端点聚合（BuzhouHealthEndpoint 含该段）。
 */
class ErrorSignaturesHealthTest {

    @AfterEach
    void cleanup() {
        ErrorSignatures.install(null);
    }

    @Test
    void alwaysUpWithBoundedTopDetails() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "boom A");
        registry.record("tool", "boom A");
        registry.record("model", "boom B");
        ErrorSignaturesHealth health = new ErrorSignaturesHealth(registry);

        assertThat(health.mechanism()).isEqualTo("error-signatures");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("distinct", 2);
        assertThat(health.details().get("top"))
                .isEqualTo(List.of("tool:boom A x2", "model:boom B x1"));
    }

    @Test
    void emptyRegistryYieldsUsableEmptyDetails() {
        ErrorSignaturesHealth health = new ErrorSignaturesHealth(ErrorSignatures.create());

        assertThat(health.details()).containsEntry("distinct", 0);
        assertThat((List<?>) health.details().get("top")).isEmpty();
    }

    @Test
    void endpointAggregatesErrorSignaturesSection() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "boom A");
        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(
                List.of(new ErrorSignaturesHealth(registry)));

        Map<String, Object> snapshot = endpoint.buzhouSnapshot();

        @SuppressWarnings("unchecked")
        Map<String, Object> mechanisms = (Map<String, Object>) snapshot.get("mechanisms");
        assertThat(mechanisms).containsKey("error-signatures");
        @SuppressWarnings("unchecked")
        Map<String, Object> section = (Map<String, Object>) mechanisms.get("error-signatures");
        assertThat(section.get("status")).isEqualTo("UP");
    }
}
