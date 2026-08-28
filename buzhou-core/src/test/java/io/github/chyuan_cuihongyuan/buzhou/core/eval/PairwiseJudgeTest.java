package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 成对对比评估红队（spec 63 §B / T278）：内容裁决（与位置无关）→ 一致赢家；位置偏好
 * （恒选首位）→ position-bias TIE；双向平局 → both-tie；协议失败 → protocol TIE；
 * 双向调用顺序相反（prompt 断言）；rubric 注入。
 */
class PairwiseJudgeTest {

    /** 按调用序回放固定响应的 stub（捕获 prompts）。 */
    static final class ScriptedJudge implements ChatModel {
        final List<String> responses;
        final List<Prompt> prompts = new ArrayList<>();
        private int cursor;

        ScriptedJudge(String... responses) {
            this.responses = List.of(responses);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            String response = responses.get(Math.min(cursor++, responses.size() - 1));
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }
    }

    /** 内容裁决 stub：无论位置，含「gold」标记的输出赢（第二个参数=首轮展示位）。 */
    static final class ContentBasedJudge implements ChatModel {
        final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            String user = prompt.getInstructions().get(1).getText();
            String slotA = between(user, "【输出A】", "【输出B】");
            String winner = slotA.contains("gold") ? "WINNER_A" : "WINNER_B";
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    winner + " 该输出完整直接"))));
        }

        private static String between(String text, String from, String to) {
            int a = text.indexOf(from) + from.length();
            int b = text.indexOf(to, a);
            return text.substring(a, b);
        }
    }

    @Test
    void contentConsistentWinnerSurvivesBidirectional() {
        ContentBasedJudge judge = new ContentBasedJudge();
        PairwiseJudge Pairwise = new PairwiseJudge(judge);

        PairwiseJudge.PairwiseVerdict verdict = Pairwise.compare(
                "解释重试语义", "gold 完整解释", "简略回答");

        assertThat(verdict.winner()).isEqualTo(PairwiseJudge.Winner.WINNER_A);
        assertThat(verdict.reason()).startsWith("consistent:");
        // 双向调用：第二轮 A 在第二展示位（prompt 内顺序相反）
        assertThat(judge.prompts).hasSize(2);
        String secondUser = judge.prompts.get(1).getInstructions().get(1).getText();
        assertThat(secondUser.indexOf("gold")).isGreaterThan(secondUser.indexOf("简略回答"));
    }

    @Test
    void positionBiasYieldsTie() {
        // 恒选首位展示：forward=WINNER_A，backward（A 在第二位）仍 WINNER_A → 翻转 → TIE
        ScriptedJudge judge = new ScriptedJudge("WINNER_A 首个更好", "WINNER_A 首个更好");
        PairwiseJudge.PairwiseVerdict verdict = new PairwiseJudge(judge).compare(
                "q", "内容甲", "内容乙");
        assertThat(verdict.winner()).isEqualTo(PairwiseJudge.Winner.TIE);
        assertThat(verdict.reason()).startsWith("position-bias:");
    }

    @Test
    void bothDirectionsTieYieldsTie() {
        ScriptedJudge judge = new ScriptedJudge("TIE 相当", "TIE 相当");
        PairwiseJudge.PairwiseVerdict verdict = new PairwiseJudge(judge).compare("q", "a", "b");
        assertThat(verdict.winner()).isEqualTo(PairwiseJudge.Winner.TIE);
        assertThat(verdict.reason()).startsWith("both-tie:");
    }

    @Test
    void protocolFailureYieldsProtocolTie() {
        ScriptedJudge judge = new ScriptedJudge("我觉得都行", "WINNER_B 第二个更好");
        PairwiseJudge.PairwiseVerdict verdict = new PairwiseJudge(judge).compare("q", "a", "b");
        assertThat(verdict.winner()).isEqualTo(PairwiseJudge.Winner.TIE);
        assertThat(verdict.reason()).startsWith("protocol:");
    }

    @Test
    void rubricReachesBothDirections() {
        ScriptedJudge judge = new ScriptedJudge("TIE", "TIE");
        new PairwiseJudge(judge, "以合规口径比较").compare("q", "a", "b");
        for (Prompt p : judge.prompts) {
            assertThat(p.getInstructions().getFirst().getText()).contains("以合规口径比较");
            assertThat(p.getInstructions().getFirst().getText()).contains("WINNER_A");
        }
    }

    @Test
    void verdictParsingTolerantToCaseAndWhitespace() {
        ScriptedJudge judge = new ScriptedJudge("  winner_a 理由", "\nTIE 平\n");
        PairwiseJudge.PairwiseVerdict verdict = new PairwiseJudge(judge).compare("q", "a", "b");
        assertThat(verdict.winner()).isEqualTo(PairwiseJudge.Winner.TIE); // 方向翻转（A→TIE）非一致
        assertThat(verdict.reason()).startsWith("position-bias:");
    }
}
