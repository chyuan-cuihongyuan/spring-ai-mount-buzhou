package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbes;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouProbesEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 332 / impl-355：探针端点装配回归——归类绑定 + 端点在（actuator 面）+
 * 缺省全 readiness + 幽灵机制装配期红。
 */
class BuzhouProbesAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void probesAndEndpointAssemble_defaultAllReadiness() {
        runner.withBean("storeHealth", BuzhouHealth.class, StubStoreHealth::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(BuzhouProbes.class);
                    assertThat(context).hasSingleBean(BuzhouProbesEndpoint.class);
                    BuzhouProbesEndpoint endpoint = context.getBean(BuzhouProbesEndpoint.class);
                    Map<String, Object> payload = endpoint.probeVerdicts();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> readiness =
                            (Map<String, Object>) payload.get("readiness");
                    assertThat(readiness.get("mechanisms")).asInstanceOf(
                            org.assertj.core.api.InstanceOfAssertFactories.LIST)
                            .contains("store"); // 未点名 → readiness
                    assertThat(readiness.get("status")).isEqualTo("UP");
                });
    }

    @Test
    void ymlNamingBindsClasses() {
        runner.withPropertyValues(
                "buzhou.health.probes.liveness-mechanisms[0]=leak",
                "buzhou.health.probes.startup-mechanisms[0]=warmup")
                .withBean("leakHealth", BuzhouHealth.class, StubLeakHealth::new)
                .withBean("warmupHealth", BuzhouHealth.class, StubWarmupHealth::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    BuzhouProbes probes = context.getBean(BuzhouProbes.class);
                    assertThat(probes.classOf("leak")).isEqualTo(BuzhouProbes.ProbeClass.LIVENESS);
                    assertThat(probes.classOf("warmup")).isEqualTo(BuzhouProbes.ProbeClass.STARTUP);
                    BuzhouProbesEndpoint endpoint = context.getBean(BuzhouProbesEndpoint.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = endpoint.probeVerdicts();
                    assertThat(payload).containsKeys("liveness", "readiness", "startup");
                });
    }

    @Test
    void ghostMechanismFailsAtAssembly() {
        runner.withPropertyValues("buzhou.health.probes.liveness-mechanisms[0]=ghost")
                .withBean("storeHealth", BuzhouHealth.class, StubStoreHealth::new)
                .run(context -> assertThat(context).hasFailed());
    }

    static final class StubStoreHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "store";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }

    static final class StubLeakHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "leak";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }

    static final class StubWarmupHealth implements BuzhouHealth {
        @Override
        public String mechanism() {
            return "warmup";
        }

        @Override
        public Status status() {
            return Status.UP;
        }
    }
}
