package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 结构化输出 e2e（spec 19 / T87 / impl-62）：合法直接返回、REASK 一次恢复、两败抛
 * StructuredOutputException、REASK 计入预算（runaway 步数闸）。
 */
class StructuredOutputEndToEndTest {

    record Verdict(String summary, boolean pass) {
    }

    /** 首轮即合法 JSON：直接解析返回，模型单次调用、无 reask 事件。 */
    @Test
    void validJsonParsedDirectly() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("{\"summary\":\"ok\",\"pass\":true}"));
        AgentSession session = newSession(model);
        List<SessionEvent> events = listen(session);

        Verdict v = session.chatForEntity("判定一下", Verdict.class);

        assertThat(v.summary()).isEqualTo("ok");
        assertThat(v.pass()).isTrue();
        assertThat(model.seenPrompts).hasSize(1);
        assertThat(events).noneMatch(e -> "structured.reask".equals(e.type()));
        session.close();
    }

    /** 首轮废话、次轮合规：REASK 事件 + 模型恰好两次调用。 */
    @Test
    void reaskRecoversOnSecondAttempt() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("我觉得一切正常，不用 JSON。"));
        model.enqueue(new AssistantMessage("{\"summary\":\"fixed\",\"pass\":false}"));
        AgentSession session = newSession(model);
        List<SessionEvent> events = listen(session);

        Verdict v = session.chatForEntity("判定一下", Verdict.class);

        assertThat(v.summary()).isEqualTo("fixed");
        assertThat(v.pass()).isFalse();
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> "structured.reask".equals(e.type()));
        session.close();
    }

    /** 两轮均不合规：抛 StructuredOutputException（含两轮输出摘要与解析错误）。 */
    @Test
    void bothAttemptsFailThrowsStructuredException() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("not json at all"));
        model.enqueue(new AssistantMessage("still not json"));
        AgentSession session = newSession(model);

        assertThatThrownBy(() -> session.chatForEntity("判定一下", Verdict.class))
                .isInstanceOf(StructuredOutputException.class)
                .hasMessageContaining("not json at all")
                .hasMessageContaining("still not json");
        assertThat(model.seenPrompts).hasSize(2);
        session.close();
    }

    /** REASK 诚实计入预算：runaway 会话累计 max-steps=1 时，REASK 的第二次模型调用被闸拦截。 */
    @Test
    void reaskHonorsRunawayStepBudget() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("garbage"));
        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties runaway =
                new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties(
                        null, null,
                        new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties.PerSession(
                                1, null),
                        null, null, null, null);
        var hook = new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook(
                runaway, new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters(), null);
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                new RuntimeConfig(List.of(hook), java.util.Set.of(), java.util.Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "agent", "sess");

        // 会话步数预算 1：首轮用掉，REASK 的第二次模型调用被 beforeModel 拦截（block 文本非 JSON → 解析失败路径）
        assertThatThrownBy(() -> session.chatForEntity("判定", Verdict.class))
                .isInstanceOf(StructuredOutputException.class);
        assertThat(model.seenPrompts).hasSize(1); // 第二次调用被预算闸拦下
        session.close();
    }

    // ---- helpers ----

    private static AgentSession newSession(ScriptedChatModel model) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        return runtime.spawn("app", "agent", "sess-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }
}
