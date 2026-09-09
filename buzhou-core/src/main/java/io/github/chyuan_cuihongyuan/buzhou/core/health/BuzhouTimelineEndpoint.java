package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康时间线端点 {@code /actuator/buzhou-timeline}（spec 405 / T702，
 * PagerDuty incident timeline 借鉴）：近期变迁 + per-mechanism 变迁计数
 * （抖动识别面）。只读；时间线未启用时本端点 bean 不在。
 */
@Endpoint(id = "buzhou-timeline")
public final class BuzhouTimelineEndpoint {

    private final HealthTimelineRecorder recorder; // 可空——理论缺席（属性键与记录器一致）

    public BuzhouTimelineEndpoint(HealthTimelineRecorder recorder) {
        this.recorder = recorder;
    }

    @ReadOperation
    public Map<String, Object> timeline() {
        if (recorder == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("entries", List.of());
            empty.put("transitionCounts", Map.of());
            empty.put("capacity", 0);
            return empty;
        }
        HealthTimeline timeline = recorder.timeline();
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (HealthTimeline.Entry e : timeline.entries()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("at", e.at().toString());
            row.put("mechanism", e.mechanism());
            row.put("from", e.from() == null ? null : e.from().name());
            row.put("to", e.to() == null ? null : e.to().name());
            entries.add(row);
        }
        payload.put("entries", entries);
        payload.put("transitionCounts", timeline.transitionCounts());
        payload.put("capacity", timeline.capacity());
        return payload;
    }
}
