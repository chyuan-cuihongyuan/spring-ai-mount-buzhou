package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 探针归类装配属性（spec 332 / T656，前缀 {@code buzhou.health.probes}）。
 * 未点名的机制一律归 readiness（保守——外部依赖型故障最常见，重启治不了）。
 *
 * @param livenessMechanisms 点名为 liveness 的机制（重启能治：内部泄漏/看门狗类）
 * @param startupMechanisms  点名为 startup 的机制（等待能治：预热/迁移未完类）
 */
@ConfigurationProperties(prefix = "buzhou.health.probes")
public record BuzhouProbeProperties(
        List<String> livenessMechanisms,
        List<String> startupMechanisms) {

    public BuzhouProbeProperties {
        livenessMechanisms = livenessMechanisms == null ? List.of() : List.copyOf(livenessMechanisms);
        startupMechanisms = startupMechanisms == null ? List.of() : List.copyOf(startupMechanisms);
    }
}
