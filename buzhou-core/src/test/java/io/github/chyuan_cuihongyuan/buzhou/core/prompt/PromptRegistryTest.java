package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 401 §Testing / T693–T694：提示词注册表——publish 单调 + latest 自动
 * 重指；label 晋级/回滚 + 钉版 + fail-fast；yml 播种幂等 + label 指针 +
 * 未配置空注册表。
 */
class PromptRegistryTest {

    @Test
    void shouldPublishMonotonicVersionsAndAutoRepointLatest() {
        PromptRegistry registry = new InMemoryPromptRegistry();
        PromptVersion v1 = registry.publish("greet", "你是助手 v1", null);
        PromptVersion v2 = registry.publish("greet", "你是助手 v2", "语气调整");

        assertThat(v1.version()).isEqualTo(1);
        assertThat(v2.version()).isEqualTo(2);
        assertThat(registry.versions("greet")).hasSize(2);
        assertThat(registry.resolve("greet")).contains(v2); // latest 自动重指
        assertThat(registry.labels("greet")).containsEntry("latest", 2);
        // 不可改写：v1 快照不随发布变化
        assertThat(registry.resolveVersion("greet", 1)).contains(v1);
        assertThat(v1.body()).isEqualTo("你是助手 v1");
    }

    @Test
    void shouldRepointLabelsForPromoteAndRollbackAndPinByNumber() {
        PromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("summarize", "三段式总结", null);
        registry.publish("summarize", "两段式总结", null);

        // 晋级：production → v1；再发布 v3 不动 production
        registry.label("summarize", "production", 1);
        registry.publish("summarize", "一段式总结", null);
        assertThat(registry.resolve("summarize", "production"))
                .hasValueSatisfying(v -> assertThat(v.body()).isEqualTo("三段式总结"));

        // 回滚：production 重指 v1（指针移动不产生新版本）
        registry.label("summarize", "production", 1);
        assertThat(registry.versions("summarize")).hasSize(3);
        assertThat(registry.labels("summarize")).containsEntry("production", 1);

        // 钉版：按号取不受指针影响
        Optional<PromptVersion> pinned = registry.resolveVersion("summarize", 2);
        assertThat(pinned).hasValueSatisfying(v -> assertThat(v.body()).isEqualTo("两段式总结"));

        // fail-fast：未知名/未知版本/空标签
        assertThatThrownBy(() -> registry.label("nope", "production", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.label("summarize", "production", 99))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.label("summarize", " ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(registry.resolve("missing")).isEmpty();
        assertThat(registry.names()).containsExactly("summarize");
    }

    @Test
    void shouldSeedIdempotentlyFromYml_whenDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.prompt.templates[0].name=greet",
                        "buzhou.prompt.templates[0].body=你好 v1",
                        "buzhou.prompt.templates[0].label=production",
                        // 同名同 body：幂等跳过（重启模拟——不掀版本）
                        "buzhou.prompt.templates[1].name=greet",
                        "buzhou.prompt.templates[1].body=你好 v1",
                        "buzhou.prompt.templates[2].name=greet",
                        "buzhou.prompt.templates[2].body=你好 v2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    PromptRegistry registry = context.getBean(PromptRegistry.class);
                    assertThat(registry.versions("greet")).hasSize(2); // v2 只有一条
                    // label 只随「声明它的条目」走：[0] 声明时指向其当时最新（v1），
                    // [2] 未声明 label 故 production 不动
                    assertThat(registry.resolve("greet", "production"))
                            .hasValueSatisfying(v -> assertThat(v.body()).isEqualTo("你好 v1"));
                    assertThat(registry.resolve("greet"))
                            .hasValueSatisfying(v -> assertThat(v.body()).isEqualTo("你好 v2"));
                });
    }

    @Test
    void shouldProvideEmptyRegistry_whenUnconfigured() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PromptRegistry.class).names()).isEmpty();
                });
    }
}
