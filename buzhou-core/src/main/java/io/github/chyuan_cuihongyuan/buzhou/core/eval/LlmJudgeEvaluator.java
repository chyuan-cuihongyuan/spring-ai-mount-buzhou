package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Locale;

/**
 * LLM-as-judge 评估器（spec 61 §A / T273 / effort#21，DeepEval/Ragas/G-Eval 借鉴）：
 * 注入 judge {@link ChatModel} 按输出协议（响应首词 PASS/FAIL，大小写/首尾空白容差）
 * 判定语义质量——弥补确定性三件（EXACT/CONTAINS/REGEX）无法表达的「回答是否解决问题」
 * 类断言。
 *
 * <p><b>输出协议</b>：系统段固定要求「回复必须以 PASS 或 FAIL 开头，随后一至两句理由」；
 * 解析取 trim 后首个空白分隔词（忽略大小写）；不可解析抛 {@link JudgeProtocolException}
 * （{@code EvalRunner} 收敛为该条 error——不猜成 FAIL，passRate 不被协议失败污染）。
 *
 * <p><b>诚实边界</b>：判别力与抗提示注入归 judge 模型（评估面信任边界内，rubric 不做
 * 消毒）；无温度控制（确定性由 ChatModel 宿主配置承担）；CI 不强制（runbook §9 的
 * LLM-judge + 人工抽检口径沿用）。
 *
 * @since 1.0.0
 */
public final class LlmJudgeEvaluator implements Evaluator {

    /** judge 响应不符合输出协议（runner 记该条 error）。 */
    public static final class JudgeProtocolException extends RuntimeException {
        public JudgeProtocolException(String message) {
            super(message);
        }
    }

    /** 默认 rubric：语义等价判定（可注入领域口径覆盖）。 */
    public static final String DEFAULT_RUBRIC = """
            判定「实际输出」是否在语义上满足「期望输出」表达的要求：\
            核心事实一致、用户诉求被回应即可；措辞与格式差异不计。\
            模棱两可时倾向 FAIL（评估口径从严）。""";

    private static final String PROTOCOL = """
            你是评估判定器。严格按以下评分规则判定，回复必须以 PASS 或 FAIL 开头，\
            随后用一至两句中文说明理由。不要输出其他前缀、标记或寒暄。""";

    private final ChatModel judge;
    private final String rubric;

    public LlmJudgeEvaluator(ChatModel judge) {
        this(judge, DEFAULT_RUBRIC);
    }

    /** 自定义 rubric（领域口径：合规/语气/精度等；null 回落默认语义等价）。 */
    public LlmJudgeEvaluator(ChatModel judge, String rubric) {
        this.judge = judge;
        this.rubric = rubric == null || rubric.isBlank() ? DEFAULT_RUBRIC : rubric.strip();
    }

    @Override
    public EvalScore evaluate(String actual, String expected, EvalItem item) {
        String system = PROTOCOL + "\n\n评分规则：" + rubric;
        String user = "【期望输出】\n" + expected + "\n\n【实际输出】\n" + actual
                + "\n\n请判定。";
        String response = judge.call(new Prompt(
                java.util.List.of(
                        new org.springframework.ai.chat.messages.SystemMessage(system),
                        new org.springframework.ai.chat.messages.UserMessage(user))))
                .getResult().getOutput().getText();
        String verdict = firstWord(response);
        if ("PASS".equalsIgnoreCase(verdict)) {
            return EvalScore.pass("judge: " + afterFirstWord(response));
        }
        if ("FAIL".equalsIgnoreCase(verdict)) {
            return EvalScore.fail("judge: " + afterFirstWord(response));
        }
        throw new JudgeProtocolException("judge 响应不符合 PASS/FAIL 首词协议："
                + (response == null ? "null" : response.lines().findFirst().orElse("")));
    }

    private static String firstWord(String response) {
        if (response == null) {
            return "";
        }
        String[] parts = response.strip().split("\\s+", 2);
        return parts.length == 0 ? "" : parts[0];
    }

    private static String afterFirstWord(String response) {
        if (response == null) {
            return "";
        }
        String[] parts = response.strip().split("\\s+", 2);
        String rest = parts.length > 1 ? parts[1].strip() : "";
        return rest.isEmpty() ? response.strip().toUpperCase(Locale.ROOT) : rest;
    }
}
