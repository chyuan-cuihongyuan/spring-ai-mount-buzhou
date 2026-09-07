package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警面板端点 {@code /actuator/buzhou-alerts}（spec 345 / T681，
 * Alertmanager UI / Grafana alerting 面板借鉴）：聚合 312 规则引擎与
 * 330 通知策略门的运行时状态——「哪些规则 firing / 哪些静默窗在吞 /
 * 抑制视图根因是谁」一屏可答。只读；bean 缺席段为空（未配告警零变化）。
 */
@Endpoint(id = "buzhou-alerts")
public final class BuzhouAlertsEndpoint {

    private final AlertRuleEngine engine; // 可空——未配规则
    private final AlertGate gate;         // 可空——未配静默/抑制

    public BuzhouAlertsEndpoint(AlertRuleEngine engine, AlertGate gate) {
        this.engine = engine;
        this.gate = gate;
    }

    @ReadOperation
    public Map<String, Object> alertsDashboard() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("engine", engineSection());
        payload.put("gate", gateSection());
        return payload;
    }

    private Map<String, Object> engineSection() {
        Map<String, Object> section = new LinkedHashMap<>();
        if (engine == null) {
            section.put("rules", List.of());
            section.put("firing", Map.of());
            return section;
        }
        List<Map<String, Object>> rules = new ArrayList<>();
        for (AlertRuleEngine.AlertRule rule : engine.rules()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", rule.name());
            entry.put("mechanism", rule.mechanism());
            entry.put("for", String.valueOf(rule.forDuration()));
            if (!rule.annotations().isEmpty()) {
                entry.put("annotations", rule.annotations()); // spec 347：面板同步注解
            }
            rules.add(entry);
        }
        section.put("rules", rules);
        section.put("firing", engine.firingView());
        return section;
    }

    private Map<String, Object> gateSection() {
        Map<String, Object> section = new LinkedHashMap<>();
        if (gate == null) {
            section.put("activeSilences", List.of());
            section.put("firingMechanisms", List.of());
            section.put("inhibitRules", List.of());
            return section;
        }
        List<Map<String, Object>> silences = new ArrayList<>();
        for (AlertGate.Silence silence : gate.activeSilences()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", silence.id());
            entry.put("mechanisms", silence.mechanisms());
            entry.put("until", String.valueOf(silence.until()));
            entry.put("comment", silence.comment());
            silences.add(entry);
        }
        section.put("activeSilences", silences);
        section.put("firingMechanisms", gate.firingMechanisms());
        List<Map<String, String>> inhibits = new ArrayList<>();
        for (AlertGate.InhibitRule rule : gate.inhibitRules()) {
            inhibits.add(Map.of("source", rule.sourceMechanism(),
                    "target", rule.targetMechanism()));
        }
        section.put("inhibitRules", inhibits);
        return section;
    }
}
