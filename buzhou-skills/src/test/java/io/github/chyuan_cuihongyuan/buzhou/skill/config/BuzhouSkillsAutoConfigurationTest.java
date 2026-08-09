package io.github.chyuan_cuihongyuan.buzhou.skill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillResourceResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.skill.SkillModule;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouSkillsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouSkillsAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SkillModule.class);
            assertThat(ctx).hasSingleBean(SkillCatalogRenderer.class);
            assertThat(ctx).hasSingleBean(SkillResourceResolver.class);
        });
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.skills.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SkillModule.class));
    }
}
