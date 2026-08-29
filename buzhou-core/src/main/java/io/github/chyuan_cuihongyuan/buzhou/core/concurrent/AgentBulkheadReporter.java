package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * 舱占用发布器 + 集群快照（spec 127 / T474）：{@code report()} 把
 * {@link AgentBulkhead} 全部配置舱的当前占用作为心跳发布到后端；
 * {@code clusterSnapshot()} 返回「远端活跃心跳聚合 + 本实例实时」双列视图。
 *
 * <p>发布/查询都是显式调用（定时 autoconfig 后续轮按需）——观测面，不参与许可
 * 裁决（{@code AgentBulkhead.acquire} 本地路径零变化）。
 *
 * <p><b>口径（诚实声明）</b>：clusterOccupied = 活跃心跳之和（含本实例最近一次心跳，
 * 最多滞后一个心跳窗）；localOccupied = 本实例实时真值。两列并列不合并——心跳
 * 滞后导致的偏差对读者可见。
 */
public final class AgentBulkheadReporter {

    /** 快照行：心跳聚合占用 / 活跃实例数 / 本地配置上限 / 本地实时占用。 */
    public record ClusterRow(int clusterOccupied, int instances, int localLimit, int localOccupied) {
    }

    private final AgentBulkhead bulkhead;
    private final BulkheadStateBackend backend;
    private final String instanceId;
    private final Duration heartbeatTtl;

    public AgentBulkheadReporter(AgentBulkhead bulkhead, BulkheadStateBackend backend,
                                 String instanceId, Duration heartbeatTtl) {
        if (backend == null || instanceId == null || instanceId.isBlank()
                || heartbeatTtl == null || heartbeatTtl.isZero() || heartbeatTtl.isNegative()) {
            throw new IllegalArgumentException("backend/instanceId 必须非空、heartbeatTtl 为正");
        }
        this.bulkhead = bulkhead == null ? AgentBulkhead.unlimited() : bulkhead;
        this.backend = backend;
        this.instanceId = instanceId;
        this.heartbeatTtl = heartbeatTtl;
    }

    /**
     * 发布本实例全部配置舱的占用心跳（每 agent 一条；未配置舱零开销不发）。
     * no-op 后端 = 零成本空操作。
     */
    public void report() {
        Instant now = Instant.now();
        bulkhead.configuredAgents().forEach((agent, limit) ->
                backend.heartbeat(new BulkheadStateBackend.InstanceOccupancy(
                        instanceId, agent, bulkhead.inFlight(agent), limit, now), heartbeatTtl));
    }

    /**
     * 集群快照：行 = 远端活跃心跳聚合（agent 并集含本地配置舱），按 agent 名稳定序。
     * 本地实时占用并列给出（不与心跳和合并——滞后偏差可见）。
     */
    public Map<String, ClusterRow> clusterSnapshot() {
        Map<String, Integer> occupancy = backend.clusterOccupancy();
        Map<String, Integer> instances = backend.clusterInstances();
        Map<String, ClusterRow> snapshot = new TreeMap<>();
        occupancy.forEach((agent, occ) -> snapshot.put(agent,
                new ClusterRow(occ, instances.getOrDefault(agent, 0),
                        localLimitOf(agent), localInFlight(agent))));
        bulkhead.configuredAgents().forEach((agent, limit) -> snapshot.computeIfAbsent(agent,
                a -> new ClusterRow(0, 0, limit, bulkhead.inFlight(a))));
        return snapshot;
    }

    private int localLimitOf(String agent) {
        Integer limit = bulkhead.configuredAgents().get(agent);
        return limit == null ? Integer.MAX_VALUE : limit;
    }

    private int localInFlight(String agent) {
        Integer limit = bulkhead.configuredAgents().get(agent);
        return limit == null ? 0 : bulkhead.inFlight(agent);
    }
}
