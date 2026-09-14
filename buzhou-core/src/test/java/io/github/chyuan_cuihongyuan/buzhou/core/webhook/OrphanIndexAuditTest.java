package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-695 / spec 949 续：outbox due 索引孤儿审计——正常路径零孤儿、
 * 人为孤儿检出、markDead 后孤儿清除归零。
 */
class OrphanIndexAuditTest {

    @Test
    void normalPathHasZeroOrphans() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);

        // append 正常路径（主记录 + due 索引成对写入）
        outbox.append("ev-1", "t", "p");
        assertThat(outbox.orphanIndexCount()).isZero();

        // appendRetry + markDead 一致记录：双删正确无孤儿
        WebhookOutbox.OutboxRecord retry = new WebhookOutbox.OutboxRecord("ev-2", "t",
                "p", 2, 1, Instant.now().plusSeconds(60).toEpochMilli(),
                Instant.now().toEpochMilli());
        outbox.appendRetry(retry);
        assertThat(outbox.orphanIndexCount()).isZero();
        outbox.markDead(retry);
        assertThat(outbox.orphanIndexCount()).isZero(); // markDead 双删（主记录+due 索引）
    }

    @Test
    void injectedOrphanDetected() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);

        // 人为注入孤儿：indexEntry 存在但主记录缺失（模拟删除时序缺陷）
        store.put(WebhookOutbox.SESSION_ID, new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                "due.0000000000999", "ev-ghost", "webhook-outbox", 0, null, Instant.now()));

        assertThat(outbox.orphanIndexCount()).isEqualTo(1);
    }
}
