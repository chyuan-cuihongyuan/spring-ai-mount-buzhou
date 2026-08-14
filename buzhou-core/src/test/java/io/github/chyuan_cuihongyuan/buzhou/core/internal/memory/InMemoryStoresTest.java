package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryStoresTest {

    @Test
    void summaryStoreAssignsMonotonicVersionsAndReadsLatest() {
        InMemorySummaryStore store = new InMemorySummaryStore();
        long v1 = store.save("s1", new StructuredSummary("s1", 0, Map.of("P0", "a"), 10, Instant.now()));
        long v2 = store.save("s1", new StructuredSummary("s1", 0, Map.of("P0", "b"), 20, Instant.now()));

        assertThat(v1).isEqualTo(1);
        assertThat(v2).isEqualTo(2);
        assertThat(store.latest("s1")).isPresent()
                .get().extracting(StructuredSummary::version).isEqualTo(2L);
        assertThat(store.history("s1", 10)).hasSize(2);
        assertThat(store.latest("unknown")).isEmpty();
    }

    @Test
    void stateStoreIsNamespacedKeyValuePerSession() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        store.put("s1", new StateEntry("fact.a", "v1", "hook", 3, 2, Instant.now()));
        store.put("s1", new StateEntry("auth.b", "v2", "guard", 1, null, Instant.now()));

        assertThat(store.get("s1", "fact.a")).isPresent()
                .get().extracting(StateEntry::value).isEqualTo("v1");
        assertThat(store.getAll("s1")).hasSize(2);
        assertThat(store.get("s2", "fact.a")).isEmpty();

        store.delete("s1", "fact.a");
        assertThat(store.get("s1", "fact.a")).isEmpty();
    }

    @Test
    void leaseStoreMutualExclusionStealAndFencing() {
        InMemorySessionLeaseStore store = new InMemorySessionLeaseStore();
        var first = store.tryAcquire("s1", "owner-A", Duration.ofSeconds(90));
        assertThat(first.acquired()).isTrue();
        assertThat(first.fencingToken()).isEqualTo(1);

        assertThat(store.tryAcquire("s1", "owner-B", Duration.ofSeconds(90)).acquired()).isFalse();

        assertThat(store.renew("s1", "owner-A", 1, Duration.ofSeconds(90))).isTrue();
        assertThat(store.renew("s1", "owner-B", 1, Duration.ofSeconds(90))).isFalse();

        var stolen = store.steal("s1", "owner-B", Duration.ofSeconds(90));
        assertThat(stolen.acquired()).isTrue();
        assertThat(stolen.fencingToken()).isEqualTo(2);
        assertThat(store.renew("s1", "owner-A", 1, Duration.ofSeconds(90))).isFalse();

        store.release("s1", "owner-B", 2);
        assertThat(store.inspect("s1")).isEmpty();
    }

    @Test
    void leaseExpiresNaturally() {
        InMemorySessionLeaseStore store = new InMemorySessionLeaseStore();
        store.tryAcquire("s1", "owner-A", Duration.ofMillis(1));
        assertThat(store.tryAcquire("s1", "owner-B", Duration.ofSeconds(90)).acquired()).isFalse();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(store.tryAcquire("s1", "owner-B", Duration.ofSeconds(90)).acquired()).isTrue();
    }

    @Test
    void shouldPhysicallyRemoveExpiredLease_whenLazyInspectRuns() {
        InMemorySessionLeaseStore store = new InMemorySessionLeaseStore();
        store.tryAcquire("s1", "owner-A", Duration.ofMillis(1));
        assertThat(store.leaseCount()).isEqualTo(1);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(store.inspect("s1")).isEmpty();
        assertThat(store.leaseCount())
                .as("过期租约在惰性判定时被物理移除（非仅判定失效）")
                .isZero();
    }

    @Test
    void shouldFailRenewAndPhysicallyRemove_whenLeaseExpired() {
        InMemorySessionLeaseStore store = new InMemorySessionLeaseStore();
        store.tryAcquire("s1", "owner-A", Duration.ofMillis(1));
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(store.renew("s1", "owner-A", 1, Duration.ofSeconds(90)))
                .as("过期租约不可再取（renew 不复活已过期租约）")
                .isFalse();
        assertThat(store.leaseCount()).isZero();
    }

    @Test
    void observabilityStoreRoundTripsSpansEventsAndSnapshots() {
        InMemoryObservabilityStore store = new InMemoryObservabilityStore();
        SpanRecord span = new SpanRecord("sp1", null, "s1", 1, "Turn", "turn-1",
                Instant.now(), null, "RUNNING", Map.of());
        EventRecord event = new EventRecord("ev1", "sp1", "s1", "Thinking", Instant.now(), Map.of("text", "t"));
        store.saveSpans(List.of(span));
        store.saveEvents(List.of(event));
        store.saveInjectionSnapshot(new InjectionSnapshot("s1", 1, List.of("m1"), Map.of("budget", 100), Instant.now()));

        assertThat(store.spansOfSession("s1")).containsExactly(span);
        assertThat(store.eventsOfSession("s1")).containsExactly(event);
        assertThat(store.injectionSnapshot("s1", 1)).isPresent();
        assertThat(store.injectionSnapshot("s1", 2)).isEmpty();
    }

    @Test
    void unitOfWorkSerializesWritesPerSession() {
        InMemoryUnitOfWork uow = new InMemoryUnitOfWork();
        String result = uow.executeInTransaction(() -> "done");
        assertThat(result).isEqualTo("done");
    }
}
