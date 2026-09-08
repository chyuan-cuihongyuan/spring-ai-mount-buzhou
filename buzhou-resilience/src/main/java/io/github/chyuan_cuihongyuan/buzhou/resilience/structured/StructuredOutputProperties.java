package io.github.chyuan_cuihongyuan.buzhou.resilience.structured;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 结构化输出 yml 面（spec 402 / T696，instructor 借鉴）：
 * {@code buzhou.resilience.structured-output.{enabled, max-repair-attempts,
 * schema.{required, properties}}}。enabled=true 而 schema 全空 = 配置错误
 * （装配期 fail-fast）。
 */
@ConfigurationProperties(prefix = "buzhou.resilience.structured-output")
public record StructuredOutputProperties(Boolean enabled, Integer maxRepairAttempts,
        SchemaSpec schema) {

    /** 修复尝试上限（默认 1；0 = 纯执法不修复）。 */
    public int effectiveMaxRepairAttempts() {
        return maxRepairAttempts == null ? 1 : Math.max(0, maxRepairAttempts);
    }

    /** 契约声明形态（properties = 键→类型）。 */
    public record SchemaSpec(List<String> required, Map<String, String> properties) {

        public SchemaSpec {
            required = required == null ? List.of() : List.copyOf(required);
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }

        /** 是否声明了任何约束（装配面 fail-fast 判定用）。 */
        public boolean isEmpty() {
            return required.isEmpty() && properties.isEmpty();
        }

        /** 声明类型归一小写。 */
        public Map<String, String> normalizedTypes() {
            java.util.Map<String, String> normalized = new java.util.LinkedHashMap<>();
            properties.forEach((k, v) -> normalized.put(k,
                    v == null ? "" : v.toLowerCase(Locale.ROOT)));
            return normalized;
        }
    }
}
