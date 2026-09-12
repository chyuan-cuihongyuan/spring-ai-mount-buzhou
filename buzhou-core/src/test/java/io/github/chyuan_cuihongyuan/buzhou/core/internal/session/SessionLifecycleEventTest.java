package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 522 / T795：会话生命周期事件补齐——session.opened（spawn 派发、
 * 全局监听可达、payload 身份三元组）与既有 session.closed 配对。
 */
class SessionLifecycleEventTest {

    @Test
    void spawnEmitsSessionOpenedBeforeAnyTurn() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("回复");
        BuzhouStores stores = Buzhou.inMemoryStores();
        var runtime = (DefaultAgentRuntime) Buzhou.runtime(model, stores);
        List<String> seenTypes = new CopyOnWriteArrayList<>();
        runtime.addGlobalEventListener(event -> seenTypes.add(event.type()));

        AgentSession session = runtime.spawn("app", "agent", "sess-life");
        // opened 在 spawn 即派发（先于任何轮次）
        assertThat(seenTypes).containsExactly("session.opened");

        session.chat("hi");
        session.close();
        // 配对：closed 在 opened 之后
        assertThat(seenTypes).containsExactly("session.opened", "session.closed");
    }

    @Test
    void openedPayloadCarriesIdentityTriple() {
        ScriptedChatModel model = new ScriptedChatModel();
        var runtime = (DefaultAgentRuntime) Buzhou.runtime(model, Buzhou.inMemoryStores());
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        runtime.addGlobalEventListener(events::add);

        runtime.spawn("my-app", "my-agent", "sess-id-1").close();

        SessionEvent opened = events.stream()
                .filter(e -> e.type().equals("session.opened")).findFirst().orElseThrow();
        assertThat(opened.payload().get("appId")).isEqualTo("my-app");
        assertThat(opened.payload().get("agentName")).isEqualTo("my-agent");
        assertThat(opened.payload().get("sessionId")).isEqualTo("sess-id-1");
    }
}
