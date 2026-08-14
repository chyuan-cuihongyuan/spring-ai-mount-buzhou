package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleanupContributor;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoverySupport;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-35 / spec 13 §stores-6（deleteSession 级联清理）端到端（FakeChatModel 驱动）：
 *
 * <ul>
 *   <li><b>一次调用清干净</b>：session.delete() 先 close（租约释放）再 SessionCleaner
 *       一次级联——五槽 store + RecoverySupport 挂接的 run-registry / tool-call-log +
 *       RuntimeConfig.cleanupContributors 挂接的外部贡献者（spill 文件同形状）全清空，
 *       其他会话不受影响；</li>
 *   <li><b>失败聚合不跳过</b>：贡献者抛异常时 delete() 上抛，但其余目标照常清理。</li>
 * </ul>
 */
class DeleteSessionCascadeEndToEndTest {

    @Test
    void deleteCascadesStoresRecoveryAndContributors() {
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        InMemoryToolCallLog toolCallLog = new InMemoryToolCallLog();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicBoolean spillProbeDeleted = new AtomicBoolean();

        FakeChatModel model = FakeChatModel.script(ScriptStep.text("第一轮完成"));
        FakeModelGuard.requireTestDouble(model);
        RuntimeConfig config = RuntimeConfig.merge(
                RecoverySupport.attach(RuntimeConfig.defaults(), registry, toolCallLog, "del-app"),
                RuntimeConfig.cleanupContributors(List.of(SessionCleanupContributor.of(
                        "spill-probe", sid -> spillProbeDeleted.set(true)))));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);

        String sessionId = "del-" + UUID.randomUUID();
        AgentSession session = runtime.spawn("del-app", "agent", sessionId);
        assertThat(session.chat("你好")).isEqualTo("第一轮完成");

        // 铺底：chat 已产消息与 run 快照；观测/摘要/状态/工具日志直接铺（各机制写入路径不同）
        stores.observabilityStore().saveSpans(List.of(new SpanRecord(
                "sp-" + sessionId, null, sessionId, 1, "TURN", "turn-1",
                Instant.now(), null, "OK", Map.of())));
        stores.observabilityStore().saveEvents(List.of(new EventRecord(
                "ev-" + sessionId, "sp-" + sessionId, sessionId, "Thinking",
                Instant.now(), Map.of())));
        stores.summaryStore().save(sessionId,
                new StructuredSummary(sessionId, 0, Map.of("P0", "a"), 9, Instant.now()));
        stores.sessionStateStore().put(sessionId,
                new StateEntry("fact.a", "v1", "hook", 1, null, Instant.now()));
        toolCallLog.append(new ToolCallLogEntry(sessionId, "call-1", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", Instant.now()));
        String otherSession = "keep-" + UUID.randomUUID();
        stores.messageStore().append(otherSession, List.of(new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                UUID.randomUUID().toString(), otherSession, 1, 0,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                "别的会话", List.of(), null, null, null, Map.of(), Instant.now())));
        assertThat(stores.messageStore().load(sessionId)).isNotEmpty();
        assertThat(registry.find(sessionId)).isPresent();

        session.delete();

        assertThat(stores.messageStore().load(sessionId)).isEmpty();
        assertThat(stores.summaryStore().latest(sessionId)).isEmpty();
        assertThat(stores.sessionStateStore().getAll(sessionId)).isEmpty();
        assertThat(stores.sessionLeaseStore().inspect(sessionId)).isEmpty();
        assertThat(stores.observabilityStore().spansOfSession(sessionId)).isEmpty();
        assertThat(stores.observabilityStore().eventsOfSession(sessionId)).isEmpty();
        assertThat(registry.find(sessionId)).isEmpty();
        assertThat(toolCallLog.find(sessionId, "call-1")).isEmpty();
        assertThat(spillProbeDeleted).isTrue();
        assertThat(stores.messageStore().load(otherSession)).hasSize(1); // 其他会话不动
        assertThatThrownBy(() -> session.chat("删除后"))
                .hasMessageContaining("already closed");
    }

    @Test
    void deleteAggregatesContributorFailureWithoutSkippingOtherTargets() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("好"));
        FakeModelGuard.requireTestDouble(model);
        RuntimeConfig config = RuntimeConfig.cleanupContributors(List.of(
                SessionCleanupContributor.of("bad-contributor", sid -> {
                    throw new IllegalStateException("bad cleanup");
                })));
        AgentRuntime runtime = Buzhou.runtime(model, stores, config);
        AgentSession session = runtime.spawn("del-app", "agent", "del-fail-" + UUID.randomUUID());
        session.chat("你好");
        assertThat(stores.messageStore().load(session.sessionId())).isNotEmpty();

        // 贡献者失败上抛（首个失败），但五槽 store 照常清理完毕
        assertThatThrownBy(session::delete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bad cleanup");
        assertThat(stores.messageStore().load(session.sessionId())).isEmpty();
        assertThat(stores.sessionLeaseStore().inspect(session.sessionId())).isEmpty();
    }
}
