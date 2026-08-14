package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ticket 29：错误反馈结构化标记测试——识别走 {@link ToolFeedbackType}（不再散落字符串前缀判断），
 * 且对既有（旧）文案词汇完全兼容：对外消息文本不变，旧前缀文本仍被正确识别。
 */
class ToolFeedbackTypeTest {

    @Test
    void shouldRecognizeExecutionFailure_whenLegacyFeedbackTextProvided() {
        String legacy = ToolErrorFeedback.format("flaky", "{\"orderId\":\"ORD-7\"}", "执行失败：disk-full");
        assertThat(legacy).startsWith("[工具执行失败]");
        assertThat(ToolFeedbackType.of(legacy)).contains(ToolFeedbackType.EXECUTION_FAILURE);
        assertThat(ToolFeedbackType.isErrorFeedback(legacy)).isTrue();
        assertThat(ToolErrorFeedback.isErrorFeedback(legacy)).isTrue();
    }

    @Test
    void shouldRecognizeValidationFailure_whenLegacyFeedbackTextProvided() {
        String legacy = ToolValidationFeedback.format("t", "{}", "缺少必填字段 orderId");
        assertThat(legacy).startsWith("[工具参数校验失败]");
        assertThat(ToolFeedbackType.of(legacy)).contains(ToolFeedbackType.VALIDATION_FAILURE);
        assertThat(ToolFeedbackType.isErrorFeedback(legacy)).isTrue();
        assertThat(ToolValidationFeedback.isValidationFeedback(legacy)).isTrue();
    }

    @Test
    void shouldReturnEmpty_whenPlainToolResultProvided() {
        assertThat(ToolFeedbackType.of("shipped")).isEmpty();
        assertThat(ToolFeedbackType.of(null)).isEmpty();
        assertThat(ToolFeedbackType.of("")).isEmpty();
        assertThat(ToolFeedbackType.isErrorFeedback("成功结果原文")).isFalse();
        assertThat(ToolFeedbackType.isErrorFeedback(null)).isFalse();
    }

    @Test
    void shouldNotCrossMatch_whenOtherChannelMarkerProvided() {
        String execution = ToolErrorFeedback.format("t", "{}", "执行失败：x");
        String validation = ToolValidationFeedback.format("t", "{}", "缺少字段");
        // 各通道自识别精确到本档（两档词汇分明，观测与策略可分别对待）
        assertThat(ToolErrorFeedback.isErrorFeedback(validation)).isFalse();
        assertThat(ToolValidationFeedback.isValidationFeedback(execution)).isFalse();
        // 汇总识别（任一档命中）兼容旧行为：两种前缀都算错误反馈
        assertThat(ToolFeedbackType.isErrorFeedback(execution)).isTrue();
        assertThat(ToolFeedbackType.isErrorFeedback(validation)).isTrue();
    }

    @Test
    void shouldKeepExternalVocabularyUnchanged_whenFormattingFeedback() {
        // 对外消息文本不变（兼容既有 examples 断言与模型侧提示词习惯）
        assertThat(ToolErrorFeedback.format("t", "{}", "执行超时（60s）"))
                .startsWith("[工具执行失败]（错误即反馈，本轮不中断）")
                .contains("工具：t").contains("入参：{}").contains("原因：执行超时（60s）");
        assertThat(ToolValidationFeedback.format("t", "{}", "缺少字段"))
                .startsWith("[工具参数校验失败]（参数未过 schema，工具未执行）")
                .contains("工具：t").contains("入参：{}").contains("原因：缺少字段");
    }

    @Test
    void shouldResolveMarkerFromEnum_whenMarkerConstantsReferenced() {
        // 标记单一事实源在枚举；格式化（写端）与识别（读端）共用同一常量，杜绝两端漂移
        assertThat(ToolErrorFeedback.MARKER).isEqualTo(ToolFeedbackType.EXECUTION_FAILURE.marker());
        assertThat(ToolValidationFeedback.MARKER).isEqualTo(ToolFeedbackType.VALIDATION_FAILURE.marker());
        assertThat(ToolFeedbackType.of("[工具执行失败]（任意后续内容）"))
                .isEqualTo(Optional.of(ToolFeedbackType.EXECUTION_FAILURE));
    }
}
