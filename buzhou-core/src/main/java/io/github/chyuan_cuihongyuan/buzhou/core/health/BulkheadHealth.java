package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.AgentBulkhead;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 隔离舱健康面（spec 92 §A / T349，#84 fog 毕业生）：未配置任何上限（全 NOOP）报
 * UNKNOWN（disabled 详情——BuzhouHealth 严格 DOWN 纪律：未启用 ≠ DOWN）；配置后
 * UP + 每 agent limit/inFlight 有界详情（只列已配置 agent——agent 名不进 tag 纪律
 * 的健康面等价物）。
 */
public final class BulkheadHealth implements BuzhouHealth {

    static final int MAX_AGENTS_SHOWN = 16;

    private final AgentBulkhead bulkhead;

    public BulkheadHealth(AgentBulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    @Override
    public String mechanism() {
        return "bulkhead";
    }

    @Override
    public Status status() {
        return bulkhead.configuredAgents().isEmpty() ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Integer> configured = bulkhead.configuredAgents();
        if (configured.isEmpty()) {
            return Map.of("disabled", true);
        }
        Map<String, Object> agents = new LinkedHashMap<>();
        int shown = 0;
        for (Map.Entry<String, Integer> entry : configured.entrySet()) {
            if (shown++ >= MAX_AGENTS_SHOWN) {
                break; // 有界详情纪律（配置上限表本就有界——防御式截断）
            }
            String agent = entry.getKey();
            agents.put(agent, "inFlight=" + bulkhead.inFlight(agent)
                    + "/limit=" + entry.getValue());
        }
        // spec 117 §A / T415：top 被拒 agent（限流风暴定位——topRejections 稳定排序）
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agents", agents);
        List<String> topRejected = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : bulkhead.topRejections(3)) {
            topRejected.add(entry.getKey() + " x" + entry.getValue());
        }
        out.put("topRejected", topRejected);
        return java.util.Collections.unmodifiableMap(out);
    }
}
