package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;
import java.util.Optional;

public interface SummaryStore {

    long save(String sessionId, StructuredSummary summary);

    Optional<StructuredSummary> latest(String sessionId);

    List<StructuredSummary> history(String sessionId, int limit);
}
