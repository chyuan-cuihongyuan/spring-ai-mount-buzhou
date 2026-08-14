package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-39 / spec 13 §T64 收集器增强：记录即时落 AuditRecordStore +
 * session.closed 收尾发布该会话 sessionHash（审计记录形态入链）。
 */
class AuditTrailCollectorTest {

    @Test
    void recordsPersistToStoreImmediately() {
        AuditChain chain = new AuditChain("a", "1");
        InMemoryAuditRecordStore store = new InMemoryAuditRecordStore(8);
        AuditTrailCollector collector = new AuditTrailCollector(chain, store);

        collector.onEvent(new SessionEvent("guard.taint.blocked",
                Map.of("sessionId", "s1", "toolName", "rm"), java.time.Instant.now()));

        assertThat(store.count()).isEqualTo(1);
        assertThat(store.loadAll().getFirst().actionType()).isEqualTo("guard.taint.blocked");
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
    }

    @Test
    void sessionClosedPublishesPerSessionHashRecord() {
        AuditChain chain = new AuditChain("a", "1");
        InMemoryAuditRecordStore store = new InMemoryAuditRecordStore(8);
        AuditTrailCollector collector = new AuditTrailCollector(chain, store);

        collector.onEvent(new SessionEvent("guard.tool.blocked",
                Map.of("sessionId", "s1"), java.time.Instant.now()));
        collector.onEvent(new SessionEvent("guard.auth.granted",
                Map.of("sessionId", "s2"), java.time.Instant.now()));
        // 发布摘要覆盖「截至发布时」的会话记录（发布记录自身不进该摘要，自引用不可约）
        String s1HashBeforeClose = chain.sessionHash("s1");
        String s2HashBeforeClose = chain.sessionHash("s2");
        // 收尾广播（不带 sessionId）→ 每会话一条 sessionHash 发布记录
        collector.onEvent(SessionEvent.of("session.closed"));

        assertThat(chain.records()).hasSize(4);
        AgentAuditRecord closeS1 = chain.records().stream()
                .filter(r -> "audit.session.closed".equals(r.actionType()))
                .filter(r -> "s1".equals(r.sessionId())).findFirst().orElseThrow();
        AgentAuditRecord closeS2 = chain.records().stream()
                .filter(r -> "audit.session.closed".equals(r.actionType()))
                .filter(r -> "s2".equals(r.sessionId())).findFirst().orElseThrow();
        assertThat(closeS1.actionDetail()).contains(s1HashBeforeClose);
        assertThat(closeS2.actionDetail()).contains(s2HashBeforeClose);
        // 发布记录同样落库（全量重放可见收尾摘要）
        assertThat(store.count()).isEqualTo(4);
        // 收尾后链仍完整
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
    }

    @Test
    void repeatedCloseDoesNotDoublePublish() {
        AuditChain chain = new AuditChain("a", "1");
        AuditTrailCollector collector = new AuditTrailCollector(chain, null);
        collector.onEvent(new SessionEvent("guard.tool.blocked",
                Map.of("sessionId", "s1"), java.time.Instant.now()));
        collector.onEvent(SessionEvent.of("session.closed"));
        collector.onEvent(SessionEvent.of("session.closed"));
        assertThat(chain.records().stream()
                .filter(r -> "audit.session.closed".equals(r.actionType())).count()).isEqualTo(1);
    }

    @Test
    void inMemoryStoreIsBoundedAndEvictionVisible() {
        InMemoryAuditRecordStore store = new InMemoryAuditRecordStore(2);
        AuditChain chain = new AuditChain("a", "1");
        for (int i = 0; i < 3; i++) {
            AgentAuditRecord record = chain.append("s" + i, "guard.tool.blocked", "{}",
                    "BLOCKED");
            store.append(record);
        }
        assertThat(store.count()).isEqualTo(2);
        assertThat(store.evicted()).isEqualTo(1);
        assertThat(store.appended()).isEqualTo(3);
        assertThat(store.loadAll()).extracting(AgentAuditRecord::sessionId)
                .containsExactly("s1", "s2");
    }

    @Test
    void storeFailureIsExplicitButChainStaysVerifiable() {
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
        // 持久化失败不吞事件也不中断链
        collector.onEvent(new SessionEvent("guard.tool.blocked",
                Map.of("sessionId", "s1"), java.time.Instant.now()));
        assertThat(chain.records()).hasSize(1);
        assertThat(chain.verify((java.security.PublicKey) null)).isTrue();
    }
}
