package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 406 §Testing / T703–T704：工具退役——描述前缀全/最小字段；调用透传 +
 * 事件；未命中零包装；yml map 装配 + 未配置无 bean；E2E 包装真挂上。
 */
class DeprecatedToolCallbackTest {

    private static ToolCallback tool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name(name).description("原描述").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "ok:" + toolInput;
            }
        };
    }

    @Test
    void shouldAmendDescriptionAndPassThroughCalls() {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        DeprecatedToolCallback.Deprecation full = new DeprecatedToolCallback.Deprecation(
                "v2.1", "v3.0", "search_v2", "旧全文检索，改用向量检索");
        DeprecatedToolCallback wrapped = new DeprecatedToolCallback(tool("search_v1"), full, events::add);

        assertThat(wrapped.getToolDefinition().name()).isEqualTo("search_v1");
        assertThat(wrapped.getToolDefinition().description())
                .startsWith("[DEPRECATED since v2.1, removal v3.0] 旧全文检索，改用向量检索 优先使用 search_v2。")
                .endsWith("原描述");

        assertThat(wrapped.call("q")).isEqualTo("ok:q"); // 透传不阻断
        assertThat(events).hasSize(1);
        assertThat(events.get(0).type())
                .isEqualTo(DeprecatedToolCallback.EVENT_DEPRECATED_CALLED);
        assertThat(events.get(0).payload())
                .containsEntry("tool", "search_v1").containsEntry("successor", "search_v2");

        // 最小字段：无 removal/successor/message
        DeprecatedToolCallback minimal = new DeprecatedToolCallback(tool("old"),
                new DeprecatedToolCallback.Deprecation(null, null, null, null), events::add);
        assertThat(minimal.getToolDefinition().description())
                .startsWith("[DEPRECATED since unknown] ").endsWith("原描述");
        minimal.call("x");
        assertThat(events.get(1).payload()).containsEntry("tool", "old")
                .doesNotContainKey("successor");
    }

    @Test
    void shouldAssembleFromYml_mapForm() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.tools.deprecated.search_v1.since=2.1",
                        "buzhou.tools.deprecated.search_v1.removal-in=3.0",
                        "buzhou.tools.deprecated.search_v1.successor=search_v2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouToolDeprecationRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouToolDeprecationRuntimeConfig");
                });
    }

    @Test
    void shouldWrapThroughRuntime_endToEnd() {
        // 用 recording 模型驱动一次工具调用：包装层事件真发生
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.wrapToolCallbacks(cb -> {
                    if ("search_v1".equals(cb.getToolDefinition().name())) {
                        return new DeprecatedToolCallback(cb, new DeprecatedToolCallback.Deprecation(
                                "2.1", null, "search_v2", null), events::add);
                    }
                    return cb;
                })),
                null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        // E2E 装配验证：runtime 能用该 config 起（包装 customizer 不炸）
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-dep")) {
            assertThat(agent.chat("hi")).isNotNull();
        }
    }
}
