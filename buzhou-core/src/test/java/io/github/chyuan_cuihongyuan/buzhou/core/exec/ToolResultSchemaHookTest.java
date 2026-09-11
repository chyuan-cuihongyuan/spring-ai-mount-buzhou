package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 409 §Testing / T709–T710：结果 schema——通过零改写（引用等）；缺键/
 * 类型错/非 JSON 反馈；无声明与错误路径不校验；反馈含标记；yml 装配默认关。
 */
class ToolResultSchemaHookTest {

    private static final String SCHEMA =
            "{\"type\":\"object\",\"required\":[\"city\",\"temp\"],"
            + "\"properties\":{\"city\":{\"type\":\"string\"},"
            + "\"temp\":{\"type\":\"number\"}}}";

    private static DefaultToolCallContext ctx(String tool, Object result) {
        DefaultToolCallContext c = new DefaultToolCallContext(
                new HookEnvironment("s", "a", new InMemorySessionStateStore()),
                "c1", tool, Map.of());
        c.replaceResult(result);
        return c;
    }

    @Test
    void shouldPassThroughUnchanged_whenSchemaSatisfiedOrNotDeclared() {
        ToolResultSchemaHook hook = new ToolResultSchemaHook(Map.of("weather", SCHEMA));
        DefaultToolCallContext ok = ctx("weather", "{\"city\":\"沪\",\"temp\":23.5}");
        String before = String.valueOf(ok.result());
        hook.afterTool(ok);
        assertThat(String.valueOf(ok.result())).isSameAs(before); // 通过零改写

        DefaultToolCallContext undeclared = ctx("other", "随便什么散文");
        String raw = String.valueOf(undeclared.result());
        hook.afterTool(undeclared);
        assertThat(String.valueOf(undeclared.result())).isSameAs(raw); // 未声明零变化
    }

    @Test
    void shouldReplaceWithStructuredFeedback_onViolation() {
        ToolResultSchemaHook hook = new ToolResultSchemaHook(Map.of("weather", SCHEMA));

        DefaultToolCallContext missing = ctx("weather", "{\"city\":\"沪\"}"); // 缺 temp
        hook.afterTool(missing);
        assertThat(String.valueOf(missing.result()))
                .contains(ToolValidationFeedback.MARKER)
                .contains("结果未过 schema，工具已执行")
                .contains("temp");

        DefaultToolCallContext wrongType = ctx("weather", "{\"city\":\"沪\",\"temp\":\"热\"}");
        hook.afterTool(wrongType);
        assertThat(String.valueOf(wrongType.result()))
                .contains(ToolFeedbackType.VALIDATION_FAILURE.marker())
                .contains("期望 type=number，实际 string");

        DefaultToolCallContext notJson = ctx("weather", "今天天气不错"); // 非 JSON
        hook.afterTool(notJson);
        assertThat(String.valueOf(notJson.result())).contains("不是合法 JSON");

        // 错误路径不校验（error 非 null——result 不被改写保持 null）
        DefaultToolCallContext withError = erroredProxy();
        hook.afterTool(withError);
        assertThat(withError.result()).isNull();
    }

    private DefaultToolCallContext erroredProxy() {
        return new DefaultToolCallContext(
                new HookEnvironment("s", "a", new InMemorySessionStateStore()),
                "c2", "weather", Map.of()) {
            @Override public Throwable error() { return new RuntimeException("工具炸了"); }
        };
    }

    @Test
    void shouldAssembleFromYml_mapForm() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.tools.result-schemas.weather={\"type\":\"object\",\"required\":[\"city\"]}")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouToolResultSchemasRuntimeConfig");
                    // spec 531 装配审计：内容非空断言（根绑定修复回归——仅 hasBean 挡不住绑空）
                    var rc = (io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig)
                            context.getBean("buzhouToolResultSchemasRuntimeConfig");
                    var hook = rc.hooks().stream()
                            .filter(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultSchemaHook)
                            .map(h -> (io.github.chyuan_cuihongyuan.buzhou.core.exec.ToolResultSchemaHook) h)
                            .findFirst().orElseThrow();
                    assertThat(hook.schemasCount()).isEqualTo(1);
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouToolResultSchemasRuntimeConfig");
                });
    }
}
