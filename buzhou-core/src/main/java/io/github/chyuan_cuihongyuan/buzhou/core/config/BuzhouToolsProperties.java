package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 工具面装配属性（spec 31 / T110 / impl-85，前缀 {@code buzhou.tools}）。
 *
 * @param resultLimitChars      工具结果入模型上下文的字符上限（默认 20_000；-1 = 不限）
 * @param resultLimitOverrides  per-tool 覆盖（glob 通配键，值 = 字符数或 -1 禁用；
 *                              追加式覆盖默认豁免 read_range——同键改值，新键叠加）
 */
@ConfigurationProperties(prefix = "buzhou.tools")
public record BuzhouToolsProperties(
        Integer resultLimitChars,
        Map<String, Integer> resultLimitOverrides) {

    public BuzhouToolsProperties {
        if (resultLimitChars == null) {
            resultLimitChars = io.github.chyuan_cuihongyuan.buzhou.core.exec
                    .ToolResultLimiter.DEFAULT_LIMIT_CHARS;
        }
        if (resultLimitChars < -1) {
            throw new BuzhouConfigurationException(
                    "buzhou.tools.result-limit-chars（" + resultLimitChars + "）非法",
                    "设为 >= 0 的整数或 -1（不限）");
        }
        if (resultLimitOverrides != null) {
            resultLimitOverrides.values().stream()
                    .filter(v -> v != null && v < -1)
                    .findAny()
                    .ifPresent(v -> {
                        throw new BuzhouConfigurationException(
                                "buzhou.tools.result-limit-overrides 值（" + v + "）非法",
                                "每项设为 >= 0 的整数或 -1（该工具不限）");
                    });
        }
    }
}
