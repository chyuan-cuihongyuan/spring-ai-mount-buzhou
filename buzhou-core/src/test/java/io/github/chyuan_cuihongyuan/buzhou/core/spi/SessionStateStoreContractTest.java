package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * store SPI 契约校验套件测试（spec 705 / T961–T962 / impl 508）：真实实现
 * 全绿、走样实现逐项红、零残留、Report 不可变。
 */
class SessionStateStoreContractTest {

    @Test
    void inMemoryImplementationPassesAllChecks() {
        SessionStateStoreContract.Report report =
                SessionStateStoreContract.verify(new InMemorySessionStateStore());

        assertThat(report.passed()).as("失败项：" + report.failures()).isTrue();
        assertThat(report.checks()).hasSize(9);
        assertThat(report.failures()).isEmpty();
        assertThat(report.checks()).isUnmodifiable();
    }

    @Test
    void brokenCasDeleteIsDetected() {
        // 走样：deleteIfValueMatches 无条件删（消费一次语义被破坏——HITL 放行会双消费）
        SessionStateStore broken = new InMemorySessionStateStore() {
            @Override
            public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
                delete(sessionId, key);
                return true;
            }
        };
        SessionStateStoreContract.Report report = SessionStateStoreContract.verify(broken);

        assertThat(report.passed()).isFalse();
        assertThat(report.failures()).contains("delete-if-value-matches-consumes-once");
    }

    @Test
    void brokenPrefixScanIsDetected() {
        // 走样：scanByPrefix 漏过滤（全量返回——outbox 读放大回归）
        SessionStateStore broken = new InMemorySessionStateStore() {
            @Override
            public Map<String, StateEntry> scanByPrefix(String sessionId, String prefix) {
                return getAll(sessionId);
            }
        };
        SessionStateStoreContract.Report report = SessionStateStoreContract.verify(broken);

        assertThat(report.failures()).contains("scan-by-prefix-filters");
    }

    @Test
    void brokenDeleteSessionIsDetected() {
        // 走样：deleteSession no-op（级联清场缺失——归档 saga 数据残留）
        SessionStateStore broken = new InMemorySessionStateStore() {
            @Override
            public void deleteSession(String sessionId) {
                // 故意 no-op
            }
        };
        SessionStateStoreContract.Report report = SessionStateStoreContract.verify(broken);

        assertThat(report.failures()).contains("delete-session-idempotent");
    }

    @Test
    void probeSessionCleanedUpAfterRun() {
        // 记录 put 过的探针会话与被 deleteSession 的会话——verify 结束应全覆盖（零残留）
        java.util.Set<String> putSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Set<String> wipedSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
        InMemorySessionStateStore store = new InMemorySessionStateStore() {
            @Override
            public void put(String sessionId, StateEntry entry) {
                putSessions.add(sessionId);
                super.put(sessionId, entry);
            }

            @Override
            public void deleteSession(String sessionId) {
                wipedSessions.add(sessionId);
                super.deleteSession(sessionId);
            }
        };

        SessionStateStoreContract.Report report = SessionStateStoreContract.verify(store);

        assertThat(report.passed()).isTrue();
        assertThat(putSessions).isNotEmpty(); // 契约确实写入了探针数据
        assertThat(wipedSessions).isEqualTo(putSessions); // 每个探针会话都被清场
    }
}
