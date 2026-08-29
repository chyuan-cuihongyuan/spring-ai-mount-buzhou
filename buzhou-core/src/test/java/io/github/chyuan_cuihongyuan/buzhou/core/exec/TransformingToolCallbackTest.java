package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 169 / T534：结果裁剪回归——变换生效 / 异常回退 / null 空白回退 /
 * 组合叠加 / 定义透传与参数校验。
 */
class TransformingToolCallbackTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 提 JSON 顶层字段的典型宿主变换（解析失败抛 RuntimeException——走 fail-open）。 */
    private static final UnaryOperator<String> JSON_FIELD = raw -> {
        try {
            JsonNode node = MAPPER.readTree(raw);
            return node.has("data") ? node.get("data").toString() : null;
        } catch (Exception e) {
            throw new IllegalStateException("parse failed", e);
        }
    };

    private ToolCallback constantTool(String name, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    @Test
    void transformExtractsField() {
        TransformingToolCallback tool = TransformingToolCallback.wrap(
                constantTool("weather", "{\"city\":\"沪\",\"data\":{\"temp\":31}}"),
                JSON_FIELD);
        assertThat(tool.call("{}")).isEqualTo("{\"temp\":31}");
    }

    @Test
    void transformFailureFallsBackToRaw() {
        TransformingToolCallback tool = TransformingToolCallback.wrap(
                constantTool("raw", "非 JSON 原文"),
                raw -> {
                    throw new IllegalStateException("parse failed");
                });
        assertThat(tool.call("{}")).isEqualTo("非 JSON 原文");
    }

    @Test
    void nullOrBlankTransformOutputFallsBackToRaw() {
        String raw = "{\"city\":\"沪\"}";
        TransformingToolCallback nullOut = TransformingToolCallback.wrap(
                constantTool("t1", raw), r -> null);
        assertThat(nullOut.call("{}")).isEqualTo(raw);

        TransformingToolCallback blankOut = TransformingToolCallback.wrap(
                constantTool("t2", raw), r -> "   ");
        assertThat(blankOut.call("{}")).isEqualTo(raw);
    }

    @Test
    void transformsComposeOuterSeesInnerFallback() {
        // 内层抛异常回退原文 → 外层拿到原文正常变换（fail-open 不吞层）
        TransformingToolCallback tool = TransformingToolCallback.wrap(
                TransformingToolCallback.wrap(
                        constantTool("t", "{\"data\":\"价值\"}"),
                        r -> {
                            throw new IllegalArgumentException("inner boom");
                        }),
                JSON_FIELD);
        assertThat(tool.call("{}")).isEqualTo("\"价值\"");
    }

    @Test
    void definitionPassesThroughAndArgumentsValidate() {
        TransformingToolCallback tool = TransformingToolCallback.wrap(
                constantTool("mytool", "x"), r -> r);
        assertThat(tool.getToolDefinition().name()).isEqualTo("mytool");

        assertThatThrownBy(() -> TransformingToolCallback.wrap(null, r -> r))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TransformingToolCallback.wrap(constantTool("t", "x"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
