package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 512 / T775–T776：提示词模板严格渲染——变量抽取去重保序、缺失一次
 * 列全（StrictUndefined）、未闭合语法错、多余变量忽略、预检两态、注册表
 * 组合（Jinja2 StrictUndefined 借鉴）。
 */
class PromptTemplateTest {

    @Test
    void extractsVariablesDedupedInOrder() {
        List<String> vars = PromptTemplate.variables(
                "你好 {{user_name}}，你的订单 {{order_id}}（{{user_name}} 收）");
        assertThat(vars).containsExactly("user_name", "order_id");
    }

    @Test
    void renderFailsListingAllMissingAtOnce() {
        String template = "你好 {{a}} 和 {{b}}，共 {{c}}";
        assertThatThrownBy(() -> PromptTemplate.render(template, Map.of("a", "A")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("b").hasMessageContaining("c")
                .hasMessageContaining("StrictUndefined");
    }

    @Test
    void renderStrictUndefinedNeverLeaksPlaceholderSyntax() {
        // 静默失败面钉住：缺失变量绝不把 {{x}} 原样发给模型
        assertThatThrownBy(() -> PromptTemplate.render("值是 {{value}}", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void renderHappyPathIgnoresExtraVariablesAndWhitespace() {
        String out = PromptTemplate.render("你好 {{ name }}，共 {{count}} 件",
                Map.of("name", "张三", "count", 3, "extra", "忽略"));
        assertThat(out).isEqualTo("你好 张三，共 3 件");
    }

    @Test
    void unclosedPlaceholderIsSyntaxError() {
        assertThatThrownBy(() -> PromptTemplate.render("模板 {{name 缺闭合", Map.of("name", "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未闭合");
    }

    @Test
    void validatePreflightTwoStates() {
        var ok = PromptTemplate.validate("{{a}}{{b}}", Set.of("a", "b", "c"));
        assertThat(ok.ok()).isTrue();
        assertThat(ok.missing()).isEmpty();

        var bad = PromptTemplate.validate("{{a}}{{b}}", Set.of("a"));
        assertThat(bad.ok()).isFalse();
        assertThat(bad.missing()).containsExactly("b");
    }

    @Test
    void composesWithPromptRegistryPublishResolve() {
        PromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好 {{user_name}}，{{policy}}", "v1");
        var version = registry.resolve("greeting").orElseThrow();
        // 预检：数据行缺 policy → 出局
        assertThat(PromptTemplate.validate(version.body(), Set.of("user_name")).ok()).isFalse();
        // 渲染：变量齐 → 成品
        String rendered = PromptTemplate.render(version.body(),
                Map.of("user_name", "李四", "policy", "礼貌用语"));
        assertThat(rendered).isEqualTo("你好 李四，礼貌用语");
    }

    @Test
    void plainTextWithoutPlaceholdersPassesThrough() {
        assertThat(PromptTemplate.variables("普通提示词")).isEmpty();
        assertThat(PromptTemplate.render("普通提示词", Map.of())).isEqualTo("普通提示词");
    }
}
