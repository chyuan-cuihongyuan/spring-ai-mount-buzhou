package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 会话归档治理配置（spec 127 §A / T455，spec 103 fog「归档 autoconfig 定时」）：
 * {@code buzhou.session-archive.*}。purge 默认<b>关</b>——归档冷层的删除动作必须
 * 显式开启（默认零行为变化）；开启后按 interval 定频 {@code purgeExpired(ttl)}。
 */
@ConfigurationProperties(prefix = "buzhou.session-archive")
public class BuzhouArchiveProperties {

    /** 定时清理开关（默认 false：宿主手动 purgeExpired 或不清理）。 */
    private boolean purgeEnabled = false;

    /** 归档保留 TTL（到期删除；ttl ≤ 0 语义沿用 purgeExpired——显式全清）。 */
    private Duration purgeTtl = Duration.ofDays(7);

    /** 清理周期（scheduleWithFixedDelay：上一轮完成后间隔计时）。 */
    private Duration purgeInterval = Duration.ofHours(1);

    public boolean isPurgeEnabled() {
        return purgeEnabled;
    }

    public void setPurgeEnabled(boolean purgeEnabled) {
        this.purgeEnabled = purgeEnabled;
    }

    public Duration getPurgeTtl() {
        return purgeTtl;
    }

    public void setPurgeTtl(Duration purgeTtl) {
        this.purgeTtl = purgeTtl;
    }

    public Duration getPurgeInterval() {
        return purgeInterval;
    }

    public void setPurgeInterval(Duration purgeInterval) {
        this.purgeInterval = purgeInterval;
    }
}
