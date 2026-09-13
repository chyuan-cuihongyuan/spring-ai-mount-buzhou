package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditIngestStatsTest {

    @Test
    void freshCollectorHasZeroCounts() {
        AuditTrailCollector collector = new AuditTrailCollector(new AuditChain("a", "1"));

        assertThat(collector.stats()).isEqualTo(new AuditTrailCollector.AuditIngestStats(0, 0, 0));
    }

    @Test
    void auditedTypeCountedAndTrackedBySession() {
        AuditChain chain = new AuditChain("a", "1");
        AuditTrailCollector collector = new AuditTrailCollector(chain, null);

        collector.onEvent(new SessionEvent("guard.taint.blocked",
                Map.of("sessionId", "s1", "toolName", "rm"), java.time.Instant.now()));

        AuditTrailCollector.AuditIngestStats stats = collector.stats();
        assertThat(stats.collected()).isEqualTo(1);
        assertThat(stats.persistFailures()).isZero();
        assertThat(stats.openSessions()).isEqualTo(1);
    }

    @Test
    void nonAuditedTypeNotCounted() {
        AuditTrailCollector collector = new AuditTrailCollector(new AuditChain("a", "1"), null);

        collector.onEvent(new SessionEvent("random.type", Map.of(), java.time.Instant.now()));

        assertThat(collector.stats().collected()).isZero();
    }

    @Test
    void persistFailureCountedButChainUnaffected() {
        AuditChain chain = new AuditChain("a", "1");
        AuditRecordStore failing = new AuditRecordStore() {
            @Override
            public void append(AgentAuditRecord record) {
                throw new IllegalStateException("db down");
            }

            @Override
            public java.util.List<AgentAuditRecord> loadAll() {
                return java.util.List.of();
            }

            @Override
            public long count() {
                return 0;
            }
        };
        AuditTrailCollector collector = new AuditTrailCollector(chain, failing);

        collector.onEvent(new SessionEvent("guard.tool.blocked",
                Map.of("sessionId", "s1"), java.time.Instant.now()));

        AuditTrailCollector.AuditIngestStats stats = collector.stats();
        assertThat(stats.collected()).isEqualTo(1);
        assertThat(stats.persistFailures()).isEqualTo(1);
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
    }

    @Test
    void closeBroadcastClearsOpenSessionTracking() {
        AuditChain chain = new AuditChain("a", "1");
        AuditTrailCollector collector = new AuditTrailCollector(chain, null);

        collector.onEvent(new SessionEvent("guard.tool.blocked",
                Map.of("sessionId", "s1"), java.time.Instant.now()));
        collector.onEvent(SessionEvent.of("session.closed"));

        assertThat(collector.stats().openSessions()).isZero();
        assertThat(collector.stats().collected()).isEqualTo(1);
    }
}
