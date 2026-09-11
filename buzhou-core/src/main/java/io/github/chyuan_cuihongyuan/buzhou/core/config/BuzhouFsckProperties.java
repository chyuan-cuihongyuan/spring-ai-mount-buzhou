package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * store fsck 定时巡检 yml 面（spec 538 / T827）：{@code buzhou.fsck.
 * {enabled, interval}}。enabled 默认关（opt-in）；interval 默认 6h。
 */
@ConfigurationProperties(prefix = "buzhou.fsck")
public record BuzhouFsckProperties(Boolean enabled, Duration interval) {

    public BuzhouFsckProperties {
        enabled = enabled != null && enabled;
        interval = interval == null ? Duration.ofHours(6) : interval;
    }
}
