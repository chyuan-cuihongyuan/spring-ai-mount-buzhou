package io.github.chyuan_cuihongyuan.buzhou.resilience;

/**
 * 跨 provider 的归一化模型调用错误分类（CONTEXT.md「归一化错误分类」，五类）。
 *
 * <p>与具体 provider 的异常形态解耦：{@link ProviderErrorClassifier} 把「异常 + 响应元数据」映射到本枚举，
 * 上层（重试决策、observability 上报、onModelError 兜底）只用统一口径。
 *
 * <ul>
 *   <li>{@link #RATE_LIMIT} — 限流（429 / Retry-After），M1 默认重试。</li>
 *   <li>{@link #NETWORK} — 网络瞬断（连接重置 / 读超时 / 瞬时不可达），M1 默认重试。</li>
 *   <li>{@link #AUTH} — 鉴权失败（401 / 403 / 无效 key），不可恢复、不重试、快速失败。</li>
 *   <li>{@link #CONTENT} — 内容拒绝（provider 内容过滤的静默拒绝，仅元数据标记、不抛异常），不重试。</li>
 *   <li>{@link #UNKNOWN} — 不可识别的异常，保守默认不重试（可配置为重试）。</li>
 * </ul>
 */
public enum ErrorCategory {
    RATE_LIMIT,
    NETWORK,
    AUTH,
    CONTENT,
    UNKNOWN
}
