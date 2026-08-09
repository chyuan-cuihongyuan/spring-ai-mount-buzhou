package io.github.chyuan_cuihongyuan.buzhou.observability.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouObservabilityAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> assertThat(ctx).hasBean("observabilityRuntimeConfig"));
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.observability.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean("observabilityRuntimeConfig"));
    }
}
