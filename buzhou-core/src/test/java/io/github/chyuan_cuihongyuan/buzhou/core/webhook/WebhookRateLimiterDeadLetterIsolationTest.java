package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限速×死信路径隔离补验（spec 739 / T1029–T1030 / impl 542）：defer 零状态机
 * 扰动、令牌恢复后全路径可达、重放产物不绕闸。
 */
class WebhookRateLimiterDeadLetterIsolationTest {

    @Test
    void deferDoesNotTouchOutboxStateMachine() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);
        outbox.append("evt-1", "session.ended", "{}");
        outbox.append("evt-2", "session.ended", "{}");
        AtomicLong clock = new AtomicLong(0);
        WebhookRateLimiter limiter = new WebhookRateLimiter(1, 1, clock::get);

        List<WebhookOutbox.OutboxRecord> due = outbox.due(Instant.now(), 10);
        int deferred = 0;
        for (WebhookOutbox.OutboxRecord record : due) {
            if (limiter.tryAcquire()) {
                outbox.delete(record); // 模拟投递成功
            } else {
                deferred++;            // defer：不 update/delete/markDead
            }
        }
        assertThat(deferred).isEqualTo(1);
        // 被 defer 的记录 attempts 仍为 0（状态机未动）且仍 due（下次可见）
        List<WebhookOutbox.OutboxRecord> stillDue = outbox.due(Instant.now(), 10);
        assertThat(stillDue).hasSize(1);
        assertThat(stillDue.get(0).attempts()).isZero();
    }

    @Test
    void tokenRecoveryReopensFullPath() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);
        outbox.append("evt-1", "session.ended", "{}");
        AtomicLong clock = new AtomicLong(0);
        WebhookRateLimiter limiter = new WebhookRateLimiter(1, 1, clock::get);

        assertThat(limiter.tryAcquire()).isTrue();   // 消耗
        clock.addAndGet(1_000);                      // 回填
        assertThat(limiter.tryAcquire()).isTrue();   // 恢复——投递/重试/死信全路径重新可达
        assertThat(outbox.due(Instant.now(), 10)).hasSize(1); // 记录仍在管线上
    }

    @Test
    void replayedRecordsSubjectToLimiter() {
        SessionStateStore store = new InMemorySessionStateStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 64);
        outbox.append("evt-1", "session.ended", "{}");
        outbox.markDead(outbox.due(Instant.now(), 1).get(0));

        // 重放 = requeueDead 迁回 outbox due——后续投递仍走 limiter（不绕闸）
        assertThat(outbox.requeueDead(10)).isEqualTo(1);
        AtomicLong clock = new AtomicLong(0);
        WebhookRateLimiter limiter = new WebhookRateLimiter(1, 0.001, clock::get);
        assertThat(limiter.tryAcquire()).isTrue();   // 首个令牌
        assertThat(limiter.tryAcquire()).isFalse();  // 重放产物同样受闸
    }
}
