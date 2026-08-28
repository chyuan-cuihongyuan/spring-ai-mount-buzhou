package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.Optional;

/**
 * 熔断状态共享后端 SPI（spec 57 §A / T254 / effort#17，core.spi——resilience 策略层与
 * store-redis 实现层共用，同 {@link RateLimitBackend} 分层范式）：「跳闸事实共享、探测
 * 与窗口留本地」——只共享 OPEN 三元组（openedAt / 冷却剩余 / 连续跳闸数），窗口样本与
 * 在飞探测是实例本地事实（共享全量状态机 = 分布式状态机，复杂度不成比例）。
 *
 * <p>默认三方法全 no-op（{@code activeTrip} 恒空）——单进程部署语义零变化；多实例共享
 * 用 Redis TTL 键（键存活 = OPEN，过期 = 可探测；LiteLLM deployment cooldown 思想）。
 * 实现必须线程安全；故障语义 = 降级本地语义（WARN 后继续——观测面故障不放大为服务
 * 故障，与限流后端 fail-fast 刻意不同，spec 57 入档）。
 */
public interface CircuitBreakerStateBackend {

    /** 活跃跳闸标记（cooldownMs = 剩余冷却毫秒）。 */
    record TripMarker(Instant openedAt, long cooldownMs, int consecutiveTrips) {
    }

    /**
     * 记录一次跳闸（本地状态机转 OPEN 时调用）：全实例在 cooldownMs 内对该模型按
     * OPEN 拒绝；consecutiveTrips 供退避倍数跨实例延续观测。
     */
    default void recordTrip(String modelName, Instant openedAt, long cooldownMs, int consecutiveTrips) {
    }

    /**
     * 当前活跃跳闸（冷却期内 = 存活）；无 / 已过冷却 / 后端不可达降级 → empty。
     */
    default Optional<TripMarker> activeTrip(String modelName) {
        return Optional.empty();
    }

    /** 清除跳闸标记（本地半开探测达标转 CLOSED 时调用；幂等）。 */
    default void clear(String modelName) {
    }

    /** 后端标识（观测/日志：noop / redis）。 */
    default String kind() {
        return "noop";
    }
}
