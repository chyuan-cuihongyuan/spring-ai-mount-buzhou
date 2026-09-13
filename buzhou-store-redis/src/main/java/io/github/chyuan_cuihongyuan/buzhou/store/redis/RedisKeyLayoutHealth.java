package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 键布局健康面（spec 727 / T1054——705 审计的接线，548 同型）：
 * mechanism=redis-key-layout 观测面恒 UP——结构碰撞是数据需关注（改键形状
 * 是迁移语义），非进程故障；details 聚合三族碰撞计数+保留段清单。
 */
public final class RedisKeyLayoutHealth implements BuzhouHealth {

    private final List<RedisKeyLayoutAudit.Finding> findings;

    public RedisKeyLayoutHealth(String keyPrefix) {
        this.findings = RedisKeyLayoutAudit.audit(keyPrefix);
    }

    @Override
    public String mechanism() {
        return "redis-key-layout";
    }

    @Override
    public Status status() {
        return Status.UP; // 观测面——findings 是数据需关注，非进程故障（548 同口径）
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("findings", (long) findings.size());
        details.put("shapeCollisions",
                findings.stream().filter(f -> f.kind().equals("SPAN_INDEX_CLASH")).count());
        details.put("colonSuffixTricks",
                findings.stream().filter(f -> f.kind().equals("COLON_SUFFIX_TRICK")).count());
        details.put("reservedSegments", RedisKeyLayoutAudit.reservedSegments().size());
        return details;
    }
}
