package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.Map;

/**
 * Webhook outbox 健康面（spec 39 §C / T140 / impl-113）：forwarder 装配时经本类暴露
 * outbox 水位——pending（未决积压）/ deadLetters（死信）/ capacity。恒 UP（投递是
 * 旁路机制，死信堆积不构成核心职能失效——告警面归 runbook §7 指标）。
 *
 * @since 1.0.0
 */
public final class WebhookOutboxHealth implements BuzhouHealth {

    private final WebhookEventForwarder forwarder;

    public WebhookOutboxHealth(WebhookEventForwarder forwarder) {
        this.forwarder = forwarder;
    }

    @Override
    public String mechanism() {
        return "webhook-outbox";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of(
                "pending", forwarder.pendingCount(),
                "deadLetters", forwarder.deadLetters().size(),
                "delivered", forwarder.delivered(),
                "dropped", forwarder.dropped());
    }
}
