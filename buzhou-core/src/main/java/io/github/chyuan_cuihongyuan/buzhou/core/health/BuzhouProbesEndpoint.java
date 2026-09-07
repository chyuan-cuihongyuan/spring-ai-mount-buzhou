package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 探针裁决端点 {@code /actuator/buzhou-probes}（spec 332 / T656，K8s probes
 * 借鉴）：三类独立裁决——liveness（重启能治）/ readiness（摘流量能治，
 * 缺省归类）/ startup（等待热身），各带成员与 failing 清单。
 *
 * <p>与 {@code /actuator/buzhou} 聚合面并列：聚合面答「哪个机制坏」，
 * 本端点答「K8s 该做什么」。只读、无敏感字段。
 */
@Endpoint(id = "buzhou-probes")
public final class BuzhouProbesEndpoint {

    private final List<BuzhouHealth> contributors;
    private final BuzhouProbes probes;

    public BuzhouProbesEndpoint(List<BuzhouHealth> contributors, BuzhouProbes probes) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
        this.probes = probes;
    }

    @ReadOperation
    public Map<String, Object> probeVerdicts() {
        Map<String, BuzhouHealth.Status> statuses = new LinkedHashMap<>();
        for (BuzhouHealth contributor : contributors) {
            statuses.put(contributor.mechanism(), safeStatus(contributor));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        probes.verdicts(statuses).forEach((probeClass, verdict) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", verdict.status().name());
            entry.put("mechanisms", verdict.mechanisms());
            entry.put("failing", verdict.failing());
            payload.put(probeClass.name().toLowerCase(java.util.Locale.ROOT), entry);
        });
        return payload;
    }

    private BuzhouHealth.Status safeStatus(BuzhouHealth contributor) {
        try {
            return contributor.status();
        } catch (RuntimeException e) {
            return BuzhouHealth.Status.DOWN; // 健康面自身炸了按 DOWN（诚实）
        }
    }
}
