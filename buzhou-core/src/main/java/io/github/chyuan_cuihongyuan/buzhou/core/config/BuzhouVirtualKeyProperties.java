package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 虚拟 key 配额装配属性（spec 158 §A / T511，spec 148 fog「yml 装配面」，
 * 前缀 {@code buzhou.virtual-keys}）：limits 为 key → token 硬顶表；active-key
 * 为本实例扣减归属（省缺 = 不启用 key 闸——编程面默认关哲学）。
 *
 * <p><b>校验分工</b>：bind 期只验 limits 值 ≥ 1（矩阵逐键绑定时 active-key 单独
 * 出现不炸）；「active-key 必须在 limits 里」在 autoconfig 装配期 fail-fast
 * （不带病上线）。
 */
@ConfigurationProperties(prefix = "buzhou.virtual-keys")
public record BuzhouVirtualKeyProperties(
        Map<String, Long> limits,
        String activeKey) {

    public BuzhouVirtualKeyProperties {
        if (activeKey != null && activeKey.isBlank()) {
            activeKey = null;
        }
        if (limits != null) {
            limits.forEach((name, limit) -> {
                if (limit == null || limit < 1) {
                    throw configError("limits." + name,
                            "每个虚拟 key 的 token 硬顶设为正整数（当前：" + limit + "）");
                }
            });
        }
    }

    private static BuzhouConfigurationException configError(String key, String fix) {
        return new BuzhouConfigurationException(
                "buzhou.virtual-keys." + key + " 配置非法", fix);
    }
}
