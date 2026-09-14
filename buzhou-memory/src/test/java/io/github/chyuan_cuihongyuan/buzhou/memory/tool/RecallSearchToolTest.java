package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecallSearchTool 直测（K 会话 R2 / spec 1201 / T1806）：recall_search 的用户可见
 * 输出合同——四模命中行格式、摘要空白归一与 160 截断、无命中/缺上下文/非法 mode
 * 三类文案分支、EMBEDDING/HYBRID 未注入 provider 显式降级（Elasticsearch
 * partial-results「降级要显式提示」思想）、limit 与轮次窗。
 * 先例：EvidenceLookupToolTest（store + BuzhouMessage 构造）、EmbeddingProviderTest（词包 fake）。
 */
class RecallSearchToolTest {

    private static final String SESSION = "s1";

    private final InMemoryMessageStore store = new InMemoryMessageStore();

    private BuzhouMessage msg(String id, int turnSeq, Role role, String content) {
        return new BuzhouMessage(id, SESSION, turnSeq, 1, role, content,
                List.of(), null, null, null, Map.of(), Instant.now());
    }

    private RecallSearchTool tool() {
        return new RecallSearchTool(store, null);
    }

    private ToolContext sessionCtx() {
        return new ToolContext(Map.of(HarnessToolCallingManager.SESSION_ID_KEY, SESSION));
    }

    /** 确定性 char-bucket 词包向量（EmbeddingProviderTest R1 先例）：重叠文本余弦高分。 */
    private static float[] bag(String text) {
        float[] vector = new float[8];
        for (char c : text.toLowerCase().toCharArray()) {
            vector[c % 8] += 1;
        }
        return vector;
    }

    @Test
    void textModeHitLineCarriesAllContractFields() {
        store.append(SESSION, List.of(msg("m1", 1, Role.USER, "kafka offset 重置讨论")));

        String result = tool().call("{\"mode\":\"text\",\"query\":\"kafka\"}", sessionCtx());

        assertThat(result).startsWith("命中 1 条（mode=text）：");
        assertThat(result).contains("turn=1").contains("role=USER").contains("id=m1")
                .contains("score=1.00").contains("kafka offset 重置讨论");
    }

    @Test
    void snippetCollapsesWhitespaceAndTruncatesAt160() {
        store.append(SESSION, List.of(
                msg("m2", 1, Role.ASSISTANT, "alpha  beta\n\tgamma " + "x".repeat(200))));

        String result = tool().call("{\"mode\":\"text\",\"query\":\"alpha beta\"}", sessionCtx());

        assertThat(result).contains("alpha beta gamma");
        assertThat(result).contains("…");
        assertThat(result).doesNotContain("x".repeat(200));
    }

    @Test
    void noHitReturnsExplicitNotice() {
        store.append(SESSION, List.of(msg("m3", 1, Role.USER, "hello world")));

        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"zzzz\"}", sessionCtx()))
                .isEqualTo("[recall_search 无命中] mode=text");
    }

    @Test
    void missingSessionContextFailsAtThreeEntries() {
        // ① 单参 call → toolContext 为 null
        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"kw\"}"))
                .contains("缺少会话上下文（sessionId）");
        // ② context 空 Map → sessionId 提取为 null
        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"kw\"}", new ToolContext(Map.of())))
                .contains("缺少会话上下文（sessionId）");
        // ③ 字面 "null" 字符串（String.valueOf 包装产物）同文案
        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"kw\"}",
                new ToolContext(Map.of(HarnessToolCallingManager.SESSION_ID_KEY, "null"))))
                .contains("缺少会话上下文（sessionId）");
    }

    @Test
    void vectorModesDegradeExplicitlyWithoutProvider() {
        assertThat(tool().call("{\"mode\":\"embedding\",\"query\":\"kw\"}", sessionCtx()))
                .isEqualTo("[recall_search 降级] EMBEDDING 模式需 EmbeddingProvider（未注入）；"
                        + "请改用 text 或 time 模式。");
        assertThat(tool().call("{\"mode\":\"hybrid\",\"query\":\"kw\"}", sessionCtx()))
                .isEqualTo("[recall_search 降级] HYBRID 模式需 EmbeddingProvider（未注入）；"
                        + "请改用 text 或 time 模式。");
    }

    @Test
    void embeddingModeWorksWithProvider() {
        store.append(SESSION, List.of(msg("m4", 1, Role.USER, "kafka offset reset policy")));
        RecallSearchTool withVector = new RecallSearchTool(store, RecallSearchToolTest::bag);

        String result = withVector.call("{\"mode\":\"embedding\",\"query\":\"kafka offset\"}", sessionCtx());

        assertThat(result).startsWith("命中 1 条（mode=embedding）：").contains("id=m4");
    }

    @Test
    void hybridModeWorksWithProvider() {
        store.append(SESSION, List.of(msg("m5", 1, Role.USER, "kafka offset reset policy")));
        RecallSearchTool withVector = new RecallSearchTool(store, RecallSearchToolTest::bag);

        String result = withVector.call("{\"mode\":\"hybrid\",\"query\":\"kafka offset\"}", sessionCtx());

        assertThat(result).startsWith("命中 1 条（mode=hybrid）：").contains("id=m5");
    }

    @Test
    void timeModeOrdersByTurnDescendingWithUnitScore() {
        store.append(SESSION, List.of(
                msg("early", 1, Role.USER, "kw"),
                msg("mid", 2, Role.USER, "kw"),
                msg("late", 3, Role.USER, "kw")));

        String result = tool().call("{\"mode\":\"time\"}", sessionCtx());

        assertThat(result).startsWith("命中 3 条（mode=time）：");
        assertThat(result.indexOf("id=late")).isLessThan(result.indexOf("id=mid"));
        assertThat(result.indexOf("id=mid")).isLessThan(result.indexOf("id=early"));
        assertThat(result).contains("score=1.00");
    }

    @Test
    void turnWindowFiltersByFromTo() {
        store.append(SESSION, List.of(
                msg("t1", 1, Role.USER, "kw"),
                msg("t3", 3, Role.USER, "kw"),
                msg("t4", 4, Role.USER, "kw"),
                msg("t5", 5, Role.USER, "kw")));

        String result = tool().call(
                "{\"mode\":\"time\",\"fromTurn\":3,\"toTurn\":4}", sessionCtx());

        assertThat(result).contains("命中 2 条").contains("id=t3").contains("id=t4")
                .doesNotContain("id=t1").doesNotContain("id=t5");
    }

    @Test
    void defaultLimitIsTenAndOverrideHonored() {
        List<BuzhouMessage> batch = new java.util.ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            batch.add(msg("bulk-" + i, i, Role.USER, "kw" + i));
        }
        store.append(SESSION, batch);

        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"kw\"}", sessionCtx()))
                .startsWith("命中 10 条");
        assertThat(tool().call("{\"mode\":\"text\",\"query\":\"kw\",\"limit\":1}", sessionCtx()))
                .startsWith("命中 1 条");
    }

    @Test
    void invalidModeReturnsFailureNoticeInsteadOfThrowing() {
        assertThat(tool().call("{\"mode\":\"bogus\"}", sessionCtx()))
                .startsWith("[recall_search 失败] ");
    }

    @Test
    void omittedModeDefaultsToText() {
        store.append(SESSION, List.of(msg("m6", 1, Role.USER, "needle in stack")));

        assertThat(tool().call("{\"query\":\"needle\"}", sessionCtx()))
                .startsWith("命中 1 条（mode=text）：");
    }
}
