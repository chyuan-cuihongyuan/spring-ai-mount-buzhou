package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractBuzhouStoresContractTest {

    /** 租约自然过期用例的短 TTL：需足够宽，避免并行构建负载下两次 tryAcquire 间隔超过 TTL 造成偶发失败。 */
    private static final Duration LEASE_EXPIRY_TEST_TTL = Duration.ofMillis(500);
    /** 过期等待时长：须大于 {@link #LEASE_EXPIRY_TEST_TTL}，并留出调度抖动余量。 */
    private static final long LEASE_EXPIRY_AWAIT_MS = 1_200L;

    protected abstract BuzhouStores stores();

    protected abstract void cleanUp();

    protected BuzhouMessage msg(String sessionId, int turnSeq, int seqInTurn, Role role) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turnSeq, seqInTurn,
                role, "content-" + turnSeq + "-" + seqInTurn, List.of(), null, null, null,
                Map.of(), Instant.now());
    }

    @Test
    void messageStoreAppendsAndLoadsInOrder() {
        String sessionId = "contract-msg-" + UUID.randomUUID();
        stores().messageStore().append(sessionId, List.of(msg(sessionId, 2, 0, Role.USER)));
        stores().messageStore().append(sessionId,
                List.of(msg(sessionId, 1, 0, Role.USER), msg(sessionId, 1, 1, Role.ASSISTANT)));

        List<BuzhouMessage> loaded = stores().messageStore().load(sessionId);
        assertThat(loaded).extracting(m -> m.turnSeq() + ":" + m.seqInTurn())
                .containsExactly("1:0", "1:1", "2:0");

        BuzhouMessage first = loaded.getFirst();
        assertThat(stores().messageStore().findById(first.id())).isPresent();
    }

    @Test
    void summaryStoreVersionsMonotonically() {
        String sessionId = "contract-sum-" + UUID.randomUUID();
        long v1 = stores().summaryStore().save(sessionId,
                new StructuredSummary(sessionId, 0, Map.of("P0", "a"), 10, Instant.now()));
        long v2 = stores().summaryStore().save(sessionId,
                new StructuredSummary(sessionId, 0, Map.of("P0", "b"), 20, Instant.now()));

        assertThat(v2).isGreaterThan(v1);
        assertThat(stores().summaryStore().latest(sessionId)).isPresent()
                .get().extracting(StructuredSummary::version).isEqualTo(v2);
        assertThat(stores().summaryStore().history(sessionId, 10)).hasSize(2);
    }

    @Test
    void stateStoreIsNamespacedPerSession() {
        String sessionId = "contract-state-" + UUID.randomUUID();
        stores().sessionStateStore().put(sessionId,
                new StateEntry("fact.a", "v1", "hook", 3, 2, Instant.now()));

        assertThat(stores().sessionStateStore().get(sessionId, "fact.a")).isPresent()
                .get().extracting(StateEntry::value).isEqualTo("v1");
        assertThat(stores().sessionStateStore().getAll(sessionId)).hasSize(1);

        stores().sessionStateStore().delete(sessionId, "fact.a");
        assertThat(stores().sessionStateStore().get(sessionId, "fact.a")).isEmpty();
    }

    @Test
    void stateStoreDeleteIfValueMatchesIsConditional() {
        String sessionId = "contract-state-cas-" + UUID.randomUUID();
        stores().sessionStateStore().put(sessionId,
                new StateEntry("auth.t.fp", "v1", "guard", 3, null, Instant.now()));

        // value 不匹配 → 不删除，返回 false
        assertThat(stores().sessionStateStore().deleteIfValueMatches(sessionId, "auth.t.fp", "v2")).isFalse();
        assertThat(stores().sessionStateStore().get(sessionId, "auth.t.fp")).isPresent();
        // value 匹配 → 删除成功，返回 true；再次消费返回 false（一次性语义）
        assertThat(stores().sessionStateStore().deleteIfValueMatches(sessionId, "auth.t.fp", "v1")).isTrue();
        assertThat(stores().sessionStateStore().get(sessionId, "auth.t.fp")).isEmpty();
        assertThat(stores().sessionStateStore().deleteIfValueMatches(sessionId, "auth.t.fp", "v1")).isFalse();
    }

    @Test
    void leaseMutualExclusionStealRenewRelease() {
        String sessionId = "contract-lease-" + UUID.randomUUID();
        LeaseAcquireResult first = stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-A", Duration.ofSeconds(90));
        assertThat(first.acquired()).isTrue();

        assertThat(stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-B", Duration.ofSeconds(90)).acquired()).isFalse();
        assertThat(stores().sessionLeaseStore()
                .renew(sessionId, "owner-A", first.fencingToken(), Duration.ofSeconds(90))).isTrue();
        assertThat(stores().sessionLeaseStore()
                .renew(sessionId, "owner-B", first.fencingToken(), Duration.ofSeconds(90))).isFalse();

        LeaseAcquireResult stolen = stores().sessionLeaseStore()
                .steal(sessionId, "owner-B", Duration.ofSeconds(90));
        assertThat(stores().sessionLeaseStore()
                .renew(sessionId, "owner-A", first.fencingToken(), Duration.ofSeconds(90))).isFalse();

        stores().sessionLeaseStore().release(sessionId, "owner-B", stolen.fencingToken());
        assertThat(stores().sessionLeaseStore().inspect(sessionId)).isEmpty();
    }

    @Test
    void leaseExpiresNaturally() {
        String sessionId = "contract-lease-exp-" + UUID.randomUUID();
        stores().sessionLeaseStore().tryAcquire(sessionId, "owner-A", LEASE_EXPIRY_TEST_TTL);
        assertThat(stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-B", Duration.ofSeconds(90)).acquired()).isFalse();
        awaitExpiry();
        assertThat(stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-B", Duration.ofSeconds(90)).acquired()).isTrue();
    }

    protected void awaitExpiry() {
        try {
            Thread.sleep(LEASE_EXPIRY_AWAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void observabilityRoundTripsSpansEventsSnapshots() {
        String sessionId = "contract-obs-" + UUID.randomUUID();
        SpanRecord span = new SpanRecord("sp-" + UUID.randomUUID(), null, sessionId, 1, "Turn",
                "turn-1", Instant.now(), null, "RUNNING", Map.of("k", "v"));
        EventRecord event = new EventRecord("ev-" + UUID.randomUUID(), span.spanId(), sessionId,
                "Thinking", Instant.now(), Map.of("text", "t"));
        stores().observabilityStore().saveSpans(List.of(span));
        stores().observabilityStore().saveEvents(List.of(event));
        stores().observabilityStore().saveInjectionSnapshot(
                new InjectionSnapshot(sessionId, 1, List.of("m1"), Map.of("budget", 100), Instant.now()));

        assertThat(stores().observabilityStore().spansOfSession(sessionId))
                .extracting(SpanRecord::spanId).containsExactly(span.spanId());
        assertThat(stores().observabilityStore().eventsOfSession(sessionId))
                .extracting(EventRecord::eventId).containsExactly(event.eventId());
        assertThat(stores().observabilityStore().injectionSnapshot(sessionId, 1)).isPresent();
    }

    @Test
    void observabilityListsSessionSummariesAndEventsOfSpan() {
        String prefix = "contract-sum-" + UUID.randomUUID();
        String older = prefix + "-older";
        String newer = prefix + "-newer";
        Instant base = Instant.now();
        stores().observabilityStore().saveSpans(List.of(
                new SpanRecord("sp-sess-" + prefix, null, older, -1, "SESSION", "session",
                        base, base, "OK", Map.of("agent.name", "contract-agent")),
                new SpanRecord("sp-turn-" + prefix, "sp-sess-" + prefix, older, 1, "TURN",
                        "turn-1", base, base, "OK", Map.of())));
        stores().observabilityStore().saveSpans(List.of(
                new SpanRecord("sp-turn2-" + prefix, null, newer, 1, "TURN", "turn-1",
                        base.plusSeconds(60), base.plusSeconds(61), "OK", Map.of())));
        EventRecord event = new EventRecord("ev-" + prefix, "sp-turn2-" + prefix, newer,
                "FINAL_REPLY", base.plusSeconds(61), Map.of("content", "done"));
        EventRecord earlier = new EventRecord("ev0-" + prefix, "sp-turn2-" + prefix, newer,
                "THINKING", base.plusSeconds(60), Map.of("content", "想"));
        // 后入先存：断言实现按 occurredAt 排序返回，而非依赖写入序
        stores().observabilityStore().saveEvents(List.of(event, earlier));

        // offset 语义游标翻页拉全量（store 为全类共享，只断言相对序与字段，不断言全局位置）
        List<SessionSummary> all = new ArrayList<>();
        String cursor = null;
        for (int i = 0; i < 200; i++) {
            List<SessionSummary> page = stores().observabilityStore().listSessionSummaries(cursor, 3);
            if (page.isEmpty()) {
                break;
            }
            all.addAll(page);
            cursor = String.valueOf(all.size());
        }
        List<SessionSummary> mine = all.stream()
                .filter(s -> s.sessionId().startsWith(prefix)).toList();
        assertThat(mine).extracting(SessionSummary::sessionId)
                .containsExactly(newer, older); // 最近活跃降序

        SessionSummary olderSummary = mine.get(1);
        assertThat(olderSummary.turnCount()).isEqualTo(1);
        assertThat(olderSummary.spanCount()).isEqualTo(2);
        assertThat(olderSummary.sessionAttributes())
                .containsEntry("agent.name", "contract-agent");
        assertThat(olderSummary.firstActivityAt()).isNotNull();
        assertThat(olderSummary.lastActivityAt())
                .isAfterOrEqualTo(olderSummary.firstActivityAt());

        SessionSummary newerSummary = mine.get(0);
        assertThat(newerSummary.turnCount()).isEqualTo(1);
        assertThat(newerSummary.spanCount()).isEqualTo(1);
        assertThat(newerSummary.sessionAttributes()).isEmpty();

        assertThat(stores().observabilityStore().eventsOfSpan("sp-turn2-" + prefix))
                .extracting(EventRecord::eventId)
                .containsExactly(earlier.eventId(), event.eventId()); // occurredAt 升序
    }

    @Test
    void unitOfWorkExecutesAndPropagatesExceptions() {
        String result = stores().unitOfWork().executeInTransaction("session-x", () -> "done");
        assertThat(result).isEqualTo("done");

        assertThatThrownBy(() -> stores().unitOfWork().executeInTransaction("session-x", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
    }

    /**
     * impl-35 / spec 13 §stores-6：deleteSession 契约——五槽 store 各自 deleteSession 后
     * 全 store 无该会话残留（消息含按 id 索引 / 摘要全部版本 / state / 租约 / 观测
     * spans+events+注入快照+单 span event 索引+会话活跃索引项），且幂等（再删无异常）。
     */
    @Test
    void deleteSessionLeavesNoResidueAcrossStores() {
        String sessionId = "contract-del-" + UUID.randomUUID();
        BuzhouStores stores = stores();
        BuzhouMessage message = msg(sessionId, 1, 0, Role.USER);
        stores.messageStore().append(sessionId, List.of(message));
        stores.summaryStore().save(sessionId,
                new StructuredSummary(sessionId, 0, Map.of("P0", "a"), 10, Instant.now()));
        stores.sessionStateStore().put(sessionId,
                new StateEntry("fact.a", "v1", "hook", 1, null, Instant.now()));
        stores.sessionLeaseStore().tryAcquire(sessionId, "owner-del", Duration.ofSeconds(90));
        SpanRecord span = new SpanRecord("sp-del-" + UUID.randomUUID(), null, sessionId, 1, "TURN",
                "turn-1", Instant.now(), null, "RUNNING", Map.of());
        stores.observabilityStore().saveSpans(List.of(span));
        stores.observabilityStore().saveEvents(List.of(new EventRecord(
                "ev-del-" + UUID.randomUUID(), span.spanId(), sessionId, "Thinking",
                Instant.now(), Map.of())));
        stores.observabilityStore().saveInjectionSnapshot(
                new InjectionSnapshot(sessionId, 1, List.of("m1"), Map.of("budget", 1), Instant.now()));
        assertThat(stores.messageStore().findById(message.id())).isPresent(); // 铺底成立

        stores.messageStore().deleteSession(sessionId);
        stores.summaryStore().deleteSession(sessionId);
        stores.sessionStateStore().deleteSession(sessionId);
        stores.sessionLeaseStore().deleteSession(sessionId);
        stores.observabilityStore().deleteSession(sessionId);

        assertThat(stores.messageStore().load(sessionId)).isEmpty();
        assertThat(stores.messageStore().findById(message.id())).isEmpty();
        assertThat(stores.summaryStore().latest(sessionId)).isEmpty();
        assertThat(stores.summaryStore().history(sessionId, 10)).isEmpty();
        assertThat(stores.sessionStateStore().getAll(sessionId)).isEmpty();
        assertThat(stores.sessionLeaseStore().inspect(sessionId)).isEmpty();
        assertThat(stores.observabilityStore().spansOfSession(sessionId)).isEmpty();
        assertThat(stores.observabilityStore().eventsOfSession(sessionId)).isEmpty();
        assertThat(stores.observabilityStore().injectionSnapshot(sessionId, 1)).isEmpty();
        assertThat(stores.observabilityStore().eventsOfSpan(span.spanId())).isEmpty();
        List<SessionSummary> all = new ArrayList<>();
        String cursor = null;
        for (int i = 0; i < 200; i++) {
            List<SessionSummary> page = stores.observabilityStore().listSessionSummaries(cursor, 3);
            if (page.isEmpty()) {
                break;
            }
            all.addAll(page);
            cursor = String.valueOf(all.size());
        }
        assertThat(all).extracting(SessionSummary::sessionId).doesNotContain(sessionId);
        // 幂等：重复删除无异常、无残留变化
        stores.messageStore().deleteSession(sessionId);
        stores.observabilityStore().deleteSession(sessionId);
        assertThat(stores.messageStore().load(sessionId)).isEmpty();
    }

    /**
     * impl-37 / spec 13 §stores-6：保留策略契约——封闭会话枚举（活动会话永不出现在结果）、
     * 观测 TTL 批删（只删过期流水）、摘要版本修剪（保留最近 K 版）。
     */
    @Test
    void retentionPruneContract() {
        String closed = "contract-ret-closed-" + UUID.randomUUID();
        String active = "contract-ret-active-" + UUID.randomUUID();
        Instant old = Instant.now().minus(Duration.ofDays(8));
        // 封闭会话：SESSION span 已结束；活动会话：未结束
        stores().observabilityStore().saveSpans(List.of(
                new SpanRecord("sp-sess-" + closed, null, closed, -1, "SESSION", "session",
                        old.minus(Duration.ofHours(1)), old, "OK", Map.of()),
                new SpanRecord("sp-turn-" + closed, "sp-sess-" + closed, closed, 1, "TURN",
                        "turn-1", old, old, "OK", Map.of())));
        stores().observabilityStore().saveSpans(List.of(new SpanRecord(
                "sp-sess-" + active, null, active, -1, "SESSION", "session",
                Instant.now().minus(Duration.ofMinutes(5)), null, "RUNNING", Map.of())));

        // ① 封闭枚举：closed 在列（锚点=endedAt）、active 永不在列
        List<io.github.chyuan_cuihongyuan.buzhou.core.spi.ClosedSession> closedSessions =
                stores().observabilityStore().listClosedSessions(Instant.now(), 10);
        assertThat(closedSessions).extracting(io.github.chyuan_cuihongyuan.buzhou.core.spi.ClosedSession::sessionId)
                .contains(closed)
                .doesNotContain(active);

        // ② 观测 TTL（7 天）：8 天前的流水过期批删；活动会话的新 span 保留
        int pruned = stores().observabilityStore().prune(
                new io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl(
                        Duration.ofDays(7), 100));
        assertThat(pruned).isGreaterThanOrEqualTo(2); // 封闭会话的 SESSION+TURN span（活动 span 未过期）
        assertThat(stores().observabilityStore().spansOfSession(closed)).isEmpty();
        assertThat(stores().observabilityStore().spansOfSession(active)).hasSize(1);

        // ③ 摘要版本修剪：5 版 → 保留最近 2 版
        String summarySession = "contract-ret-sum-" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            stores().summaryStore().save(summarySession,
                    new StructuredSummary(summarySession, 0, Map.of("P0", "v" + i), 1, Instant.now()));
        }
        assertThat(stores().summaryStore().pruneVersions(2)).isEqualTo(3);
        assertThat(stores().summaryStore().history(summarySession, 10)).hasSize(2);
        assertThat(stores().summaryStore().latest(summarySession)).isPresent()
                .get().extracting(StructuredSummary::version).isEqualTo(5L);
    }
}
