package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-04 / T30 入参 schema 校验器：required/type/enum/items/边界 + permissive 语义
 * （未知关键字忽略、schema 缺失放行、宁漏报不误拦）。
 */
class ToolArgsValidatorTest {

    private static final String SCHEMA = """
            {"type":"object","properties":{
              "orderId":{"type":"string","minLength":3},
              "count":{"type":"integer","minimum":1,"maximum":10},
              "mode":{"type":"string","enum":["fast","slow"]},
              "tags":{"type":"array","items":{"type":"string"}},
              "nested":{"type":"object","properties":{"deep":{"type":"boolean"}},"required":["deep"]}
            },"required":["orderId"]}""";

    @Test
    void missingRequiredFieldFails() {
        Optional<String> error = ToolArgsValidator.validate(SCHEMA, "{\"count\":1}");
        assertThat(error).isPresent();
        assertThat(error.get()).contains("缺少必填字段「orderId」");
    }

    @Test
    void typeMismatchAndBoundsFail() {
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"count\":\"many\"}"))
                .hasValueSatisfying(e -> assertThat(e).contains("期望 type=integer"));
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"count\":99}"))
                .hasValueSatisfying(e -> assertThat(e).contains("大于 maximum=10.0"));
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"ab\"}"))
                .hasValueSatisfying(e -> assertThat(e).contains("minLength"));
    }

    @Test
    void enumViolationAndNestedAndItemsFail() {
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"mode\":\"normal\"}"))
                .hasValueSatisfying(e -> assertThat(e).contains("enum"));
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"tags\":[\"a\",7]}"))
                .hasValueSatisfying(e -> assertThat(e).contains("$.tags[1]"));
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"nested\":{}}"))
                .hasValueSatisfying(e -> assertThat(e).contains("缺少必填字段「deep」"));
    }

    @Test
    void validArgumentsPass() {
        assertThat(ToolArgsValidator.validate(SCHEMA,
                "{\"orderId\":\"abc\",\"count\":3,\"mode\":\"fast\",\"tags\":[\"a\"],\"nested\":{\"deep\":true}}"))
                .isEmpty();
        // 可选字段缺省 = 通过；integer 接受整值
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\"}")).isEmpty();
    }

    @Test
    void permissiveSemanticsNeverOverblock() {
        // schema 缺失 / 空 / 非 object → 放行
        assertThat(ToolArgsValidator.validate(null, "{\"any\":1}")).isEmpty();
        assertThat(ToolArgsValidator.validate("{}", "{\"any\":1}")).isEmpty();
        assertThat(ToolArgsValidator.validate("not-json", "{\"any\":1}")).isEmpty();
        // 未知关键字忽略
        assertThat(ToolArgsValidator.validate(
                "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\",\"format\":\"uri\"}},\"x-custom\":1}",
                "{\"a\":\"not-a-uri-but-format-ignored\"}")).isEmpty();
        // 额外字段默认允许（additionalProperties 未声明 false 时不拦）
        assertThat(ToolArgsValidator.validate(SCHEMA, "{\"orderId\":\"abc\",\"extra\":true}")).isEmpty();
    }

    @Test
    void invalidJsonArgumentsFail() {
        assertThat(ToolArgsValidator.validate(SCHEMA, "not-json"))
                .hasValueSatisfying(e -> assertThat(e).contains("不是合法 JSON"));
    }
}
