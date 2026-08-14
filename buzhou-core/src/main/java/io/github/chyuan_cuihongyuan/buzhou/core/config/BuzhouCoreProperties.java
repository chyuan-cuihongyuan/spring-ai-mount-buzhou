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
@ConfigurationProperties(prefix = "buzhou")
public record BuzhouCoreProperties(String modelName, Store store,
                                   Duration leaseTtl, Duration leaseRenewInterval,
                                   Lifecycle lifecycle, Core core) {

    /** impl-33：租约 TTL 默认值（既有硬编码语义收敛）。 */
    public static final Duration DEFAULT_LEASE_TTL = Duration.ofSeconds(90);

    public BuzhouCoreProperties {
        modelName = (modelName == null || modelName.isBlank()) ? "unknown" : modelName;
        store = store == null ? new Store(null) : store;
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

    public record Store(String type) {
        public Store {
            type = (type == null || type.isBlank()) ? "memory" : type;
        }
    }

    /**
     * impl-34 / spec 13 §core-4：core 运行时旋钮（{@code buzhou.core.*}）。
     *
     * @param eventDispatch 事件分发（{@code buzhou.core.event-dispatch.*}；默认 SYNC 内联）
     */
    public record Core(EventDispatch eventDispatch) {
        public Core {
            eventDispatch = eventDispatch == null ? new EventDispatch(null, null, null, null)
                    : eventDispatch;
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
            capacity = capacity == null || capacity <= 0
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
