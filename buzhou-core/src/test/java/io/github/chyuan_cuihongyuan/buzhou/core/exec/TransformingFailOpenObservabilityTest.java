package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 变换 fail-open 可观测测试（spec 635 / T920–T921 / impl 488）：
 * 抛异常/null/空白三路 fail-open 计数可读、原文照返、成功路径不计数。
 */
class TransformingFailOpenObservabilityTest {

    private static ToolCallback toolReturning(String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("weather").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }

            @Override
            public String call(String toolInput, ToolContext ctx) {
                return result;
            }
        };
    }

    /** 三路 fail-open：计数递增 + 原文照返；成功路径不计数。 */
    @Test
    void failOpenPathsCountedAndOriginalReturned() {
        TransformingToolCallback throwing = TransformingToolCallback.wrap(
                toolReturning("{\"temp\":25}"), raw -> {
                    throw new IllegalStateException("字段名笔误");
                });
        assertThat(throwing.call("{}")).isEqualTo("{\"temp\":25}");
        assertThat(throwing.call("{}")).isEqualTo("{\"temp\":25}");
        assertThat(throwing.failOpenCount()).isEqualTo(2);

        TransformingToolCallback nullReturn = TransformingToolCallback.wrap(
                toolReturning("raw"), raw -> null);
        assertThat(nullReturn.call("{}")).isEqualTo("raw");
        assertThat(nullReturn.failOpenCount()).isEqualTo(1);

        TransformingToolCallback blank = TransformingToolCallback.wrap(
                toolReturning("raw"), raw -> "   ");
        assertThat(blank.call("{}")).isEqualTo("raw");
        assertThat(blank.failOpenCount()).isEqualTo(1);

        TransformingToolCallback ok = TransformingToolCallback.wrap(
                toolReturning("big-json"), raw -> "提炼");
        assertThat(ok.call("{}")).isEqualTo("提炼");
        assertThat(ok.failOpenCount()).isZero();
    }

    /** 空原文短路（不算失败）；wrap null 校验不变。 */
    @Test
    void emptyShortCircuitAndValidation() {
        TransformingToolCallback empty = TransformingToolCallback.wrap(
                toolReturning(""), raw -> {
                    throw new IllegalStateException("不应到达");
                });
        assertThat(empty.call("{}")).isEmpty();
        assertThat(empty.failOpenCount()).isZero();

        assertThatThrownBy(() -> TransformingToolCallback.wrap(null, s -> s))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
