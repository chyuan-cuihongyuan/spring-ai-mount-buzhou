package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ragas 系数值型评估器（spec 87 §A / T333，Ragas faithfulness / answer-relevancy
 * 借鉴）：与 {@link LlmJudgeEvaluator}（二值 PASS/FAIL）互补的<b>连续分</b>面——
 * judge 输出 {@code S <num>/<den>} 协议（S 前缀容差），score = num/den，过阈值判
 * pass。协议失败抛 {@link LlmJudgeEvaluator.JudgeProtocolException}（runner 收敛
 * 该条 error——不猜，与 spec 61 同口径）。
 *
 * <p><b>两指标</b>：
 * <ul>
 *   <li><b>faithfulness</b>——实际输出的断言被期望输出（黄金答案）支持的比例
 *       （幻觉面：编造断言拉低分）；</li>
 *   <li><b>answerRelevancy</b>——实际输出对输入（用户问题）的针对性与覆盖
 *       （跑题面：答非所问拉低分）。</li>
 * </ul>
 * 分母 0（无断言可评）按 0.0 从严（空输出不给满分——spec 80 同口径）。
 *
 * @since 1.0.0
 */
public final class RagasEvaluators {

    private static final Pattern SCORE_PROTOCOL =
            Pattern.compile("^\\s*S\\s+(\\d+)\\s*/\\s*(\\d+)");

    private RagasEvaluators() {
    }

    /** faithfulness（断言支持率）：threshold 默认 0.8。 */
    public static Evaluator faithfulness(ChatModel judge) {
        return faithfulness(judge, 0.8);
    }

    /** faithfulness（断言支持率，pass = score ≥ threshold）。 */
    public static Evaluator faithfulness(ChatModel judge, double threshold) {
        return new ScoredJudge(judge, """
                你的任务：评估「实际输出」的事实忠实度（faithfulness）。
                步骤：1) 把实际输出分解为全部事实性断言；2) 判断每条断言是否被
                「期望输出」（黄金答案）支持。编造/无依据的断言计为不支持；措辞
                差异但事实一致计为支持。""",
                threshold, Reference.EXPECTED, "faithfulness");
    }

    /** answerRelevancy（回答针对性）：threshold 默认 0.8。 */
    public static Evaluator answerRelevancy(ChatModel judge) {
        return answerRelevancy(judge, 0.8);
    }

    /** answerRelevancy（回答针对性，pass = score ≥ threshold）。 */
    public static Evaluator answerRelevancy(ChatModel judge, double threshold) {
        return new ScoredJudge(judge, """
                你的任务：评估「实际输出」对「用户输入」的回答针对性
                （answer relevancy，0-10 整数）：输出是否直接回应输入的诉求、
                覆盖其核心要点；冗余/跑题/答非所问扣分。""",
                threshold, Reference.INPUT, "answerRelevancy");
    }

    /**
     * G-Eval 自定义维度打分（spec 89 §A / T341，DeepEval G-Eval 借鉴）：宿主给维度名
     * 与评分标准（rubric），judge 按标准打 0-10 分——「合规性/语气/精度/简洁性」等
     * 领域口径无需新类。参照系 BOTH：输入与黄金答案都进 prompt（自定义维度各取所需）。
     */
    public static Evaluator gEval(ChatModel judge, String dimension, String rubric) {
        return gEval(judge, dimension, rubric, 0.8);
    }

    /** G-Eval 带阈值（pass = score ≥ threshold；detail 前缀维度名）。 */
    public static Evaluator gEval(ChatModel judge, String dimension, String rubric,
            double threshold) {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("gEval 维度名不能为空（用于 detail 溯源与看板分组）");
        }
        return new ScoredJudge(judge, """
                你的任务：按以下评分标准评估「实际输出」，维度：""" + dimension.strip() + """
                。标准描述：
                """ + (rubric == null || rubric.isBlank() ? "输出在该维度上的质量（0-10 整数）" : rubric.strip()),
                threshold, Reference.BOTH, dimension.strip());
    }

    /** 评分参照系（prompt 组装面——测试钉住不漂移）。 */
    enum Reference { EXPECTED, INPUT, BOTH }

    /** 共用数值协议实现（S num/den → score；clamp 0..1）。 */
    private static final class ScoredJudge implements Evaluator {

        private final ChatModel judge;
        private final String rubric;
        private final double threshold;
        private final Reference reference;
        private final String dimension;

        ScoredJudge(ChatModel judge, String rubric, double threshold, Reference reference,
                String dimension) {
            this.judge = judge;
            this.rubric = rubric;
            this.threshold = Math.max(0.0, Math.min(1.0, threshold));
            this.reference = reference;
            this.dimension = dimension;
        }

        @Override
        public EvalScore evaluate(String actual, String expected, EvalItem item) {
            String system = """
                    你是评估打分器。严格按任务说明评分，回复必须形如 S x/y\
                    （S 大写，x/y 为整数，x ≤ y），随后用一至两句中文说明理由。\
                    不要输出其他前缀、标记或寒暄。""";
            StringBuilder user = new StringBuilder("任务说明：").append(rubric);
            if (reference == Reference.EXPECTED || reference == Reference.BOTH) {
                user.append("\n\n【期望输出（黄金答案）】\n").append(expected);
            }
            if (reference == Reference.INPUT || reference == Reference.BOTH) {
                user.append("\n\n【用户输入】\n").append(item.input());
            }
            user.append("\n\n【实际输出】\n").append(actual)
                    .append("\n\n请评分（S x/y；分母按任务说明给定的量纲）。");
            ChatResponse response = judge.call(new Prompt(List.of(
                    new org.springframework.ai.chat.messages.SystemMessage(system),
                    new org.springframework.ai.chat.messages.UserMessage(user.toString()))));
            String text = response.getResult().getOutput().getText();
            Matcher m = SCORE_PROTOCOL.matcher(String.valueOf(text));
            if (!m.find()) {
                throw new LlmJudgeEvaluator.JudgeProtocolException(
                        "数值评分协议失败（期望 S x/y，收到：" + preview(text) + "）");
            }
            int num = Integer.parseInt(m.group(1));
            int den = Integer.parseInt(m.group(2));
            if (num > den || den < 0) {
                throw new LlmJudgeEvaluator.JudgeProtocolException(
                        "数值评分越界（num=" + num + " den=" + den + "）");
            }
            double score = den == 0 ? 0.0 : Math.min(1.0, (double) num / den);
            boolean pass = score >= threshold;
            String detail = "[" + dimension + "] score="
                    + String.format(Locale.ROOT, "%.3f", score)
                    + "（" + num + "/" + den + "）threshold=" + threshold
                    + (den == 0 ? "；分母 0 按从严 0.0" : "");
            return pass ? EvalScore.pass(detail) : EvalScore.fail(detail);
        }

        private static String preview(String text) {
            if (text == null) {
                return "<null>";
            }
            return text.lines().findFirst().map(s -> s.length() > 60 ? s.substring(0, 60) : s)
                    .orElse("<blank>");
        }
    }
}
