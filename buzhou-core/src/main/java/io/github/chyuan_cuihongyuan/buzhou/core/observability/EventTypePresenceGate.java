package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 事件静默缺失门（spec 739 / T1078，726 分布的对偶面）：期望出现的事件
 * 类型在观测集内一次都没出现——生命周期不完整（如有 started 无 finished）
 * 的前向信号。分布读数回答「什么在发生」，本面回答「什么没发生」。
 *
 * <p>纯函数：期望清单由调用方供给（不同机制的生命周期契约不同）。
 */
public final class EventTypePresenceGate {

    /** 不可变报告（missing 字典序）。 */
    public record Report(List<String> missing, int expectedCount, int observedTypes) {
    }

    private EventTypePresenceGate() {
    }

    /** 门禁（null fail-fast；空期望 = 空 missing——无契约不误报）。 */
    public static Report gate(List<EventRecord> events, Set<String> expectedTypes) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(expectedTypes, "expectedTypes");
        Set<String> observed = new HashSet<>();
        for (EventRecord event : events) {
            if (event != null && event.type() != null) {
                observed.add(event.type());
            }
        }
        List<String> missing = new ArrayList<>();
        for (String expected : expectedTypes) {
            if (!observed.contains(expected)) {
                missing.add(expected);
            }
        }
        java.util.Collections.sort(missing);
        return new Report(List.copyOf(missing), expectedTypes.size(), observed.size());
    }
}
