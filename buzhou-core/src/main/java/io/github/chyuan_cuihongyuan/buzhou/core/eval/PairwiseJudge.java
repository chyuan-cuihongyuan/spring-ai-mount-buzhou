package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Locale;

/**
 * 成对对比评估（spec 63 §A / T277 / effort#23，Ragas pairwise / Chatbot Arena 借鉴）：
 * 判定两个输出（A/B）哪个更好地回应输入——<b>双向评判消位置偏差</b>：LLM judge 系统性
 * 偏好首位展示（Arena 实证），故 (A,B) 与 (B,A) 各评一次，<b>两方向裁定同一赢家才判
 * WINNER_A/WINNER_B</b>；不一致判 TIE 并标注 position-bias（偏差显性化而非静默）。
 *
 * <p><b>输出协议</b>：judge 响应首词 WINNER_A / WINNER_B / TIE（大小写/空白容差），
 * 随后一至两句理由；协议失败按该方向 TIE 记（不猜）。最终 reason 标注裁决依据：
 * {@code consistent}（双向一致）/ {@code position-bias}（方向翻转）/ {@code protocol}
 * （协议失败）/ {@code both-tie}（双向皆平）。
 *
 * <p><b>诚实边界</b>（#21 同口径）：判别力与抗注入归 judge 模型；无温度控制；
 * 成本 = 每对 2 次 judge 调用。宿主自行驱动两次运行后调用本面（不做 A/B 编排）。
 *
 * @since 1.0.0
 */
public final class PairwiseJudge {

    /** 成对裁决结果。 */
    public enum Winner {
        WINNER_A, WINNER_B, TIE
    }

    /** 裁决（winner + 裁决依据标注 reason）。 */
    public record PairwiseVerdict(Winner winner, String reason) {
    }

    /** 默认 rubric（可注入领域口径覆盖）。 */
    public static final String DEFAULT_RUBRIC = """
            判定哪个输出更好地回应了输入：完整性、正确性、直接性；\
            两者相当（或各有明显硬伤）时判 TIE。措辞与格式差异不计。""";

    private static final String PROTOCOL = """
            你是成对评估判定器。严格按评分规则比较「输出A」与「输出B」，回复必须以 \
            WINNER_A、WINNER_B 或 TIE 之一开头，随后用一至两句中文说明理由。\
            不要输出其他前缀、标记或寒暄。""";

    private final ChatModel judge;
    private final String rubric;

    public PairwiseJudge(ChatModel judge) {
        this(judge, DEFAULT_RUBRIC);
    }

    /** 自定义 rubric（null/空回落默认）。 */
    public PairwiseJudge(ChatModel judge, String rubric) {
        this.judge = judge;
        this.rubric = rubric == null || rubric.isBlank() ? DEFAULT_RUBRIC : rubric.strip();
    }

    /** 双向评判并给出一致裁决。 */
    public PairwiseVerdict compare(String input, String outputA, String outputB) {
        String forward = ask(input, outputA, outputB);   // (A,B)：首词相对「输出A」
        String backward = ask(input, outputB, outputA);  // (B,A)：首词相对「输出B」（标签同位）
        boolean forwardProtocolOk = isProtocolOk(forward);
        boolean backwardProtocolOk = isProtocolOk(backward);
        if (!forwardProtocolOk || !backwardProtocolOk) {
            return new PairwiseVerdict(Winner.TIE, "protocol: judge 响应不符合首词协议（forward="
                    + firstWord(forward) + "，backward=" + firstWord(backward) + "）");
        }
        Winner wForward = parseVerdict(forward);
        Winner wBackward = parseVerdict(backward);
        if (wForward == Winner.TIE && wBackward == Winner.TIE) {
            return new PairwiseVerdict(Winner.TIE, "both-tie: 双向均判平局");
        }
        // 双向一致才裁：forward=WINNER_A 与 backward=WINNER_B 指向同一内容赢家（对称）
        if (wForward == Winner.WINNER_A && wBackward == Winner.WINNER_B) {
            return new PairwiseVerdict(Winner.WINNER_A, "consistent: " + afterFirstWord(forward));
        }
        if (wForward == Winner.WINNER_B && wBackward == Winner.WINNER_A) {
            return new PairwiseVerdict(Winner.WINNER_B, "consistent: " + afterFirstWord(backward));
        }
        return new PairwiseVerdict(Winner.TIE, "position-bias: 双向裁决翻转（forward="
                + wForward + "，backward=" + wBackward + "）——judge 存在首位展示偏好，不裁");
    }

    private String ask(String input, String first, String second) {
        String system = PROTOCOL + "\n\n评分规则：" + rubric;
        String user = "【输入】\n" + input + "\n\n【输出A】\n" + first
                + "\n\n【输出B】\n" + second + "\n\n请判定。";
        return judge.call(new Prompt(List.of(
                new org.springframework.ai.chat.messages.SystemMessage(system),
                new org.springframework.ai.chat.messages.UserMessage(user))))
                .getResult().getOutput().getText();
    }

    private static boolean isProtocolOk(String response) {
        String w = firstWord(response);
        return "WINNER_A".equalsIgnoreCase(w) || "WINNER_B".equalsIgnoreCase(w)
                || "TIE".equalsIgnoreCase(w);
    }

    private static Winner parseVerdict(String response) {
        return Winner.valueOf(firstWord(response).toUpperCase(Locale.ROOT));
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
        return parts.length > 1 ? parts[1].strip() : "";
    }
}
