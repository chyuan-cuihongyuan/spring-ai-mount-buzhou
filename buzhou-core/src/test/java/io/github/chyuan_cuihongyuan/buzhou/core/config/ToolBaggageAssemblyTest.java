package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolBaggage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 337 / impl-360：行李装配回归——bean 恒在（运行时 API 预先在场）/
 * yml 播种生效 / 越限值启动红。
 */
class ToolBaggageAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void beanAlwaysPresent_emptyWithoutYml() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ToolBaggage.class);
            assertThat(context.getBean(ToolBaggage.class).isEmpty()).isTrue();
        });
    }

    @Test
    void ymlSeedsBaggage() {
        runner.withPropertyValues(
                "buzhou.tools.baggage.tenant=acme",
                "buzhou.tools.baggage.env=prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ToolBaggage baggage = context.getBean(ToolBaggage.class);
                    assertThat(baggage.view())
                            .containsEntry("tenant", "acme")
                            .containsEntry("env", "prod");
                });
    }

    @Test
    void oversizedValueFailsFast() {
        runner.withPropertyValues(
                "buzhou.tools.baggage.k=" + "x".repeat(ToolBaggage.MAX_VALUE_CHARS + 1))
                .run(context -> assertThat(context).hasFailed());
    }
}
