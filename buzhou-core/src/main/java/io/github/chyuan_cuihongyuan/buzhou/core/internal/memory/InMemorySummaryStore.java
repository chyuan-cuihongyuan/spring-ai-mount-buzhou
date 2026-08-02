package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemorySummaryStore implements SummaryStore {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<StructuredSummary>> bySession =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();

    @Override
    public long save(String sessionId, StructuredSummary summary) {
        long version = versions.computeIfAbsent(sessionId, k -> new AtomicLong()).incrementAndGet();
        StructuredSummary versioned = new StructuredSummary(sessionId, version, summary.sections(),
                summary.tokenEstimate(), summary.createdAt());
        bySession.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(versioned);
        return version;
    }

    @Override
    public Optional<StructuredSummary> latest(String sessionId) {
        List<StructuredSummary> all = bySession.get(sessionId);
        if (all == null || all.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(all.get(all.size() - 1));
    }

    @Override
    public List<StructuredSummary> history(String sessionId, int limit) {
        List<StructuredSummary> all = bySession.getOrDefault(sessionId, new CopyOnWriteArrayList<>());
        return all.stream()
                .sorted(Comparator.comparingLong(StructuredSummary::version).reversed())
                .limit(limit)
                .toList();
    }
}
