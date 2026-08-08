package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.List;
import java.util.Optional;

public interface ObservabilityStore {

    void saveSpans(List<SpanRecord> spans);

    void saveEvents(List<EventRecord> events);

    List<SpanRecord> spansOfSession(String sessionId);

    List<EventRecord> eventsOfSession(String sessionId);

    void saveInjectionSnapshot(InjectionSnapshot snapshot);

    Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq);

    /**
     * 会话列表（spec 03 推演 #11，ticket 17 dashboard 数据源）：按最近活跃降序。
     *
     * @param cursor 分页游标（offset 语义的不透明字符串；null/空 = 首页）
     * @param size   页大小
     */
    List<SessionSummary> listSessionSummaries(String cursor, int size);

    /** 单 span 的 Event 流（spec 03 findEventsBySpan 的实现定名，ticket 17 补回）。 */
    List<EventRecord> eventsOfSpan(String spanId);
}
