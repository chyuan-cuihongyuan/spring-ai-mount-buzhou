package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-35 / spec 13 §stores-6：SessionCleaner 协调器单元测试——
 * 一次级联覆盖五槽 store + 恢复设施 + 外部贡献者；逐目标隔离、失败聚合报告；空槽跳过。
 */
class SessionCleanerTest {

    @Test
    void cascadesAcrossStoresRecoveryAndContributors() {
        InMemoryMessageStore messages = new InMemoryMessageStore();
        InMemorySummaryStore summaries = new InMemorySummaryStore();
        InMemorySessionStateStore states = new InMemorySessionStateStore();
        InMemorySessionLeaseStore leases = new InMemorySessionLeaseStore();
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore();
        InMemoryRunRegistry runRegistry = new InMemoryRunRegistry();
        InMemoryToolCallLog toolCallLog = new InMemoryToolCallLog();
        BuzhouStores stores = new BuzhouStores(messages, summaries, states, leases, observability,
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryUnitOfWork());

        String sessionId = "clean-" + UUID.randomUUID();
        String otherSession = "other-" + UUID.randomUUID();
        messages.append(sessionId, List.of(message(sessionId)));
        messages.append(otherSession, List.of(message(otherSession)));
        summaries.save(sessionId, new StructuredSummary(sessionId, 0, Map.of("P0", "a"), 9, Instant.now()));
        states.put(sessionId, new StateEntry("fact.a", "v1", "hook", 1, null, Instant.now()));
        leases.tryAcquire(sessionId, "owner-1", Duration.ofSeconds(60));
        observability.saveSpans(List.of(new SpanRecord("sp-1", null, sessionId, 1, "TURN",
                "turn-1", Instant.now(), null, "OK", Map.of())));
        observability.saveEvents(List.of(new EventRecord("ev-1", "sp-1", sessionId, "Thinking",
                Instant.now(), Map.of())));
        observability.saveInjectionSnapshot(new InjectionSnapshot(sessionId, 1,
                List.of("m1"), Map.of(), Instant.now()));
        runRegistry.save(new RunStateSnapshot(sessionId, "app", "agent", RunStatus.RUNNING,
                2, 1, "owner-1", Instant.now()));
        toolCallLog.append(new ToolCallLogEntry(sessionId, "call-1", "echo", "hash-1",
                ToolCallOutcome.COMPLETED, "ok", Instant.now()));

        AtomicBoolean contributorRan = new AtomicBoolean();
        SessionCleaner cleaner = new SessionCleaner(stores, runRegistry, toolCallLog)
                .withContributor("spill-files", sid -> {
                    assertThat(sid).isEqualTo(sessionId);
                    contributorRan.set(true);
                });

        SessionCleanupResult result = cleaner.deleteSession(sessionId);

        assertThat(result.fullyCleaned()).isTrue();
        assertThat(result.cleaned()).containsExactly(
                "message-store", "summary-store", "session-state-store", "session-lease-store",
                "observability-store", "run-registry", "tool-call-log", "spill-files");
        assertThat(messages.load(sessionId)).isEmpty();
        assertThat(messages.load(otherSession)).hasSize(1); // 其他会话不受影响
        assertThat(summaries.latest(sessionId)).isEmpty();
        assertThat(states.getAll(sessionId)).isEmpty();
        assertThat(leases.inspect(sessionId)).isEmpty();
        assertThat(observability.spansOfSession(sessionId)).isEmpty();
        assertThat(observability.eventsOfSession(sessionId)).isEmpty();
        assertThat(observability.injectionSnapshot(sessionId, 1)).isEmpty();
        assertThat(runRegistry.find(sessionId)).isEmpty();
        assertThat(toolCallLog.find(sessionId, "call-1")).isEmpty();
        assertThat(contributorRan).isTrue();
    }

    @Test
    void isolatesFailuresAndAggregatesReport() {
        ObservabilityStore throwingObservability = new InMemoryObservabilityStore() {
            @Override
            public void deleteSession(String sessionId) {
                throw new IllegalStateException("observability down");
            }
        };
        InMemoryMessageStore messages = new InMemoryMessageStore();
        BuzhouStores stores = new BuzhouStores(messages, new InMemorySummaryStore(),
                new InMemorySessionStateStore(), new InMemorySessionLeaseStore(),
                throwingObservability, null);
        String sessionId = "clean-fail-" + UUID.randomUUID();
        messages.append(sessionId, List.of(message(sessionId)));

        SessionCleanupResult result = SessionCleaner.of(stores,
                        List.of(SessionCleanupContributor.of("probe", sid -> { })))
                .deleteSession(sessionId);

        assertThat(result.fullyCleaned()).isFalse();
        assertThat(result.failures()).containsOnlyKeys("observability-store");
        assertThat(result.failures().get("observability-store"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("observability down");
        // 失败被隔离：其余目标（含贡献者）照常清理
        assertThat(result.cleaned()).containsExactly(
                "message-store", "summary-store", "session-state-store", "session-lease-store", "probe");
        assertThat(messages.load(sessionId)).isEmpty();
    }

    @Test
    void skipsNullSlotsAndRecoversInContributorsList() {
        // 空槽组合（enhance 等非会话路径可能出现 null 槽）：跳过、不 NPE、不进报告
        BuzhouStores sparse = new BuzhouStores(new InMemoryMessageStore(), null, null, null, null, null);
        SessionCleanupResult result = SessionCleaner.of(sparse, List.of()).deleteSession("any-session");
        assertThat(result.fullyCleaned()).isTrue();
        assertThat(result.cleaned()).containsExactly("message-store");
    }

    @Test
    void runtimeConfigContributorsFlowIntoCleaner() {
        InMemoryRunRegistry runRegistry = new InMemoryRunRegistry();
        InMemoryToolCallLog toolCallLog = new InMemoryToolCallLog();
        String sessionId = "attach-" + UUID.randomUUID();
        runRegistry.save(new RunStateSnapshot(sessionId, "app", "agent", RunStatus.RUNNING,
                1, 0, "owner", Instant.now()));
        toolCallLog.append(new ToolCallLogEntry(sessionId, "call-1", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", Instant.now()));

        // RecoverySupport.attach 的等价形状：贡献者经 RuntimeConfig 汇入 SessionCleaner.of
        List<SessionCleanupContributor> contributors = List.of(
                SessionCleanupContributor.of("run-registry", runRegistry::deleteSession),
                SessionCleanupContributor.of("tool-call-log", toolCallLog::deleteSession));
        SessionCleaner cleaner = SessionCleaner.of(new BuzhouStores(new InMemoryMessageStore(),
                new InMemorySummaryStore(), new InMemorySessionStateStore(),
                new InMemorySessionLeaseStore(), new InMemoryObservabilityStore(), null),
                contributors);

        SessionCleanupResult result = cleaner.deleteSession(sessionId);
        assertThat(result.cleaned()).contains("run-registry", "tool-call-log");
        assertThat(runRegistry.find(sessionId)).isEmpty();
        assertThat(toolCallLog.find(sessionId, "call-1")).isEmpty();
    }

    private static BuzhouMessage message(String sessionId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                "content", List.of(), null, null, null, Map.of(), Instant.now());
    }
}
