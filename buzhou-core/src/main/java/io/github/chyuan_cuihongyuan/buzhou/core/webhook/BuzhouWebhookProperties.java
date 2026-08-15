package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 事件外发 webhook 装配属性（spec 20 / T89；outbox 持久化升级 spec 24 / T103 / impl-78，
 * 前缀 {@code buzhou.webhook}）。
 *
 * <p>safe-by-default：<b>不配 url = 完全不装配</b>（零开销）。配了 url 才注册 forwarder。
 *
 * @param url             投递目标（POST application/json）
 * @param secret          HMAC-SHA256 签名密钥（null = 不签名）
 * @param timeout         单次请求超时（默认 5s）
 * @param maxAttempts     单条记录总尝试上限（含首试，默认 8；IOException/5xx 才重试，4xx 即死）
 * @param outboxCapacity  持久化 outbox 未决记录容量（默认 10_000；满则拒入 + 计数，不阻塞主链）
 * @param queueCapacity   <b>已废弃 no-op</b>（spec 24 起投递前暂存持久化，内存队列移除）；
 *                        显式配置时启动 WARN 提示迁移 outbox-capacity
 */
@ConfigurationProperties(prefix = "buzhou.webhook")
public record BuzhouWebhookProperties(
        String url,
        String secret,
        Duration timeout,
        Integer maxAttempts,
        Integer outboxCapacity,
        Integer queueCapacity) {

    private static final System.Logger LOGGER = System.getLogger(BuzhouWebhookProperties.class.getName());

    public BuzhouWebhookProperties {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            timeout = Duration.ofSeconds(5);
        }
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 8;
        }
        if (outboxCapacity == null || outboxCapacity < 1) {
            outboxCapacity = 10_000;
        }
        if (queueCapacity != null) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "buzhou.webhook.queue-capacity 已废弃（no-op）：投递暂存已持久化 outbox 化，"
                            + "请改用 buzhou.webhook.outbox-capacity");
        }
        if (url != null && !url.isBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException(
                    "buzhou.webhook.url（" + url + "）非法", "以 http:// 或 https:// 开头");
        }
    }

    /** 是否装配（配置了非空 url）。 */
    public boolean enabled() {
        return url != null && !url.isBlank();
    }
}
