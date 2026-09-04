package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 342 / impl-365：维护 cordon 装配回归——bean 恒在（按钮预先在场）/
 * yml 窗绑定 / 非法窗红。
 */
class MaintenanceCordonAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void beansAlwaysPresent_noWindowNoBehavior() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SpawnAdmissionFloor.class);
            assertThat(context).hasSingleBean(MaintenanceCordon.class);
            MaintenanceCordon cordon = context.getBean(MaintenanceCordon.class);
            assertThat(cordon.cordoned()).isFalse(); // 无窗零行为
            assertThat(context.getBean(SpawnAdmissionFloor.class).get().name())
                    .isEqualTo("LOW");
        });
    }

    @Test
    void ymlWindowBinds() {
        runner.withPropertyValues(
                "buzhou.maintenance.from=2099-01-01T00:00:00Z",
                "buzhou.maintenance.until=2099-01-01T01:00:00Z",
                "buzhou.maintenance.reason=未来升级窗",
                "buzhou.maintenance.poll-interval=30s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MaintenanceCordon cordon = context.getBean(MaintenanceCordon.class);
                    assertThat(cordon.cordoned()).isFalse(); // 窗未到不追溯
                    assertThat(cordon.view()).containsEntry("reason", "null");
                });
    }

    @Test
    void invalidWindowFailsFast() {
        runner.withPropertyValues(
                "buzhou.maintenance.from=2099-01-01T01:00:00Z",
                "buzhou.maintenance.until=2099-01-01T00:00:00Z") // from ≥ until
                .run(context -> assertThat(context).hasFailed());
    }
}
