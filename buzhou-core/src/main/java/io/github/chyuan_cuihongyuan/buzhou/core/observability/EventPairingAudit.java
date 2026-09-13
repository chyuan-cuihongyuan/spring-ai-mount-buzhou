package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 事件配对完整性审计（spec 735 / T1070，观测对偶配对）：请求/应答型事件对
 * （TOOL_INPUT→TOOL_OUTPUT、HITL_REQUEST→HITL_DECISION 等）按 spanId 配对——
 * 悬空请求（无应答，中断/崩溃证据）与孤儿应答（无请求）变结构化发现。
 *
 * <p>配对规则由调用方供给（requestType → responseType），泛化任意配对族；
 * 纯函数不改事件。
 */
public final class EventPairingAudit {

    /** 单条发现（kind ∈ UNPAIRED_REQUEST / UNPAIRED_RESPONSE；eventId 定位）。 */
    public record Finding(String kind, String spanId, String eventId, String type) {
    }

    /** 不可变报告（findings 按 spanId+eventId 字典序）。 */
    public record Report(List<Finding> findings, int paired) {
    }

    private EventPairingAudit() {
    }

    /**
     * 配对审计（null fail-fast；空事件 = 零发现零配对）。
     *
     * @param events            事件集（同 spanId 内配对）
     * @param requestToResponse 请求类型 → 应答类型（如 TOOL_INPUT→TOOL_OUTPUT）
     */
    public static Report audit(List<EventRecord> events, Map<String, String> requestToResponse) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(requestToResponse, "requestToResponse");
        Map<String, Map<String, Set<String>>> bySpan = new HashMap<>();
        for (EventRecord event : events) {
            if (event == null) {
                continue;
            }
            String span = event.spanId() == null ? "" : event.spanId();
            bySpan.computeIfAbsent(span, k -> new HashMap<>())
                    .computeIfAbsent(event.type(), k -> new HashSet<>())
                    .add(event.eventId());
        }
        List<Finding> findings = new ArrayList<>();
        int paired = 0;
        for (Map.Entry<String, Map<String, Set<String>>> spanEntry : bySpan.entrySet()) {
            String span = spanEntry.getKey();
            Map<String, Set<String>> types = spanEntry.getValue();
            for (Map.Entry<String, String> rule : requestToResponse.entrySet()) {
                Set<String> requests = types.getOrDefault(rule.getKey(), Set.of());
                Set<String> responses = types.getOrDefault(rule.getValue(), Set.of());
                int pairs = Math.min(requests.size(), responses.size());
                paired += pairs;
                for (int i = 0; i < requests.size() - pairs; i++) {
                    findings.add(new Finding("UNPAIRED_REQUEST", span,
                            requests.iterator().next(), rule.getKey()));
                }
                for (int i = 0; i < responses.size() - pairs; i++) {
                    findings.add(new Finding("UNPAIRED_RESPONSE", span,
                            responses.iterator().next(), rule.getValue()));
                }
            }
        }
        findings.sort(Comparator.comparing(Finding::spanId).thenComparing(Finding::eventId));
        return new Report(List.copyOf(findings), paired);
    }
}
