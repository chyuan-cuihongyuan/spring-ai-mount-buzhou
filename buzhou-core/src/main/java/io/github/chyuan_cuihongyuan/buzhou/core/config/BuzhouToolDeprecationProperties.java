package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 工具退役 yml 面（spec 406 / T704，K8s API deprecation 借鉴）：
 * {@code buzhou.tools.deprecated.<toolName>.{since, removal-in, successor,
 * message}}。声明即装配（wrapToolCallbacks 名匹配包装）；空表/未配置零变化。
 */
@ConfigurationProperties(prefix = "buzhou.tools.deprecated")
public record BuzhouToolDeprecationProperties(Map<String, Spec> tools) {

    public BuzhouToolDeprecationProperties {
        tools = tools == null ? Map.of() : Map.copyOf(tools);
    }

    /** 退役声明形态（单规范构造器——relaxed binding 自带 removal-in 兼容）。 */
    public record Spec(String since, String removalIn, String successor, String message) {
    }
}
