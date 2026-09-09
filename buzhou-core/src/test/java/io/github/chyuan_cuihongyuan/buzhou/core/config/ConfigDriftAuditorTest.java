package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 414 §Testing / T719–T720：配置漂移——首拍基线零事件；值变更 from/to；
 * 新增/删除键语义；敏感值掩码不外泄；yml 装配默认关。
 */
class ConfigDriftAuditorTest {

    private static final class MutableEnv extends StandardEnvironment {
        final Map<String, Object> props;

        MutableEnv(Map<String, Object> props) {
            this.props = props;
            getPropertySources().addFirst(new MapPropertySource("test", props));
        }
    }

    @Test
    void shouldBaselineFirstPoll_andDetectValueChangeOnNextPoll() {
        Map<String, Object> props = new java.util.HashMap<>(Map.of(
                "buzhou.model-name", "m1", "buzhou.api-token", "sk-secret-123"));
        MutableEnv env = new MutableEnv(props);
        List<ConfigDriftAuditor.Change> heard = new CopyOnWriteArrayList<>();
        ConfigDriftAuditor auditor = new ConfigDriftAuditor(env, Duration.ofSeconds(30),
                heard::addAll);
        Instant t0 = Instant.parse("2026-09-08T00:00:00Z");

        assertThat(auditor.pollOnce(t0)).isEmpty(); // 首拍基线
        assertThat(heard).isEmpty();

        props.put("buzhou.model-name", "m2"); // 值变更
        props.put("buzhou.new-key", "v");      // 新增
        props.remove("buzhou.api-token");      // 删除

        List<ConfigDriftAuditor.Change> changes = auditor.pollOnce(t0.plusSeconds(30));
        assertThat(changes).hasSize(3);
        // 掩码后值：api-token 从 *** 删除到 (unset)——真实值不外泄
        assertThat(changes).anySatisfy(c -> {
            assertThat(c.key()).isEqualTo("buzhou.model-name");
            assertThat(c.from()).isEqualTo("m1");
            assertThat(c.to()).isEqualTo("m2");
        });
        assertThat(changes).anySatisfy(c -> {
            assertThat(c.key()).isEqualTo("buzhou.new-key");
            assertThat(c.from()).isEqualTo("(unset)");
        });
        assertThat(changes).anySatisfy(c -> {
            assertThat(c.key()).isEqualTo("buzhou.api-token");
            assertThat(c.to()).isEqualTo("(unset)");
            assertThat(c.from()).isEqualTo("***"); // 掩码——删除前也是掩码值
        });
        assertThat(heard).hasSize(3); // listener 收到同一批

        // 再拍无变更零事件
        assertThat(auditor.pollOnce(t0.plusSeconds(60))).isEmpty();
    }

    @Test
    void shouldMaskSensitiveValuesInSnapshot() {
        MutableEnv env = new MutableEnv(Map.of(
                "buzhou.store.password", "hunter2",
                "buzhou.model-name", "plain"));
        ConfigDriftAuditor auditor = new ConfigDriftAuditor(env, Duration.ofSeconds(30), null);
        Map<String, String> snapshot = auditor.snapshot();
        assertThat(snapshot).containsEntry("buzhou.store.password", "***")
                .containsEntry("buzhou.model-name", "plain");
    }

    @Test
    void shouldAssembleFromYml_onlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.config-audit.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouConfigDriftAuditor");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouConfigDriftAuditor");
                });
    }
}
