package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 干跑拦截装配属性（spec 323 / T637，前缀 {@code buzhou.dry-run}，
 * Terraform plan 借鉴）。默认关（enabled 未配/false 不装配）。
 *
 * @param enabled 总开关
 * @param tools   include 清单（空/未配 = 全量拦——纯演练语义）
 */
@ConfigurationProperties(prefix = "buzhou.dry-run")
public record DryRunProperties(Boolean enabled, List<String> tools) {
}
