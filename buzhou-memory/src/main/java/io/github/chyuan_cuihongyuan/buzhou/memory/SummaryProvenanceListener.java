package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 摘要溯源台账（spec 171 / T537，W&B artifact lineage 借鉴）：微压缩折叠的
 * messageIds 累积 → 摘要折入时落一条 lineage（代际/trigger/源集/count）——
 * 「这段结论出自哪些消息」可回查。实现 {@link CompactionListener}（spec 95
 * 挂点，零管线侵入；异常吞语义沿 listener 契约）。
 *
 * <p>有界：LRU 64 会话 × 每会话最近 32 条（更早代际不驻留——持久化留档）。
 */
public final class SummaryProvenanceListener implements CompactionListener {

    /** 一代摘要的溯源（源消息 id 集即证据链）。 */
    public record Entry(long generation, String trigger, List<String> sourceMessageIds,
                        int foldedCount, Instant at) {
    }

    private static final int MAX_SESSIONS = 64;
    private static final int MAX_ENTRIES_PER_SESSION = 32;

    private final LinkedHashMap<String, SessionLedger> sessions =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SessionLedger> eldest) {
                    return size() > MAX_SESSIONS;
                }
            };

    private static final class SessionLedger {
        final List<String> pending = new ArrayList<>();
        final List<Entry> entries = new ArrayList<>();
    }

    /** 微压缩折叠的 messageIds 进待落账池（同代多次微压缩累积合并）。 */
    @Override
    public synchronized void onCompacted(String sessionId, MicroCompactionResult result,
                                         double evictRatio) {
        ledgerOf(sessionId).pending.addAll(result.compactedMessageIds());
    }

    /** 摘要折入成功：待落账池固化为一条 lineage（trigger = 本次折入判据）。 */
    @Override
    public synchronized void onSummaryFolded(String sessionId, NineSectionSummary summary,
                                             String trigger) {
        SessionLedger ledger = ledgerOf(sessionId);
        List<String> sources = List.copyOf(ledger.pending);
        ledger.pending.clear();
        ledger.entries.add(new Entry(summary.generation(), trigger, sources,
                sources.size(), Instant.now()));
        if (ledger.entries.size() > MAX_ENTRIES_PER_SESSION) {
            ledger.entries.removeFirst(); // 更早代际滑出（持久化留档——内存窗有界）
        }
    }

    /** 会话 lineage（代际序）。 */
    public synchronized List<Entry> lineage(String sessionId) {
        SessionLedger ledger = sessions.get(sessionId);
        return ledger == null ? List.of() : List.copyOf(ledger.entries);
    }

    /** 某代源集直取（无该代 = 空）。 */
    public synchronized List<String> sourcesOf(String sessionId, long generation) {
        return lineage(sessionId).stream()
                .filter(e -> e.generation() == generation)
                .findFirst()
                .map(Entry::sourceMessageIds)
                .orElse(List.of());
    }

    /** 台账驻留会话数（观测面）。 */
    public synchronized int trackedSessions() {
        return sessions.size();
    }

    private SessionLedger ledgerOf(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new SessionLedger());
    }
}
