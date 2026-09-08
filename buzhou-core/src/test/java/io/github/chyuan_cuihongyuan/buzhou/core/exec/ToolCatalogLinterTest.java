package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 420 §Testing / T731–T732：目录 lint——三规则正负样本；clean 零发现
 * 零事件；重复装配事件去重；yml 装配默认关。
 */
class ToolCatalogLinterTest {

    private static ToolCallback tool(String name, String description) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name(name).description(description).inputSchema("{}").build();
            }

            @Override
            public String call(String input) {
                return "ok";
            }
        };
    }

    @Test
    void shouldDetectAllThreeRules_andStayQuietOnCleanCatalog() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        ToolCatalogLinter linter = new ToolCatalogLinter(events::add);

        // 坏名字
        assertThat(linter.lint(tool("Bad-Name", "这是一个足够长的描述"), List.of()))
                .extracting(ToolCatalogLinter.Finding::rule)
                .containsExactly(ToolCatalogLinter.RULE_NAME);
        // 过短描述
        assertThat(linter.lint(tool("good_name", "短"), List.of()))
                .extracting(ToolCatalogLinter.Finding::rule)
                .containsExactly(ToolCatalogLinter.RULE_DESC);
        // 过长描述
        assertThat(linter.lint(tool("long_desc", "x".repeat(5000)), List.of()))
                .extracting(ToolCatalogLinter.Finding::rule)
                .containsExactly(ToolCatalogLinter.RULE_DESC);
        // 跨源重名（同名不同实例）
        ToolCallback first = tool("dup", "这是一个足够长的描述");
        assertThat(linter.lint(tool("dup", "这是另一个足够长的描述"), List.of(first)))
                .extracting(ToolCatalogLinter.Finding::rule)
                .containsExactly(ToolCatalogLinter.RULE_DUP);
        // clean：零发现
        assertThat(linter.lint(tool("clean_tool", "这是一个干净工具的充分描述"), List.of()))
                .isEmpty();

        // announce：有发现才发事件；同会话第二次装配去重
        List<ToolCatalogLinter.Finding> findings = List.of(
                new ToolCatalogLinter.Finding("dup", ToolCatalogLinter.RULE_DUP, "同名"));
        linter.announce("s1", findings);
        linter.announce("s1", findings);
        linter.announce("s2", findings);
        assertThat(events).hasSize(2); // s1 一次 + s2 一次
        assertThat(events.get(0).type()).isEqualTo(ToolCatalogLinter.EVENT_LINT);
        assertThat(events.get(0).payload()).containsEntry("findings", 1);
    }

    @Test
    void shouldAssembleFromYml_onlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.tools.catalog-lint.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouToolCatalogLintRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouToolCatalogLintRuntimeConfig");
                });
    }
}
