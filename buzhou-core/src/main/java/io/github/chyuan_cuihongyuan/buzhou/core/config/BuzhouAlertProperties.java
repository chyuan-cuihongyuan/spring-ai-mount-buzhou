package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 健康告警装配属性（spec 312 / T616 + spec 330 / T652，前缀 {@code buzhou.alert}）。
 * 无规则 = 不装配引擎（零变化）。
 *
 * @param interval     评估周期（默认 30s）
 * @param rules        规则集（name + mechanism + for 持续窗防抖默认 0 立即）
 * @param silences     静默窗（机制匹配 + duration；until = 启动时刻 + duration）
 * @param inhibitRules 抑制规则（source firing 时 target 通知被抑制）
 */
@ConfigurationProperties(prefix = "buzhou.alert")
public record BuzhouAlertProperties(
        Duration interval,
        List<RuleSpec> rules,
        List<SilenceSpec> silences,
        List<InhibitSpec> inhibitRules) {

    public BuzhouAlertProperties {
        interval = interval == null ? Duration.ofSeconds(30) : interval;
        if (interval.isZero() || interval.isNegative()) {
            throw new BuzhouConfigurationException(
                    "buzhou.alert.interval（" + interval + "）非法", "正时长，如 30s");
        }
        rules = rules == null ? List.of() : List.copyOf(rules);
        silences = silences == null ? List.of() : List.copyOf(silences);
        inhibitRules = inhibitRules == null ? List.of() : List.copyOf(inhibitRules);
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

    /** 静默窗声明形态（spec 330）。mechanisms 含 {@code *} 即全量匹配。 */
    public record SilenceSpec(List<String> mechanisms, Duration duration,
            String comment, String createdBy) {

        public SilenceSpec {
            if (mechanisms == null || mechanisms.isEmpty()
                    || mechanisms.stream().anyMatch(m -> m == null || m.isBlank())) {
                throw new BuzhouConfigurationException(
                        "buzhou.alert.silences[].mechanisms 非法（silence=" + comment + "）",
                        "非空机制清单，如 [bulkhead] 或 [*]");
            }
            if (duration == null || duration.isZero() || duration.isNegative()) {
                throw new BuzhouConfigurationException(
                        "buzhou.alert.silences[].duration（" + duration + "）非法", "正时长，如 30m");
            }
            comment = comment == null ? "" : comment;
            createdBy = createdBy == null ? "" : createdBy;
        }
    }

    /** 抑制规则声明形态（spec 330）：source firing 时 target 通知被抑制。 */
    public record InhibitSpec(String sourceMechanism, String targetMechanism) {

        public InhibitSpec {
            if (sourceMechanism == null || sourceMechanism.isBlank()
                    || targetMechanism == null || targetMechanism.isBlank()) {
                throw new BuzhouConfigurationException(
                        "buzhou.alert.inhibit-rules[].source/target-mechanism 非法",
                        "两键必填——健康面机制名");
            }
            if (sourceMechanism.equals(targetMechanism)) {
                throw new BuzhouConfigurationException(
                        "buzhou.alert.inhibit-rules[]（" + sourceMechanism + "）非法",
                        "source/target 不得同机制——自抑 = 永久吞通知");
            }
        }
    }
}
