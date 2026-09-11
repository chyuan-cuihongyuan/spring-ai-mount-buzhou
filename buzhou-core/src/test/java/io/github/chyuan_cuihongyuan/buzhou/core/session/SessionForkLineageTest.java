package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话 fork 谱系测试（spec 602 / T854–T855 / impl 455，OTel span-links 思想落为
 * buzhou 关联面）：谱系 state 写入（普通 fork 与时间旅行 fork）、事件携带 copy 计数、
 * 导出携带谱系条目。
 */
class SessionForkLineageTest {

    /** 捕获全部会话事件的 hook（事件先经 hook 链再达 listener——fork 时点即可捕获）。 */
    private static final class EventCaptureHook implements BuzhouHook {
        final List<SessionEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionEventContext ctx) {
            events.add(ctx.event());
        }
    }

    /** 普通 fork：子会话 state 带 buzhou.fork.source → 源会话 id；事件带 copy 计数。 */
    @Test
    void forkWritesLineageStateAndEventCounts() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EventCaptureHook capture = new EventCaptureHook();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(RuntimeConfig.defaults(),
                        RuntimeConfig.hooks(List.of(capture))));

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1");
        source.close();

        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");

        Map<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> state =
                stores.sessionStateStore().getAll("sess-branch");
        assertThat(state).containsKey("buzhou.fork.source");
        assertThat(state.get("buzhou.fork.source").value()).isEqualTo("sess-src");
        assertThat(state.get("buzhou.fork.source").producer()).isEqualTo("buzhou.core.fork");

        SessionEvent forked = capture.events.stream()
                .filter(e -> "session.forked".equals(e.type())).findFirst().orElseThrow();
        assertThat(forked.payload()).containsEntry("sourceSessionId", "sess-src");
        assertThat(forked.payload()).containsEntry("copiedMessages", 2); // 一问一答
        assertThat(forked.payload()).containsEntry("copiedSummary", false);
        branch.close();
    }

    /** 时间旅行 fork（forkFromTurn）：同样写谱系 + 事件带 upToTurn 与前缀消息数。 */
    @Test
    void timeTravelForkWritesLineageAndCounts() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        model.enqueueText("a2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        EventCaptureHook capture = new EventCaptureHook();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(RuntimeConfig.defaults(),
                        RuntimeConfig.hooks(List.of(capture))));

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1");
        source.chat("q2");
        source.close();

        AgentSession branch = runtime.forkFromTurn("sess-src", "app", "agent", "sess-tt", 1);

        assertThat(stores.sessionStateStore().getAll("sess-tt").get("buzhou.fork.source").value())
                .isEqualTo("sess-src");
        SessionEvent forked = capture.events.stream()
                .filter(e -> "session.forked".equals(e.type())).findFirst().orElseThrow();
        assertThat(forked.payload()).containsEntry("upToTurn", 1);
        assertThat(forked.payload()).containsEntry("copiedMessages", 2);
        branch.close();
    }

    /** 导出携带谱系：exportSession 的 state 段含 buzhou.fork.source（跨环境移植不丢谱系）。 */
    @Test
    void exportCarriesLineage() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession source = runtime.spawn("app", "agent", "sess-src");
        source.chat("q1");
        source.close();
        AgentSession branch = runtime.fork("sess-src", "app", "agent", "sess-branch");
        branch.close();

        SessionExport export = runtime.exportSession("sess-branch");
        assertThat(export.state()).containsKey("buzhou.fork.source");
    }
}
