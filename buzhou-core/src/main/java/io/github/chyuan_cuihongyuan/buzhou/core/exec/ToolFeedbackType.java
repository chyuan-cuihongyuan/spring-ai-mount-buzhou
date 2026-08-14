package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.Optional;

/**
 * 工具错误反馈的结构化类型标记（spec 13 §cross-11 / ticket 29）：反馈文案机器可读分类的
 * <b>单一事实源</b>。此前识别靠散落的中文字符串前缀判断（字符串前缀当协议，反模式），
 * 现收敛为本枚举——每档反馈类型声明自己的标记前缀，识别逻辑遍历枚举走标记，
 * 新增反馈档位自动纳入识别；<b>对外消息词汇不变</b>（标记即既有文案前缀，兼容既有测试断言）。
 *
 * <p>两档语义（与既有体系一致，可分别对待）：
 * <ul>
 *   <li>{@link #EXECUTION_FAILURE}：调用已发生但出错（{@link ToolErrorFeedback} 通道）；</li>
 *   <li>{@link #VALIDATION_FAILURE}：调用未发生——参数未过 schema（{@link ToolValidationFeedback} 通道）。</li>
 * </ul>
 */
public enum ToolFeedbackType {

    /** 执行期失败：调用已发生但出错（错误即反馈通道）。 */
    EXECUTION_FAILURE("[工具执行失败]"),

    /** 参数校验失败：调用未发生，工具零误执行（REASK 通道）。 */
    VALIDATION_FAILURE("[工具参数校验失败]");

    private final String marker;

    ToolFeedbackType(String marker) {
        this.marker = marker;
    }

    /** 对外反馈文案的标记前缀（词汇不变；格式化与识别共用，保证两端一致）。 */
    public String marker() {
        return marker;
    }

    /** 判断文案是否以本类型标记开头（null 安全）。 */
    public boolean matches(String content) {
        return content != null && content.startsWith(marker);
    }

    /**
     * 识别一段工具响应文案的结构化类型。
     *
     * @param content 工具响应文案（可能来自历史消息回放，需兼容旧文案）
     * @return 命中的反馈类型；null / 无标记 / 普通结果返回 {@link Optional#empty()}
     */
    public static Optional<ToolFeedbackType> of(String content) {
        if (content == null || content.isEmpty()) {
            return Optional.empty();
        }
        for (ToolFeedbackType type : values()) {
            if (type.matches(content)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** 任一反馈档位标记命中即视为错误反馈（替代散落的字符串前缀判断；兼容既有文案）。 */
    public static boolean isErrorFeedback(String content) {
        return of(content).isPresent();
    }
}
