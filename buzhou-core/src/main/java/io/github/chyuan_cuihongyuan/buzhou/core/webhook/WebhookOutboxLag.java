package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * outbox 积压滞后面（spec 135 / T483，Kafka consumer-lag 借鉴）：pending 计数 +
 * 最老积压 age（<b>含退避中</b>——due() 看不到的部分恰是停摆最严重处；入队即
 * 计时）+ 死信数一读即知投递是否停摆；stalled 阈值判定可直连告警。
 *
 * <p>诚实边界：最老积压扫描为全量读值（容量软上限内可接受；pending 计数走
 * countByPrefix 下推不受影响）。gauge 绑定显式（bindGauges）——autoconfig 接线
 * 后续按需。
 */
public final class WebhookOutboxLag {

    /** 一次滞后读数（无积压 age=-1 / oldestEventId=null）。 */
    public record Lag(int pendingCount, long oldestPendingAgeMillis,
                      String oldestEventId, int deadCount) {
    }

    private final WebhookOutbox outbox;
    private final Clock clock;

    public WebhookOutboxLag(WebhookOutbox outbox) {
        this(outbox, Clock.systemUTC());
    }

    /** 时钟注入（age 断言确定性）。 */
    public WebhookOutboxLag(WebhookOutbox outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    /** 当前滞后读数（scanLimit = 最老积压扫描上限）。 */
    public Lag read(int scanLimit) {
        Optional<WebhookOutbox.OutboxRecord> oldest = outbox.pendingOldest(scanLimit);
        long age = oldest.map(r -> clock.millis() - r.createdAtEpochMs()).orElse(-1L);
        return new Lag(outbox.pendingCount(), age,
                oldest.map(WebhookOutbox.OutboxRecord::eventId).orElse(null),
                outbox.deadCount());
    }

    /** 停摆判定：最老积压 age ≥ 阈值（无积压 = false——空 outbox 不是停摆）。 */
    public boolean stalled(Duration threshold, int scanLimit) {
        Lag lag = read(scanLimit);
        return lag.oldestPendingAgeMillis() >= threshold.toMillis();
    }

    /**
     * gauge 绑定（活读 Supplier）：buzhou.webhook.outbox.pending /
     * buzhou.webhook.outbox.oldest-age-ms。显式调用（重复调用以 metrics 实现
     * 去重语义为准）。
     */
    public void bindGauges(int scanLimit) {
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .gauge("buzhou.webhook.outbox.pending",
                        () -> outbox.pendingCount());
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                .gauge("buzhou.webhook.outbox.oldest-age-ms",
                        () -> read(scanLimit).oldestPendingAgeMillis());
    }
}
