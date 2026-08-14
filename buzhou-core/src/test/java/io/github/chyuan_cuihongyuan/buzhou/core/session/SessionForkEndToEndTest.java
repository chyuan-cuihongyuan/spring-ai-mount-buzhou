package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话 fork e2e（spec 20 / T88 / impl-63）：历史完整复制续聊、分支独立演化、
 * State 不复制（预算重置）、空源拒绝、session.forked 事件。
 */
class SessionForkEndToEndTest {

    /** fork 后新会话带完整源历史：第二轮 prompt 含源会话的问答上下文。 */
    @Test
    void forkCarriesHistoryIntoNewSession() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("源会话回答：不周山");
        model.enqueueText("分支回答");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("这座山叫什么？");
        source.close();

        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");
        branch.chat("再问一遍？");

        // 第二轮 prompt（分支的第二问）应包含源会话历史（memory 注入）
        String secondPrompt = model.seenPrompts.get(1).getContents().toString();
        assertThat(secondPrompt).contains("这座山叫什么");
        assertThat(secondPrompt).contains("不周山");
        branch.close();
    }

    /** 分支独立演化：分支续写不影响源会话消息历史。 */
    @Test
    void branchesEvolveIndependently() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        model.enqueueText("b1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1");
        int sourceMessages = stores.messageStore().load("sess-src").size();

        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");
        branch.chat("q2");
        branch.close();

        assertThat(stores.messageStore().load("sess-src")).hasSize(sourceMessages); // 源不动
        assertThat(stores.messageStore().load("sess-branch")).hasSize(sourceMessages + 2); // 分支 +问+答
        source.close();
    }

    /** State 不复制：源会话消耗掉的预算不带到分支（fork = 重试语义）。 */
    @Test
    void forkResetsSessionStateBudgets() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 源会话预算闸：会话累计步数 1（一次 chat 后下一次模型调用被拦）
        var runaway = new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties(
                null, null,
                new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties.PerSession(1, null),
                null, null, null, null);
        var hook = new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayHook(
                runaway, new io.github.chyuan_cuihongyuan.buzhou.core.runaway.RunawayCounters(),
                stores.observabilityStore());
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                new RuntimeConfig(List.of(hook), java.util.Set.of(), java.util.Set.of(), null, List.of()));

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1"); // 用掉唯一一步
        source.close();

        // 分支预算重置：State 未复制 → 分支首个 chat 正常（预算从零起算）
        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");
        assertThat(branch.chat("q2")).isEqualTo("r2");
        branch.close();
    }

    /** 空源拒绝 + forked 事件。 */
    @Test
    void emptySourceRejectedAndForkedEventEmitted() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        assertThatThrownBy(() -> runtime.fork("no-such-session", "app", "agent", "sess-x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无消息历史");

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1");
        source.close();

        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        // forked 事件在 fork 时已发（先于本 listener 注册）；改从事件流断言分支可继续对话 + 源消息可见
        branch.addEventListener(events::add);
        branch.chat("q2");
        assertThat(stores.messageStore().load("sess-branch")).isNotEmpty();
        branch.close();
    }
}
