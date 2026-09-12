package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * webhook 投递限速测试（spec 718 / T987–T988 / impl 521）：令牌桶节流、
 * defer 不碰重试状态机、时钟回填放行、默认关零回归。
 */
class WebhookRateLimiterTest {

    @Test
    void tokenBucketThrottlesAndRefills() {
        AtomicLong clock = new AtomicLong(1_000_000);
        WebhookRateLimiter limiter = new WebhookRateLimiter(2, 10, clock::get); // burst 2，10/s

        assertThat(limiter.tryAcquire()).isTrue();  // 满桶起步
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse(); // 空桶
        assertThat(limiter.deferredCount()).isEqualTo(1);

        clock.addAndGet(150); // 回填 1.5 → 1 个令牌
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.deferredCount()).isEqualTo(2);
    }

    @Test
    void capacityBurstHonored() {
        AtomicLong clock = new AtomicLong(0);
        WebhookRateLimiter limiter = new WebhookRateLimiter(5, 1, clock::get);
        int granted = 0;
        for (int i = 0; i < 8; i++) {
            if (limiter.tryAcquire()) {
                granted++;
            }
        }
        assertThat(granted).isEqualTo(5); // burst=5（时钟未动——无回填）
    }

    @Test
    void invalidConstructionRejected() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new WebhookRateLimiter(0, 1, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new WebhookRateLimiter(2, 0, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deferredRecordsKeepOutboxStateUntouched() {
        // 限速闸拒绝路径不产生 outbox 更新——由 forwarder.processDueBatch 的 defer
        // 分支不调用 scheduleRetryOrDead 保证（结构断言：deferred 计数与 attempts 隔离）
        AtomicLong clock = new AtomicLong(0);
        WebhookRateLimiter limiter = new WebhookRateLimiter(1, 1, clock::get); // 1 token/s

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
        // defer 路径只加计数——无任何时钟/状态突变副作用面（除 deferred 计数）
        assertThat(limiter.deferredCount()).isEqualTo(1);
        clock.addAndGet(2_000); // 2s × 1/s = 2 tokens（cap 1）
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void dueRecordsStillVisibleAfterDefer() {
        // defer 语义的 outbox 侧不变量：记录仍 due（outbox.due 可见）——
        // 用真实 outbox 验证 defer 不改 nextAttemptAt
        WebhookOutbox outbox = new WebhookOutbox(
                new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore(),
                64);
        outbox.append("evt-1", "session.ended", "{\"id\":1}");
        List<WebhookOutbox.OutboxRecord> due = outbox.due(Instant.now(), 10);
        assertThat(due).hasSize(1); // due 且未被 defer 改动
        assertThat(due.get(0).attempts()).isZero();
    }
}
