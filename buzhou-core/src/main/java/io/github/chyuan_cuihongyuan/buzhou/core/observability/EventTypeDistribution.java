package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 事件类型分布读数（spec 726 / T1052，Grafana Loki top-k 思想）：
 * 哪类事件在刷屏、哪些从不发生——543/720 span 面的事件对偶。
 * 类型字符串不假设闭集（自定义类型照常聚合）。纯函数读数。
 */
public final class EventTypeDistribution {

    /** 单类型行（count 降序；同计数 type 字典序稳定）。 */
    public record Row(String type, long count) {
    }

    /** 不可变报告。 */
    public record Report(List<Row> rows, long total, int distinctTypes, Row top) {
    }

    private EventTypeDistribution() {
    }

    /** 聚合（null fail-fast；空表 = total 0 + top null）。 */
    public static Report of(List<EventRecord> events) {
        Objects.requireNonNull(events, "events");
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (EventRecord event : events) {
            if (event == null) {
                continue;
            }
            String type = event.type() == null ? "UNKNOWN" : event.type();
            counts.merge(type, 1L, Long::sum);
            total++;
        }
        List<Row> rows = new ArrayList<>(counts.size());
        counts.forEach((type, count) -> rows.add(new Row(type, count)));
        rows.sort(Comparator.comparingLong(Row::count).reversed().thenComparing(Row::type));
        Row top = rows.isEmpty() ? null : rows.get(0);
        return new Report(List.copyOf(rows), total, rows.size(), top);
    }
}
