package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 舱占用共享后端 SPI（spec 127 / T473，spec 57 CircuitBreakerStateBackend 同范式——
 * 共享事实、不共享状态机）：实例对每 agent 发布<b>占用心跳</b>（TTL 表达活跃），
 * 集群查询把活跃心跳按 agent 求和——跨实例舱压力观测聚合。
 *
 * <p><b>许可授予留本地</b>（AgentBulkhead 本地信号量低延迟路径不变）；本 SPI 只是
 * 观测面——诚实边界：不做跨实例许可协调（集群总占用不做全局闸）。
 *
 * <p>默认四方法 no-op——单进程部署语义零变化；实现必须线程安全；故障语义 =
 * 降级空快照 + WARN（观测面故障不放大为服务故障，spec 57 入档同款）。
 */
public interface BulkheadStateBackend {

    /** 一次实例级占用心跳（occupied = 该实例该 agent 当前在飞 Turn 数）。 */
    record InstanceOccupancy(String instanceId, String agentName, int occupied,
                             int limit, Instant at) {
    }

    /**
     * 发布占用心跳：实例存活期由 TTL 表达（过期心跳不计入聚合——实例崩溃后
     * 观测自愈，无需显式注销）。
     */
    default void heartbeat(InstanceOccupancy occupancy, Duration ttl) {
    }

    /**
     * 集群占用聚合：agent → 全实例<b>活跃</b>心跳占用之和（过期剔除）。
     * 无心跳 / 后端不可达降级 → 空 Map。
     */
    default Map<String, Integer> clusterOccupancy() {
        return Map.of();
    }

    /**
     * 集群实例数聚合：agent → 活跃心跳实例数（过期剔除；无 → 空 Map）。
     */
    default Map<String, Integer> clusterInstances() {
        return Map.of();
    }

    /** 后端标识（观测/日志：noop / in-memory / redis）。 */
    default String kind() {
        return "noop";
    }
}
