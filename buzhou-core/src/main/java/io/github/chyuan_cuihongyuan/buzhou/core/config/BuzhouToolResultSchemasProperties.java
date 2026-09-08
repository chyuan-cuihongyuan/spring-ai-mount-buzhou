package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 工具结果 schema yml 面（spec 409 / T710，MCP outputSchema 借鉴）：
 * {@code buzhou.tools.result-schemas.<toolName>} = 最小子集 JSON schema 串。
 * 声明即校验（复用 ToolArgsValidator）；未声明工具零变化。
 */
@ConfigurationProperties(prefix = "buzhou.tools.result-schemas")
public record BuzhouToolResultSchemasProperties(Map<String, String> schemas) {

    public BuzhouToolResultSchemasProperties {
        schemas = schemas == null ? Map.of() : Map.copyOf(schemas);
    }
}
