package io.github.chyuan_cuihongyuan.buzhou.core.exec;

/**
 * 「错误即反馈」统一通道：工具侧失败（执行异常 / 超时 / 取消 / 中断 / 工具缺失）一律合成为
 * 结构化错误文案回喂模型，按 tool_call 原序注入 ToolResponseMessage，Turn 不因单工具异常终结，
 * 模型可依据错误与原入参自我纠错继续完成任务。
 *
 * <p>与模型侧异常处理（buzhou-resilience 的 onModelError，重试/统一超时/兜底）正交：
 * 本通道只作用于<b>工具侧</b>异常；模型侧异常仍由模型韧性层语义处理，互不吞没。
 *
 * <p>来源：OpenAI Agents SDK {@code tool_not_found_behavior='return_error_to_model'} +
 * {@code tool_error_formatter} 的 best-of-breed 思想（wayfinder T16 / docs/spec/11）。
 */
public final class ToolErrorFeedback {

    private ToolErrorFeedback() {
    }

    /**
     * 合成结构化错误反馈文案。
     *
     * @param toolName  被调用的工具名（缺失场景即模型误写的名字）
     * @param arguments 原始工具入参（回显给模型以便定位纠错；空白时显示 {@code {}}）
     * @param reason    失败原因（沿用既有原因文案，如「执行失败：…」「执行超时（60s）」「未知工具：…」）
     */
    public static String format(String toolName, String arguments, String reason) {
        String args = arguments == null || arguments.isBlank() ? "{}" : arguments;
        return "[工具执行失败]（错误即反馈，本轮不中断）\n"
                + "工具：" + toolName + "\n"
                + "入参：" + args + "\n"
                + "原因：" + reason + "\n"
                + "建议：请依据「原因」修正入参或改用其他途径继续完成任务；请勿不做任何修改地原样重试。";
    }

    /** 工具缺失的失败原因文案（保留既有「未知工具：」前缀语义）。 */
    public static String missingToolReason(String toolName) {
        return "未知工具：" + toolName + "（可用工具列表中不存在此名称，请核对工具名拼写后重试）";
    }
}
