package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ResourcePolicySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 guard 健康：UP（审计可读）/ DOWN（审计存储抛异常）/
 * UNKNOWN（审计关闭）；详情含策略 provenance。降级运行（无签名密钥）不是 DOWN。
 */
class GuardHealthTest {

    @TempDir
    Path tempDir;

    private AuditRecordStore throwingStore() {
        return new AuditRecordStore() {
            @Override
            public void append(io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord record) {
                throw new IllegalStateException("db down");
            }

            @Override
            public List<io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord> loadAll() {
                throw new IllegalStateException("db down");
            }

            @Override
            public long count() {
                throw new IllegalStateException("db down");
            }
        };
    }

    private AuditRecordStore healthyStore() {
        return new io.github.chyuan_cuihongyuan.buzhou.guard.audit.InMemoryAuditRecordStore();
    }

    @Test
    void upWhenAuditStoreReadable() throws Exception {
        Path file = Files.createTempFile(tempDir, "policy", ".json");
        Files.writeString(file, "{\"rules\":[{\"id\":\"r\",\"action\":\"ALLOW\"}]}");
        try (PolicyRefresher refresher = new PolicyRefresher(
                new ResourcePolicySource("file:" + file), Duration.ZERO)) {
            GuardHealth health = new GuardHealth(true, healthyStore(), refresher);
            assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
            assertThat(health.details()).containsEntry("policyRules", 1)
                    .containsKey("policyRevision");
        }
    }

    @Test
    void downWhenAuditStoreBroken() {
        GuardHealth health = new GuardHealth(true, throwingStore(), null);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.DOWN);
    }

    @Test
    void unknownWhenAuditDisabled() {
        assertThat(new GuardHealth(false, null, null).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(new GuardHealth(true, null, null).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
    }
}
