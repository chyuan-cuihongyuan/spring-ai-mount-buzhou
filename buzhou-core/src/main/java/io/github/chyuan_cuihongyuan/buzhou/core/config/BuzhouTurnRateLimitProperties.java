package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 轮次限速 yml 面（spec 425 / T742，nginx token bucket 借鉴）：
 * {@code buzhou.ratelimit.turns.{burst, permits-per-minute}} 双声明即装配
 * TurnRateLimitHook（默认键 sessionId——单会话频次帽；租户整体帽由宿主
 * 手工构造常量键 hook）；缺任一不装配零行为。
 */
@ConfigurationProperties(prefix = "buzhou.ratelimit.turns")
public record BuzhouTurnRateLimitProperties(Integer burst, Double permitsPerMinute) {
}
