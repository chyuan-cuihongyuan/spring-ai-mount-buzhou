package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageStore SPI 契约校验套件测试（spec 743 / T1037–T1038 / impl 546）：
 * 真实实现全绿、走样实现逐项红、零残留。
 */
class MessageStoreContractTest {

    @Test
    void inMemoryImplementationPassesAllChecks() {
        MessageStoreContract.Report report =
                MessageStoreContract.verify(new InMemoryMessageStore());

        assertThat(report.passed()).as("失败项：" + report.failures()).isTrue();
        assertThat(report.checks()).hasSize(4);
        assertThat(report.checks()).isUnmodifiable();
    }

    @Test
    void brokenDeleteSessionIsDetected() {
        // 走样：deleteSession no-op（级联清场缺失——归档 saga 残留）
        MessageStore broken = new InMemoryMessageStore() {
            @Override
            public void deleteSession(String sessionId) {
                // 故意 no-op
            }
        };
        MessageStoreContract.Report report = MessageStoreContract.verify(broken);

        assertThat(report.passed()).isFalse();
        assertThat(report.failures()).contains("delete-session-idempotent");
    }

    @Test
    void probeSessionCleanedUpAfterRun() {
        java.util.Set<String> putSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Set<String> wipedSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
        InMemoryMessageStore store = new InMemoryMessageStore() {
            @Override
            public void append(String sessionId, java.util.List<BuzhouMessage> messages) {
                putSessions.add(sessionId);
                super.append(sessionId, messages);
            }

            @Override
            public void deleteSession(String sessionId) {
                wipedSessions.add(sessionId);
                super.deleteSession(sessionId);
            }
        };

        MessageStoreContract.Report report = MessageStoreContract.verify(store);

        assertThat(report.passed()).isTrue();
        assertThat(putSessions).isNotEmpty();
        assertThat(wipedSessions).isEqualTo(putSessions); // 每个探针会话都被清场
    }
}
