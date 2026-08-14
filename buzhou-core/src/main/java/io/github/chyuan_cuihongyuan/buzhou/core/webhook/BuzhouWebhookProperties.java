package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 事件外发 webhook 装配属性（spec 20 / T89 / impl-64，前缀 {@code buzhou.webhook}）。
 *
 * <p>safe-by-default：<b>不配 url = 完全不装配</b>（零开销）。配了 url 才注册 forwarder。
 *
 * @param url            投递目标（POST application/json）
 * @param secret         HMAC-SHA256 签名密钥（null = 不签名）
 * @param timeout        单次请求超时（默认 5s）
 * @param maxAttempts    失败重试上限（含首试，默认 3；IOException/5xx 才重试，4xx 不重试）
 * @param queueCapacity  待投递有界队列容量（默认 256；满则丢弃 + 计数，不阻塞会话主链）
 */
@ConfigurationProperties(prefix = "buzhou.webhook")
public record BuzhouWebhookProperties(
        String url,
        String secret,
        Duration timeout,
        Integer maxAttempts,
        Integer queueCapacity) {

    public BuzhouWebhookProperties {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            timeout = Duration.ofSeconds(5);
        }
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 3;
        }
        if (queueCapacity == null || queueCapacity < 1) {
            queueCapacity = 256;
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
