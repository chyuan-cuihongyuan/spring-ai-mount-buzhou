package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1413 / T2128：结构化输出 REASK 读数——漏斗双守恒（attempts=首过+再问解析+
 * 失败；reasks=再问解析+失败）、首过率派生、reset 归零；静态面测试前后归零防串扰。
 */
class StructuredOutputStatsTest {

    record Verdict(String summary, boolean pass) {
    }

    private AgentSession newSession(ScriptedChatModel model) {
        return Buzhou.runtime(model, Buzhou.inMemoryStores(),
                new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                        null, List.of()))
                .spawn("app", "agent", "s-sostats");
    }

    @BeforeEach
    void reset() {
        StructuredOutputStats.resetForTest();
    }

    @AfterEach
    void resetAfter() {
        StructuredOutputStats.resetForTest();
    }

    @Test
    void firstPassFlowCountsOnce() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("{\"summary\":\"ok\",\"pass\":true}"));
        AgentSession session = newSession(model);
        Verdict v = session.chatForEntity("判定", Verdict.class);
        session.close();

        assertThat(v.summary()).isEqualTo("ok");
        StructuredOutputStats.Snapshot s = StructuredOutputStats.stats();
        assertThat(s.attempts()).isEqualTo(1);
        assertThat(s.firstPassParsed()).isEqualTo(1);
        assertThat(s.reasks()).isZero();
        assertThat(s.attempts()).isEqualTo(
                s.firstPassParsed() + s.reaskParsed() + s.failures());
        assertThat(s.firstPassRate()).isEqualTo(1.0d);
    }

    @Test
    void reaskRecoveryFlowCountsFunnel() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("废话，不是 JSON。"));
        model.enqueue(new AssistantMessage("{\"summary\":\"fixed\",\"pass\":false}"));
        AgentSession session = newSession(model);
        Verdict v = session.chatForEntity("判定", Verdict.class);
        session.close();

        assertThat(v.summary()).isEqualTo("fixed");
        StructuredOutputStats.Snapshot s = StructuredOutputStats.stats();
        assertThat(s.attempts()).isEqualTo(1);
        assertThat(s.firstPassParsed()).isZero();
        assertThat(s.reasks()).isEqualTo(1);
        assertThat(s.reaskParsed()).isEqualTo(1);
        assertThat(s.failures()).isZero();
        assertThat(s.reasks()).isEqualTo(s.reaskParsed() + s.failures());
        assertThat(s.firstPassRate()).isEqualTo(0.0d);
    }

    @Test
    void doubleFailureCountsAsFailure() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("第一轮废话。"));
        model.enqueue(new AssistantMessage("第二轮还是废话。"));
        AgentSession session = newSession(model);
        assertThatThrownBy(() -> session.chatForEntity("判定", Verdict.class))
                .isInstanceOf(StructuredOutputException.class);
        session.close();

        StructuredOutputStats.Snapshot s = StructuredOutputStats.stats();
        assertThat(s.attempts()).isEqualTo(1);
        assertThat(s.reasks()).isEqualTo(1);
        assertThat(s.failures()).isEqualTo(1);
        // 双守恒式闭合
        assertThat(s.attempts()).isEqualTo(
                s.firstPassParsed() + s.reaskParsed() + s.failures());
        assertThat(s.reasks()).isEqualTo(s.reaskParsed() + s.failures());
    }

    @Test
    void emptyStatsAndReset() {
        StructuredOutputStats.Snapshot empty = StructuredOutputStats.stats();
        assertThat(empty.attempts()).isZero();
        assertThat(empty.firstPassRate()).isEqualTo(-1.0d); // 全零哨兵
        StructuredOutputStats.resetForTest();
        assertThat(StructuredOutputStats.stats().attempts()).isZero();
    }
}
