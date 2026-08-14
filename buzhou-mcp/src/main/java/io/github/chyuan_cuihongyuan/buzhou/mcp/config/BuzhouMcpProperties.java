package io.github.chyuan_cuihongyuan.buzhou.mcp.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * mcp 模块外层装配属性（spec 21 / T91 / impl-66，前缀 {@code buzhou.mcp}）。
 * 模块内部细键仍走 {@code fromYml} map 契约（by-design，见 spec 21）。
 *
 * @param enabled              模块开关（默认开）
 * @param dangerousToolPatterns 客户端侧危险工具名模式（glob，命中即登记进 guard HITL 清单）
 * @param shutdownBudget       停机关闭预算（默认 35s；显式 0 = 不等待）
 */
@ConfigurationProperties(prefix = "buzhou.mcp")
public record BuzhouMcpProperties(
        Boolean enabled,
        List<String> dangerousToolPatterns,
        Duration shutdownBudget) {

    public BuzhouMcpProperties {
        enabled = enabled == null || enabled;
        dangerousToolPatterns = dangerousToolPatterns == null ? List.of() : List.copyOf(dangerousToolPatterns);
        shutdownBudget = shutdownBudget == null ? Duration.ofSeconds(35) : shutdownBudget;
        if (shutdownBudget.isNegative()) {
            throw new BuzhouConfigurationException(
                    "buzhou.mcp.shutdown-budget（" + shutdownBudget + "）非法",
                    "设为非负时长（显式 0 = 停机不等待连接关闭），如 35s");
        }
    }
}
