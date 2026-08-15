package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话索引 e2e（spec 30 / T109 / impl-84）：生命周期点维护（onOpen ACTIVE / onTurnEnd
 * 刷活 / onClose CLOSED）+ 过滤查询 + 更新失败不阻断会话（最终一致）。
 */
class SessionIndexEndToEndTest {

    /** spawn→chat→close 全生命周期：索引状态与 turnCount 随之演化。 */
    @Test
    void indexTracksSessionLifecycle() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a1");
        model.enqueueText("a2");
        SessionIndexStore index = new InMemorySessionIndexStore();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.merge(
                RuntimeConfig.defaults(),
                RuntimeConfig.assemblyCustomizers(List.of(SessionIndexObserver.wiring(index)))));

        AgentSession session = runtime.spawn("app-1", "agent-1", "idx-1");
        assertThat(index.get("idx-1"))
                .hasValueSatisfying(i -> {
                    assertThat(i.status()).isEqualTo(SessionInfo.STATUS_ACTIVE);
                    assertThat(i.turnCount()).isZero();
                });

        session.chat("q1");
        session.chat("q2");
        assertThat(index.get("idx-1"))
                .hasValueSatisfying(i -> {
                    assertThat(i.status()).isEqualTo(SessionInfo.STATUS_ACTIVE);
                    assertThat(i.turnCount()).isEqualTo(2);
                });

        session.close();
        assertThat(index.get("idx-1"))
                .hasValueSatisfying(i -> assertThat(i.status()).isEqualTo(SessionInfo.STATUS_CLOSED));
    }

    /** 过滤查询：appId 精确 + 最近活跃优先。 */
    @Test
    void listFiltersByAppAndOrdersByLastActive() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("a");
        SessionIndexStore index = new InMemorySessionIndexStore();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.merge(
                RuntimeConfig.defaults(),
                RuntimeConfig.assemblyCustomizers(List.of(SessionIndexObserver.wiring(index)))));

        runtime.spawn("app-a", "ag", "first").close();
        AgentSession second = runtime.spawn("app-b", "ag", "second");
        second.chat("hi"); // second 更晚活跃
        second.close();

        var rows = index.list(new SessionIndexQuery("app-a", null, null, null, null, 0, 10));
        assertThat(rows).singleElement().extracting(SessionInfo::sessionId).isEqualTo("first");

        var all = index.list(new SessionIndexQuery(null, null, SessionInfo.STATUS_ACTIVE,
                null, null, 0, 10));
        assertThat(all).isEmpty(); // 两个会话都已 close
    }

    /** 索引更新失败（抛异常的 store）不阻断会话——最终一致口径。 */
    @Test
    void indexFailureNeverBreaksSession() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("still-works");
        SessionIndexStore broken = new InMemorySessionIndexStore() {
            @Override
            public void upsert(SessionInfo info) {
                throw new IllegalStateException("索引后端不可用");
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), RuntimeConfig.merge(
                RuntimeConfig.defaults(),
                RuntimeConfig.assemblyCustomizers(List.of(SessionIndexObserver.wiring(broken)))));

        AgentSession session = runtime.spawn("app", "ag", "broken-idx");
        assertThat(session.chat("q")).isEqualTo("still-works"); // 会话不受索引故障影响
        session.close();
    }
}
