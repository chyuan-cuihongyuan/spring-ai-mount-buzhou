package io.github.chyuan_cuihongyuan.buzhou.resilience;

/**
 * 指标 tag 基数纪律工具（spec 44 §B / T160 / impl-131）：模型名等外部输入进 Micrometer tag
 * 前统一截断（默认 32 字符）——既有 RateLimitAdvisor 纪律抽公用，ModelCircuitBreaker gauge 与
 * fallback-switches from/to 补齐（修前「只落一半」）。
 *
 * @since 1.0.0
 */
public final class MetricTags {

    /** tag 值截断上限（与 RateLimitAdvisor 既有纪律一致）。 */
    public static final int MAX_TAG_LENGTH = 32;

    private MetricTags() {
    }

    /** tag 值截断（null → "unknown"；超长截 32）。 */
    public static String bound(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() > MAX_TAG_LENGTH ? value.substring(0, MAX_TAG_LENGTH) : value;
    }
}
