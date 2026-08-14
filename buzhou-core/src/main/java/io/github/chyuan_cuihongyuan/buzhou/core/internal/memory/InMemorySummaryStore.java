package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 摘要内存实现（事实台账 = noeviction 语义）。
 *
 * <p>impl-36 / spec 13 §growth-8：新会话写入超过 {@code maxSessions} 抛
 * {@link QuotaExceededException}（绝不静默丢；旧版本修剪是切片 37 的保留策略职责）。
 */
public class InMemorySummaryStore implements SummaryStore {

    /** impl-36：准入串行化（检查与写入同临界区——原子拒绝）。 */
    private final Object admissionLock = new Object();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<StructuredSummary>> bySession =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();
    private final int maxSessions;

    public InMemorySummaryStore() {
        this(InMemoryStoreConfig.defaults());
    }

    public InMemorySummaryStore(InMemoryStoreConfig config) {
        InMemoryStoreConfig effective = config == null ? InMemoryStoreConfig.defaults() : config;
        this.maxSessions = effective.maxSessions();
    }

    @Override
    public long save(String sessionId, StructuredSummary summary) {
        synchronized (admissionLock) {
            if (!bySession.containsKey(sessionId) && bySession.size() >= maxSessions) {
                throw new QuotaExceededException(
                        "内存摘要存储会话数已达上限 maxSessions=%d（sessionId=%s）"
                                .formatted(maxSessions, sessionId));
            }
            long version = versions.computeIfAbsent(sessionId, k -> new AtomicLong()).incrementAndGet();
            StructuredSummary versioned = new StructuredSummary(sessionId, version, summary.sections(),
                    summary.tokenEstimate(), summary.createdAt());
            bySession.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(versioned);
            return version;
        }
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

    /** impl-35 / spec 13 §stores-6：移除该会话全部摘要版本与版本计数器（幂等）。 */
    @Override
    public void deleteSession(String sessionId) {
        bySession.remove(sessionId);
        versions.remove(sessionId);
    }

    /** impl-36：在册会话数（测试与运维可观测）。 */
    int sessionCount() {
        return bySession.size();
    }

    /** impl-37 / spec 13 §stores-6：旧版本修剪——每会话保留最近 keepLatest 个版本。 */
    @Override
    public int pruneVersions(int keepLatest) {
        int keep = Math.max(1, keepLatest);
        int deleted = 0;
        for (CopyOnWriteArrayList<StructuredSummary> versions : bySession.values()) {
            if (versions.size() <= keep) {
                continue;
            }
            versions.sort(Comparator.comparingLong(StructuredSummary::version));
            int overflow = versions.size() - keep;
            versions.subList(0, overflow).clear();
            deleted += overflow;
        }
        return deleted;
    }
}
