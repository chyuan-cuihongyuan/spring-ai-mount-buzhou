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
        stores().sessionLeaseStore().tryAcquire(sessionId, "owner-A", Duration.ofMillis(50));
        assertThat(stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-B", Duration.ofSeconds(90)).acquired()).isFalse();
        awaitExpiry();
        assertThat(stores().sessionLeaseStore()
                .tryAcquire(sessionId, "owner-B", Duration.ofSeconds(90)).acquired()).isTrue();
    }

    protected void awaitExpiry() {
        try {
            Thread.sleep(150);
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
}
