package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * impl-36 / spec 13 §growth-8：内存 store 套件的有界化配置（{@code buzhou.store.in-memory.*}
 * 的编程式等价物；Spring 装配经 {@code BuzhouCoreProperties.Store.InMemory} 映射为本形状）。
 *
 * <p>语义分两族（Redis 76K★ 内存策略族）：
 * <ul>
 *   <li><b>事实台账（message / summary / state）= noeviction</b>——
 *       {@code maxSessions} 准入 + per-session 消息上限，超额抛
 *       {@link io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException}，
 *       绝不静默丢；</li>
 *   <li><b>可再生集合（observability）= volatile-lru 语义（采样近似）</b>——
 *       观测会话数超 {@code maxObservabilitySessions} 时采样逐出最不活跃会话，
 *       per-session 观测记录（spans 与 events 各自）超 {@code maxObservabilityRecordsPerSession}
 *       时丢最旧（丢弃计数可观测）。</li>
 * </ul>
 *
 * <p>归一化约定（沿用 {@code EventDispatchConfig} 惯例）：null / 非正 → 默认值；
 * 启动期不引入 JSR-303（全量启动校验是切片 42 的事）。
 *
 * @param maxSessions                      事实台账最大会话数；默认 1,000
 * @param maxMessagesPerSession            单会话消息上限；默认 5,000
 * @param maxObservabilitySessions         观测（可再生）最大会话数；默认 1,000
 * @param maxObservabilityRecordsPerSession 单会话观测记录上限（spans 与 events 各自丢最旧）；默认 10,000
 */
public record InMemoryStoreConfig(
        Integer maxSessions,
        Integer maxMessagesPerSession,
        Integer maxObservabilitySessions,
        Integer maxObservabilityRecordsPerSession) {

    public static final int DEFAULT_MAX_SESSIONS = 1_000;
    public static final int DEFAULT_MAX_MESSAGES_PER_SESSION = 5_000;
    public static final int DEFAULT_MAX_OBSERVABILITY_SESSIONS = 1_000;
    public static final int DEFAULT_MAX_OBSERVABILITY_RECORDS_PER_SESSION = 10_000;

    /** 全默认值（等价于无参构造的既有行为）。 */
    public static InMemoryStoreConfig defaults() {
        return new InMemoryStoreConfig(null, null, null, null);
    }

    public InMemoryStoreConfig {
        maxSessions = maxSessions == null || maxSessions <= 0
                ? DEFAULT_MAX_SESSIONS : maxSessions;
        maxMessagesPerSession = maxMessagesPerSession == null || maxMessagesPerSession <= 0
                ? DEFAULT_MAX_MESSAGES_PER_SESSION : maxMessagesPerSession;
        maxObservabilitySessions = maxObservabilitySessions == null || maxObservabilitySessions <= 0
                ? DEFAULT_MAX_OBSERVABILITY_SESSIONS : maxObservabilitySessions;
        maxObservabilityRecordsPerSession = maxObservabilityRecordsPerSession == null
                || maxObservabilityRecordsPerSession <= 0
                ? DEFAULT_MAX_OBSERVABILITY_RECORDS_PER_SESSION : maxObservabilityRecordsPerSession;
    }
}
