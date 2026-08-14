package io.github.chyuan_cuihongyuan.buzhou.tools.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.tools.ToolsModule;
import io.github.chyuan_cuihongyuan.buzhou.tools.todo.TodoAttachmentRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouToolsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BuzhouCoreAutoConfiguration.class, BuzhouToolsAutoConfiguration.class))
            .withBean(ChatModel.class, ScriptedChatModel::new);

    @Test
    void enabledByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ToolsModule.class);
            // todo 默认开 → TodoAttachmentRenderer 注册（AttachmentRenderer 可能还有其他模块贡献
            // （如 impl-45 runawayBudgetRenderer），多 renderer 经 CompositeAttachmentRenderer 组合）
            assertThat(ctx).hasSingleBean(TodoAttachmentRenderer.class);
        });
    }

    @Test
    void disabledWhenSwitchedOff() {
        runner.withPropertyValues("buzhou.tools.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ToolsModule.class));
    }

    @Test
    void todoRendererAbsentWhenTodoDisabled() {
        runner.withPropertyValues("buzhou.tools.todo.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(TodoAttachmentRenderer.class);
                    // 非 tools 模块贡献的 renderer（如 runawayBudgetRenderer）不受本开关影响
                    assertThat(ctx).hasBean("runawayBudgetRenderer");
                });
    }
}
