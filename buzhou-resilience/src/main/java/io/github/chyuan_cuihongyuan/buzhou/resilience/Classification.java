package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.time.Duration;

/**
 * 分类结果：{@link ErrorCategory} + 可选的 {@code retryAfter}（如 429 的 Retry-After）。
 *
 * <p>把「类别」与「服务端建议的退避」一并返回，重试回路据此决定退避时长：
 * 有 {@code retryAfter} 时优先尊重（钳制到 {@code maxBackoff}），否则走指数退避。
 *
 * @param category   归一化类别（永不为 null）
 * @param retryAfter 服务端建议的退避（来自 Retry-After 等）；null 表示未提供
 */
public record Classification(ErrorCategory category, Duration retryAfter) {

    public Classification {
        if (category == null) {
            category = ErrorCategory.UNKNOWN;
        }
    }

    public static Classification of(ErrorCategory category) {
        return new Classification(category, null);
    }
}
