package io.github.chyuan_cuihongyuan.buzhou.core.exec;

/**
 * 「参数校验即反馈」通道（wayfinder2 impl-04 / T30 / docs/spec/12）：工具入参未过
 * JSON Schema 时<b>不执行工具</b>，合成结构化校验反馈回喂模型（REASK），模型修正后重试；
 * 重试受每 Turn 独立预算约束（{@code TurnLoopPolicy.retryBudget}），耗尽由
 * {@code BoundedToolCallingAdvisor} 优雅收尾。
 *
 * <p>与 {@link ToolErrorFeedback}（执行期失败）是<b>两档可区分词汇</b>：
 * 校验失败=调用未发生；执行失败=调用已发生但出错。观测与策略可分别对待。
 *
 * <p>来源：Pydantic AI（ValidationError 自动转 retry 消息）+ Instructor（REASK）。
 */
public final class ToolValidationFeedback {

    /** 反馈文案标记前缀（供停止条件统计校验失败次数）。 */
    public static final String MARKER = "[工具参数校验失败]";

    private ToolValidationFeedback() {
    }

    /**
     * 合成结构化校验反馈文案。
     *
     * @param toolName     被调用的工具名
     * @param arguments    原始工具入参（回显给模型以便定位修正）
     * @param reason       校验失败描述（来自 {@link ToolArgsValidator}）
     */
    public static String format(String toolName, String arguments, String reason) {
        String args = arguments == null || arguments.isBlank() ? "{}" : arguments;
        return MARKER + "（参数未过 schema，工具未执行）\n"
                + "工具：" + toolName + "\n"
                + "入参：" + args + "\n"
                + "原因：" + reason + "\n"
                + "建议：请按工具 schema 修正入参后重试；请勿不做任何修改地原样重试。";
    }

    /** 判断一段工具响应文案是否为校验反馈（供每 Turn 重试预算计数）。 */
    public static boolean isValidationFeedback(String content) {
        return content != null && content.startsWith(MARKER);
    }
}
