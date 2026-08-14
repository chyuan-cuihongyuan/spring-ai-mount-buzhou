package io.github.chyuan_cuihongyuan.buzhou.core.config;

/**
 * 装配期配置错误（impl-42 / spec 13 §T68）：拼错/越界的配置在<b>启动即失败</b>（fail-fast），
 * 消息自带人类可读「description + action」——经 {@link BuzhouStoreFailureAnalyzer} 翻译到
 * 启动诊断输出（FAILURE ANALYSIS 面板）。
 */
public class BuzhouConfigurationException extends IllegalStateException {

    /** 建议动作（FailureAnalyzer 的 action 行）。 */
    private final String action;

    public BuzhouConfigurationException(String description, String action) {
        super(composeMessage(description, action));
        this.action = action;
    }

    public BuzhouConfigurationException(String description, String action, Throwable cause) {
        super(composeMessage(description, action), cause);
        this.action = action;
    }

    /** message 携带建议（裸栈也可见指引，不只依赖 FailureAnalyzer 面板）。 */
    private static String composeMessage(String description, String action) {
        return action == null || action.isBlank() ? description
                : description + "。建议：" + action;
    }

    public String action() {
        return action;
    }
}
