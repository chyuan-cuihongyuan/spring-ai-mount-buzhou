package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 135 / T484：outbox 积压滞后回归——空读数 / 积压 age 与 oldest /
 * 投递回落 / 死信升 pending 降 / stalled 阈值翻转 / 退避后移仍计入 age。
 */
class WebhookOutboxLagTest {

    private static final class MutableClock extends Clock {
        // 从真实 now 起步：outbox.append 的 createdAt 走真实时钟——对齐后 age 断言可用小容差
        private Instant now = Instant.now();

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void emptyOutboxHasNoLagAndIsNotStalled() {
        MutableClock clock = new MutableClock();
        WebhookOutboxLag lag = new WebhookOutboxLag(
                new WebhookOutbox(new InMemorySessionStateStore(), 8), clock);

        WebhookOutboxLag.Lag read = lag.read(16);
        assertThat(read.pendingCount()).isZero();
        assertThat(read.oldestPendingAgeMillis()).isEqualTo(-1);
        assertThat(read.oldestEventId()).isNull();
        assertThat(read.deadCount()).isZero();
        assertThat(lag.stalled(Duration.ofMinutes(1), 16)).isFalse();
    }

    @Test
    void backlogAgeGrowsWithClockAndOldestIdentified() {
        MutableClock clock = new MutableClock();
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.append("first", "t", "{}");
        clock.advanceMillis(1_000);
        outbox.append("second", "t", "{}");
        clock.advanceMillis(4_000);
        WebhookOutboxLag lag = new WebhookOutboxLag(outbox, clock);

        WebhookOutboxLag.Lag read = lag.read(16);
        assertThat(read.pendingCount()).isEqualTo(2);
        assertThat(read.oldestEventId()).isEqualTo("first");
        assertThat(read.oldestPendingAgeMillis()).isBetween(4_999L, 6_000L);
    }

    @Test
    void deliveryDropsPendingAndDeadLetterRaisesDeadCount() {
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.append("deliver-me", "t", "{}");
        outbox.append("doomed", "t", "{}");
        WebhookOutboxLag lag = new WebhookOutboxLag(outbox);

        var due = outbox.due(Instant.now(), 10);
        assertThat(due).hasSize(2);
        outbox.delete(due.stream()
                .filter(r -> r.eventId().equals("deliver-me")).findFirst().orElseThrow());
        outbox.markDead(due.stream()
                .filter(r -> r.eventId().equals("doomed")).findFirst().orElseThrow());

        WebhookOutboxLag.Lag read = lag.read(16);
        assertThat(read.pendingCount()).isZero();
        assertThat(read.deadCount()).isEqualTo(1);
    }

    @Test
    void stalledFlipsWhenOldestAgeCrossesThreshold() {
        MutableClock clock = new MutableClock();
        // spec 524 勘察修复：outbox.append 的 createdAt 取真实时钟——MutableClock 先对齐
        // 真实时间再 append，测试余量不随负载漂移（100ms 额度原被 setup 延迟吃掉）
        clock.advanceMillis(System.currentTimeMillis() - clock.instant().toEpochMilli() + 1_000);
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.append("stuck", "t", "{}");
        WebhookOutboxLag lag = new WebhookOutboxLag(outbox, clock);

        assertThat(lag.stalled(Duration.ofMinutes(5), 16)).isFalse();
        clock.advanceMillis(Duration.ofMinutes(5).toMillis() + 100);
        assertThat(lag.stalled(Duration.ofMinutes(5), 16)).isTrue();
    }

    @Test
    void backedOffRecordStillCountsTowardAge() {
        MutableClock clock = new MutableClock();
        // 同上：时钟先对齐真实时间——due() 判定与 append createdAt 不再受 setup 延迟影响
        clock.advanceMillis(System.currentTimeMillis() - clock.instant().toEpochMilli() + 1_000);
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.append("e1", "t", "{}");
        WebhookOutbox.OutboxRecord current = outbox.due(clock.instant(), 10).getFirst();
        // 退避后移 60s——due() 不可见，但 lag 的 age 仍计它
        outbox.update(current, new WebhookOutbox.OutboxRecord("e1", "t", "{}",
                current.seq(), 1, clock.instant().toEpochMilli() + 60_000,
                current.createdAtEpochMs()));
        clock.advanceMillis(30_000);
        WebhookOutboxLag lag = new WebhookOutboxLag(outbox, clock);

        assertThat(outbox.due(clock.instant(), 10)).isEmpty();
        WebhookOutboxLag.Lag read = lag.read(16);
        assertThat(read.pendingCount()).isEqualTo(1);
        assertThat(read.oldestEventId()).isEqualTo("e1");
        assertThat(read.oldestPendingAgeMillis()).isBetween(29_999L, 31_000L);
    }

    @Test
    void scanLimitBoundsOldestProbe() {
        WebhookOutbox outbox = new WebhookOutbox(new InMemorySessionStateStore(), 8);
        outbox.append("a", "t", "{}");
        outbox.append("b", "t", "{}");
        WebhookOutboxLag lag = new WebhookOutboxLag(outbox);

        // scanLimit=0：最老探测截断（count 走下推不受限）；age 读不到 = -1
        WebhookOutboxLag.Lag read = lag.read(0);
        assertThat(read.pendingCount()).isEqualTo(2);
        assertThat(read.oldestPendingAgeMillis()).isEqualTo(-1);
    }
}
