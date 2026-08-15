package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 内核装配属性（spec 09 配置项表，前缀 {@code buzhou}）。
 *
 * <p>承载 store 实现选择（{@code buzhou.store.type}）与模型名（{@code buzhou.model-name}，
 * 供 memory / observability 等模块共享，避免重复绑定）。
 *
 * <p>impl-33 / spec 13 §core-3：租约参数入配置面——{@code buzhou.lease-ttl}
 * （默认 90s）与 {@code buzhou.lease-renew-interval}（默认 TTL/3）。compact constructor
 * 给默认值（不引 jakarta.validation——启动期 JSR-303 校验是切片 42 的事）。
 *
 * @param modelName          模型名，默认 {@code unknown}（对齐 {@code MemoryModule} 默认）
 * @param store              store 选择；默认 {@code memory}
 * @param leaseTtl           impl-33：会话租约 TTL；null/非正 → 默认 90s
 * @param leaseRenewInterval impl-33：后台续租间隔；null（默认）= TTL/3，显式配置可为任意节奏
 *                           （如测试用短间隔驱动真实时序）
 * @param lifecycle          impl-30 / spec 13 §core-1：停机排空预算
 *                           （{@code buzhou.lifecycle.timeout-per-shutdown-phase}，默认 30s）
 * @param core               impl-34 / spec 13 §core-4：core 运行时旋钮
 *                           （{@code buzhou.core.*}，事件分发模式）
 */
@org.springframework.validation.annotation.Validated
@ConfigurationProperties(prefix = "buzhou")
public record BuzhouCoreProperties(String modelName, Store store,
                                   Duration leaseTtl, Duration leaseRenewInterval,
                                   Lifecycle lifecycle, Core core) {

    /** impl-33：租约 TTL 默认值（既有硬编码语义收敛）。 */
    public static final Duration DEFAULT_LEASE_TTL = Duration.ofSeconds(90);

    public BuzhouCoreProperties {
        modelName = (modelName == null || modelName.isBlank()) ? "unknown" : modelName;
        store = store == null ? new Store(null, null) : store;
        leaseTtl = (leaseTtl == null || leaseTtl.isZero() || leaseTtl.isNegative())
                ? DEFAULT_LEASE_TTL : leaseTtl;
        leaseRenewInterval = (leaseRenewInterval == null
                || leaseRenewInterval.isZero() || leaseRenewInterval.isNegative())
                ? null : leaseRenewInterval;
        lifecycle = lifecycle == null ? new Lifecycle(null) : lifecycle;
        core = core == null ? new Core(null) : core;
    }

    /** 生效的后台续租间隔：未显式配置时取 TTL/3（HikariCP maxLifetime 防同步抖动节奏）。 */
    public Duration effectiveLeaseRenewInterval() {
        return leaseRenewInterval != null ? leaseRenewInterval : leaseTtl.dividedBy(3);
    }

    public record Store(String type, InMemory inMemory) {
        public Store {
            type = (type == null || type.isBlank()) ? "memory" : type;
            inMemory = inMemory == null ? new InMemory(null, null, null, null) : inMemory;
        }

        /**
         * impl-36 / spec 13 §growth-8：内存套件容量配额（{@code buzhou.store.in-memory.*}）。
         *
         * @param maxSessions                      事实台账最大会话数；默认 1,000
         * @param maxMessagesPerSession            单会话消息上限；默认 5,000
         * @param maxObservabilitySessions         观测（可再生）最大会话数；默认 1,000
         * @param maxObservabilityRecordsPerSession 单会话观测记录上限（spans/events 各自丢最旧）；默认 10,000
         */
        public record InMemory(
                Integer maxSessions,
                Integer maxMessagesPerSession,
                Integer maxObservabilitySessions,
                Integer maxObservabilityRecordsPerSession) {

            /** 装配用：映射为内存套件配置对象（归一化在目标形状内完成）。 */
            public io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig toConfig() {
                return new io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig(
                        maxSessions, maxMessagesPerSession,
                        maxObservabilitySessions, maxObservabilityRecordsPerSession);
            }
        }
    }

    /**
     * impl-34 / spec 13 §core-4：core 运行时旋钮（{@code buzhou.core.*}）。
     *
     * @param eventDispatch 事件分发（{@code buzhou.core.event-dispatch.*}；默认 SYNC 内联）
     * @param toolTimeout   单工具执行超时（impl-49：此前硬编码 60s；run_command 等长任务工具
     *                      的模块级 maxTimeout 大于本值时须同步调大本值，否则 harness 层先掐断）
     */
    public record Core(EventDispatch eventDispatch, java.time.Duration toolTimeout,
                       java.time.Duration indexClosedRetention) {
        /** 多构造器场景下显式指定绑定构造器（1/2 参兼容构造仅供编程式使用）。 */
        @org.springframework.boot.context.properties.bind.ConstructorBinding
        public Core {
            eventDispatch = eventDispatch == null ? new EventDispatch(null, null, null, null)
                    : eventDispatch;
            toolTimeout = toolTimeout == null || toolTimeout.isZero() || toolTimeout.isNegative()
                    ? java.time.Duration.ofSeconds(60) : toolTimeout;
            // spec 37 §C / T134：索引保留期——null = 默认 30d；负值/0 = 永久（不清扫）
            indexClosedRetention = indexClosedRetention == null
                    ? java.time.Duration.ofDays(30) : indexClosedRetention;
        }

        /** 既有 1 参构造兼容。 */
        public Core(EventDispatch eventDispatch) {
            this(eventDispatch, null, null);
        }

        /** 既有 2 参构造兼容。 */
        public Core(EventDispatch eventDispatch, java.time.Duration toolTimeout) {
            this(eventDispatch, toolTimeout, null);
        }
    }

    /**
     * impl-34 / spec 13 §core-4：会话事件分发模式（{@code buzhou.core.event-dispatch.*}）。
     *
     * @param mode        {@code sync}（默认，内联）| {@code buffered}（有界队列异步）
     * @param capacity    队列容量（默认 1024）
     * @param overflow    {@code drop-oldest}（默认）| {@code block}
     * @param pushTimeout block 策略入队限时（默认 2s）
     */
    public record EventDispatch(
            io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.Mode mode,
            Integer capacity,
            io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.OverflowPolicy overflow,
            Duration pushTimeout) {

        public EventDispatch {
            mode = mode == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.Mode.SYNC : mode;
            if (capacity != null && capacity <= 0) {
                // impl-42 / spec 13 §T68：越界值启动即拒（负容量此前被静默归一为默认）
                throw new IllegalArgumentException(
                        "buzhou.core.event-dispatch.capacity 必须为正整数（收到 " + capacity + "）");
            }
            capacity = capacity == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.DEFAULT_CAPACITY
                    : capacity;
            overflow = overflow == null
                    ? io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.OverflowPolicy.DROP_OLDEST
                    : overflow;
            pushTimeout = pushTimeout == null || pushTimeout.isZero() || pushTimeout.isNegative()
                    ? io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig.DEFAULT_PUSH_TIMEOUT
                    : pushTimeout;
        }

        /** 装配用：映射为会话层配置对象。 */
        public io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig toConfig() {
            return new io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig(
                    mode, capacity, overflow, pushTimeout);
        }
    }

    /**
     * impl-30 / spec 13 §core-1：优雅停机预算——{@code buzhou.lifecycle.timeout-per-shutdown-phase}
     * （默认 30s）。core lifecycle stop 的排空等待上界：预算内等在途 Turn 收尾（AFTER_CURRENT_TURN），
     * 到点硬截断（对在途会话发 IMMEDIATE 取消 + executor {@code shutdownNow}）；同时作为会话
     * executor 优雅关闭的 {@code awaitTermination} 预算（{@code shutdown()} 后限时等待）。
     *
     * @param timeoutPerShutdownPhase 单个停机 phase 的排空超时；null/非正 → 默认 30s
     */
    public record Lifecycle(Duration timeoutPerShutdownPhase) {

        /** impl-30：停机排空预算默认值（Spring Boot graceful shutdown 默认 30s 对齐）。 */
        public static final Duration DEFAULT_TIMEOUT_PER_SHUTDOWN_PHASE = Duration.ofSeconds(30);

        public Lifecycle {
            timeoutPerShutdownPhase = (timeoutPerShutdownPhase == null
                    || timeoutPerShutdownPhase.isZero() || timeoutPerShutdownPhase.isNegative())
                    ? DEFAULT_TIMEOUT_PER_SHUTDOWN_PHASE : timeoutPerShutdownPhase;
        }
    }
}
