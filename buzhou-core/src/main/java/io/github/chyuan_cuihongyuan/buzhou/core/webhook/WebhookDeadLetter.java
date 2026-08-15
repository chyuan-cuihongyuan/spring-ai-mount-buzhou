package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.time.Instant;

/**
 * Webhook 死信查询形态（spec 24 / T103 / impl-78）：超 max-attempts 或 4xx 即死的
 * 待外发事件，经 {@link WebhookEventForwarder#deadLetters()} 查询（上限 100）。
 * 死信不自动重试；重放由运维按需自建（spec 24 out-of-scope）。
 *
 * @param eventId   幂等键（与投递请求头 X-Buzhou-Event-Id 同值）
 * @param type      事件类型
 * @param attempts  总尝试次数（含首试；损坏记录隔离为 -1）
 * @param createdAt 入队时间
 */
public record WebhookDeadLetter(String eventId, String type, int attempts, Instant createdAt) {
}
