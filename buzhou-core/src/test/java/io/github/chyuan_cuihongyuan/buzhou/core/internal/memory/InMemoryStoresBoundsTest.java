package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-36 / spec 13 §growth-8：内存套件有界化——事实台账 noeviction 拒绝（类型正确、
 * 原子不留半份）、可再生集合（观测）采样逐出只碰自己、会话级联删除后内存回落
 * （含 InMemoryUnitOfWork 锁对象随会话移除）。
 */
class InMemoryStoresBoundsTest {

    private static final InMemoryStoreConfig TIGHT =
            new InMemoryStoreConfig(2, 3, 2, 4);    @Test
    void factLedgerRejectsNewSessionsBeyondMaxWithQuotaExceeded() {
        InMemoryMessageStore messages = new InMemoryMessageStore(TIGHT);
        InMemorySummaryStore summaries = new InMemorySummaryStore(TIGHT);
        InMemorySessionStateStore states = new InMemorySessionStateStore(TIGHT);
        // 各台账独立计会话（同一会话写消息+摘要+状态 = 三个台账各登记一次）
        messages.append("s1", List.of(msg("s1")));
        messages.append("s2", List.of(msg("s2")));
        summaries.save("s1", new StructuredSummary("s1", 0, Map.of(), 1, Instant.now()));
        summaries.save("s2", new StructuredSummary("s2", 0, Map.of(), 1, Instant.now()));
        states.put("s1", new StateEntry("k", "v", "hook", 1, null, Instant.now()));
        states.put("s2", new StateEntry("k", "v", "hook", 1, null, Instant.now()));

        // 第 3 个新会话：三类事实台账一致拒绝，类型正确（QuotaExceeded / NON_RETRYABLE 语义）
        assertThatThrownBy(() -> messages.append("s3", List.of(msg("s3"))))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("maxSessions=2");
        assertThatThrownBy(() -> summaries.save("s3",
                new StructuredSummary("s3", 0, Map.of(), 1, Instant.now())))
                .isInstanceOf(QuotaExceededException.class);
        assertThatThrownBy(() -> states.put("s3",
                new StateEntry("k", "v", "hook", 1, null, Instant.now())))
                .isInstanceOf(QuotaExceededException.class);
        // 既有会话不受影响；拒绝不留半份（s3 未被登记）
        messages.append("s1", List.of(msg("s1")));
        assertThat(messages.load("s3")).isEmpty();
        assertThat(messages.sessionCount()).isEqualTo(2);
    }

    @Test
    void factLedgerRejectsPerSessionMessagesBeyondCapAtomically() {
        InMemoryMessageStore messages = new InMemoryMessageStore(TIGHT);
        messages.append("s1", List.of(msg("s1"), msg("s1"), msg("s1"))); // 到达上限 3

        // 超限追加：整批拒绝（原子——不是写入 3 条后丢第 4 条，而是 4 条全不进）
        assertThatThrownBy(() -> messages.append("s1", List.of(msg("s1"), msg("s1"))))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("maxMessagesPerSession=3");
        assertThat(messages.messageCount("s1")).isEqualTo(3);

        // 会话释放后：新会话可入场（noeviction 释放空间后另行发起）
        messages.deleteSession("s1");
        messages.append("s3", List.of(msg("s3")));
        assertThat(messages.sessionCount()).isEqualTo(1);
    }

    @Test
    void observabilityEvictsOnlyRegenerableSessionsAndKeepsFacts() {
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore(TIGHT);
        InMemoryMessageStore messages = new InMemoryMessageStore(TIGHT);
        messages.append("s1", List.of(msg("s1")));
        messages.append("s2", List.of(msg("s2")));

        // 观测 2 个会话（达到 maxObservabilitySessions=2）
        observability.saveSpans(List.of(span("s1", "sp-1")));
        observability.saveEvents(List.of(event("s1", "ev-1")));
        observability.saveInjectionSnapshot(
                new InjectionSnapshot("s1", 1, List.of("m1"), Map.of(), Instant.now()));
        observability.saveSpans(List.of(span("s2", "sp-2")));

        // 第 3 个观测会话入场：容量触发采样逐出——候选 ≤ 采样数（精确扫描），
        // s1（最久未活跃）被逐；事实台账（messages）原封不动
        observability.saveSpans(List.of(span("s3", "sp-3")));

        assertThat(observability.spansOfSession("s1")).isEmpty();
        assertThat(observability.eventsOfSession("s1")).isEmpty();
        assertThat(observability.injectionSnapshot("s1", 1)).isEmpty();
        assertThat(observability.spansOfSession("s3")).hasSize(1);
        assertThat(observability.sessionCount()).isEqualTo(2); // 容量回落到上限内
        assertThat(messages.sessionCount()).isEqualTo(2); // 事实台账不逐出
        assertThat(messages.load("s1")).hasSize(1);
    }

    /** impl-36：仅写快照的会话同样计入观测容量、可被逐出（不留无界洞）。 */
    @Test
    void snapshotOnlySessionsAreAdmittedAndEvictable() {
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore(TIGHT);
        observability.saveInjectionSnapshot(
                new InjectionSnapshot("snap-s1", 1, List.of("m1"), Map.of(), Instant.now()));
        observability.saveInjectionSnapshot(
                new InjectionSnapshot("snap-s2", 1, List.of("m1"), Map.of(), Instant.now()));
        assertThat(observability.sessionCount()).isEqualTo(2);

        // 第 3 个仅快照会话入场 → 最久未活跃的 snap-s1 被逐（其快照随之移除）
        observability.saveInjectionSnapshot(
                new InjectionSnapshot("snap-s3", 1, List.of("m1"), Map.of(), Instant.now()));

        assertThat(observability.injectionSnapshot("snap-s1", 1)).isEmpty();
        assertThat(observability.injectionSnapshot("snap-s3", 1)).isPresent();
        assertThat(observability.sessionCount()).isEqualTo(2);
    }

    @Test
    void observabilityDropsOldestPerSessionRecordsVisibly() {
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore(TIGHT);
        // 单会话 6 条事件 > maxObservabilityRecordsPerSession=4：丢最旧 2 条，计数可见
        observability.saveEvents(List.of(
                event("s1", "ev-1"), event("s1", "ev-2"), event("s1", "ev-3"),
                event("s1", "ev-4"), event("s1", "ev-5"), event("s1", "ev-6")));

        assertThat(observability.eventsOfSession("s1"))
                .extracting(EventRecord::eventId)
                .containsExactly("ev-3", "ev-4", "ev-5", "ev-6");
        assertThat(observability.droppedRecordsCount()).isEqualTo(2);

        // spans 同款 FIFO 上限（长跑活跃会话的 span 流水亦有界）
        observability.saveSpans(List.of(
                span("s1", "sp-1"), span("s1", "sp-2"), span("s1", "sp-3"),
                span("s1", "sp-4"), span("s1", "sp-5")));
        assertThat(observability.spansOfSession("s1"))
                .extracting(SpanRecord::spanId)
                .containsExactly("sp-2", "sp-3", "sp-4", "sp-5");
        assertThat(observability.droppedRecordsCount()).isEqualTo(3);
    }

    @Test
    void sessionCleanerReleaseMemoryIncludingUnitOfWorkLocks() {
        InMemoryMessageStore messages = new InMemoryMessageStore(TIGHT);
        InMemorySummaryStore summaries = new InMemorySummaryStore(TIGHT);
        InMemorySessionStateStore states = new InMemorySessionStateStore(TIGHT);
        InMemoryObservabilityStore observability = new InMemoryObservabilityStore(TIGHT);
        InMemoryUnitOfWork uow = new InMemoryUnitOfWork();
        BuzhouStores stores = new BuzhouStores(messages, summaries, states,
                new InMemorySessionLeaseStore(), observability, uow);

        messages.append("s1", List.of(msg("s1")));
        summaries.save("s1", new StructuredSummary("s1", 0, Map.of(), 1, Instant.now()));
        states.put("s1", new StateEntry("k", "v", "hook", 1, null, Instant.now()));
        observability.saveSpans(List.of(span("s1", "sp-1")));
        // 会话开过事务 → 常驻锁对象
        uow.executeInTransaction("s1", () -> "work");
        assertThat(uow.hasSessionLock("s1")).isTrue();

        cleanAndAssertMemoryReclaimed(stores, uow, messages, summaries, states, observability);
    }

    private void cleanAndAssertMemoryReclaimed(BuzhouStores stores, InMemoryUnitOfWork uow,
                                               InMemoryMessageStore messages,
                                               InMemorySummaryStore summaries,
                                               InMemorySessionStateStore states,
                                               InMemoryObservabilityStore observability) {
        var result = new SessionCleaner(stores).deleteSession("s1");
        assertThat(result.fullyCleaned()).isTrue();
        // 内存回落：全台账清零 + UoW 锁随会话移除（长跑进程不慢性泄漏）
        assertThat(messages.sessionCount()).isZero();
        assertThat(summaries.sessionCount()).isZero();
        assertThat(states.sessionCount()).isZero();
        assertThat(observability.sessionCount()).isZero();
        assertThat(uow.hasSessionLock("s1")).isFalse();
        assertThat(uow.lockCount()).isZero();
    }

    private static BuzhouMessage msg(String sessionId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                "content", List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static SpanRecord span(String sessionId, String spanId) {
        return new SpanRecord(spanId, null, sessionId, 1, "TURN", "turn-1",
                Instant.now(), null, "OK", Map.of());
    }

    private static EventRecord event(String sessionId, String eventId) {
        return new EventRecord(eventId, null, sessionId, "Thinking", Instant.now(), Map.of());
    }
}
