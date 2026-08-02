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
}
