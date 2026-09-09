package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 工具泳道注册表（spec 173 / T541，Hystrix 线程池隔离舱借鉴）：命名泳道 →
 * 共享 Semaphore（同名单例）——按工具资源画像分道（slow-db:2 / fast-cache:16），
 * 慢的一队饿不死快的一队。与 agent 舱（84）/工具熔断（131）正交：资源分道面。
 */
public final class ToolLaneRegistry {

    private final Map<String, Semaphore> lanes = new ConcurrentHashMap<>();
    /** spec 422：优先级泳道分仓（与 Semaphore 泳道类型不同——不共享 namespace 防混用同名 CCE）。 */
    private final Map<String, io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane> priorityLanes =
            new ConcurrentHashMap<>();

    /** 取/建泳道（同名单例；permits>=1——重复取同名单例忽略新 permits）。 */
    public Semaphore lane(String name, int permits) {
        if (name == null || name.isBlank() || permits < 1) {
            throw new IllegalArgumentException("泳道名非空、permits>=1（当前 " + permits + "）");
        }
        return lanes.computeIfAbsent(name, k -> new Semaphore(permits, true));
    }

    /**
     * spec 422 / T735：取/建<b>优先级</b>泳道（同名单例；permits>=1——重复取
     * 同名单例忽略新 permits；{@link io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane}
     * 0-9 优先级插队语义）。
     */
    public io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane priorityLane(String name, int permits) {
        if (name == null || name.isBlank() || permits < 1) {
            throw new IllegalArgumentException("泳道名非空、permits>=1（当前 " + permits + "）");
        }
        return priorityLanes.computeIfAbsent(name,
                k -> new io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane(permits));
    }

    /** 已注册泳道名集（观测面）。 */
    public java.util.Set<String> laneNames() {
        return java.util.Set.copyOf(lanes.keySet());
    }
}
