package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 健康告警装配属性（spec 312 / T616，前缀 {@code buzhou.alert}）。无规则 =
 * 不装配引擎（零变化）。
 *
 * @param interval 评估周期（默认 30s）
 * @param rules    规则集（name + mechanism + for 持续窗防抖默认 0 立即）
 */
@ConfigurationProperties(prefix = "buzhou.alert")
public record BuzhouAlertProperties(
        Duration interval,
        List<RuleSpec> rules) {

    public BuzhouAlertProperties {
        interval = interval == null ? Duration.ofSeconds(30) : interval;
        if (interval.isZero() || interval.isNegative()) {
            throw new BuzhouConfigurationException(
                    "buzhou.alert.interval（" + interval + "）非法", "正时长，如 30s");
        }
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /** 规则声明形态（for 为 Java 关键字——yml 键 {@code for} 经 @Name 绑到 forDuration）。 */
    public record RuleSpec(String name, String mechanism,
            @org.springframework.boot.context.properties.bind.Name("for") Duration forDuration) {

        public RuleSpec {
            if (name == null || name.isBlank() || mechanism == null || mechanism.isBlank()) {
                throw new BuzhouConfigurationException(
                        "buzhou.alert.rules[].name/mechanism（rule=" + name + "）非法",
                        "两键必填——name 规则名、mechanism 健康面机制名");
            }
            forDuration = forDuration == null ? Duration.ZERO : forDuration;
        }
    }
}
