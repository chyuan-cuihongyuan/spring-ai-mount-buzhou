package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.config.ConfigDoctor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 107 §B / T392：配置体检健康段红队——就绪前 UNKNOWN（pending）；就绪后 UP +
 * errors/warnings/checkedKeys 缓存；端点聚合段。spec 91 fog 收口。
 * spec 142 §B / T467：陈旧度——无 TTL 零变化；TTL 过期转 UNKNOWN(stale) +
 * reexamine 手动刷新恢复；非正 TTL fail-fast。借鉴 Consul TTL check。
 */
class ConfigDoctorHealthTest {

    @Test
    void unknownBeforeReadyThenUpWithCachedCounts() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("buzhou.bulkhead.enabld", "true"); // 拼错键 → warning
        ConfigDoctorHealth health = new ConfigDoctorHealth(env);

        assertThat(health.mechanism()).isEqualTo("config-doctor");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsEntry("pending", true);

        health.onApplicationEvent(readyEvent());

        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("errors", 0)
                .containsEntry("warnings", 1)
                .containsEntry("checkedKeys", 1);
    }

    @Test
    void endpointAggregatesConfigDoctorSection() {
        MockEnvironment env = new MockEnvironment();
        ConfigDoctorHealth health = new ConfigDoctorHealth(env);
        health.onApplicationEvent(readyEvent());

        BuzhouHealthEndpoint endpoint = new BuzhouHealthEndpoint(List.of(health));
        Map<String, Object> snapshot = endpoint.buzhouSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> mechanisms = (Map<String, Object>) snapshot.get("mechanisms");
        assertThat(mechanisms).containsKey("config-doctor");
        @SuppressWarnings("unchecked")
        Map<String, Object> section = (Map<String, Object>) mechanisms.get("config-doctor");
        assertThat(section.get("status")).isEqualTo("UP");
    }

    @Test
    void defaultNoTtlNeverGoesStale() throws Exception {
        MockEnvironment env = new MockEnvironment();
        ConfigDoctorHealth health = new ConfigDoctorHealth(env);
        health.onApplicationEvent(readyEvent());
        Thread.sleep(50);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP); // 既有行为零变化
        assertThat(health.details()).containsEntry("examinedAt",
                health.details().get("examinedAt")); // 时刻面在场
    }

    @Test
    void ttlExpiryTransitionsToUnknownStaleAndReexamineRecovers() throws Exception {
        MockEnvironment env = new MockEnvironment();
        ConfigDoctorHealth health = new ConfigDoctorHealth(env, java.time.Duration.ofMillis(40));
        health.onApplicationEvent(readyEvent());
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("stale", false);

        Thread.sleep(80); // 超过 TTL
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsEntry("stale", true)
                .containsEntry("freshnessTtlMs", 40L);

        health.reexamine(); // 手动刷新面：TTL 的逃逸口
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details()).containsEntry("stale", false);
    }

    @Test
    void nonPositiveTtlRejectedFailFast() {
        MockEnvironment env = new MockEnvironment();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new ConfigDoctorHealth(env, java.time.Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new ConfigDoctorHealth(env, java.time.Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ApplicationReadyEvent readyEvent() {
        return new ApplicationReadyEvent(
                new org.springframework.boot.SpringApplication(ConfigDoctor.class),
                new String[0],
                new org.springframework.context.annotation.AnnotationConfigApplicationContext(),
                java.time.Duration.ZERO);
    }
}
